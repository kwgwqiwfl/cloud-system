package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.TaskOperate;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
//@Transactional
public class IpDomainHandle extends TaskOperate<IpTaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.FULL;
    }

//    @Value("${ml.client.proxy.valid.time:30}")
//    private int proxyValidTime;// 代理执行超时时间
    @Value("${ml.client.ip.file.path:/}")
    private String ipFilePath;//文件路径
    @Value("${ml.client.ip.file.name.prefix:test}")
    private String ipFileNamePrefix;//文件名前缀

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(IpTaskEntity ipTaskEntity) {
        try {
            return batchCrawl(ipTaskEntity);
        } catch (Throwable e) {
//            throw new IllegalArgumentException(e.getMessage());
            log.error("runTask失败："+e.getMessage(),e);
            return false;
        }
    }

    //批量采集控制逻辑
    public boolean batchCrawl(IpTaskEntity ipTaskEntity) throws IOException {
        Long taskId = ipTaskEntity.getTaskId();
        IpSegment ipSegment = ipTaskEntity.getIpSegment();
        String startIp = ipSegment.getStartIp();
        String currentIp = IpUtil.getSegmentStartIp(startIp);
        boolean isFirstSegment = true;
        boolean isBatchComplete = false;
        String csvPath = FileUtil.ipCsvFileName(ipFilePath, ipFileNamePrefix, ipSegment.getSegmentNo());
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);
        final BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(tmpCsvPath),
                        StandardCharsets.UTF_8
                )
        );
        IpBreakpoint breakpoint = new IpBreakpoint();

        try {
            ProxyIp currentProxy = getAvailableProxy();

            while (true) {
                // ====================== 【每次循环都获取最新的停止标志】 ======================
                AtomicBoolean taskStopFlag = GlobalTaskManager.TASK_STOP_MAP.get(ipSegment.getSegmentNo());
                if (taskStopFlag != null && taskStopFlag.get()) {
                    log.info("任务[" + taskId + "]已终止，最后处理ip段：" + currentIp);
                    break;
                }
                if (shouldStop(currentIp, ipSegment.getEndIp())) {
                    break;
                }
                List<String> pangIpList = null;
                while (pangIpList == null) {
                    try {
                        pangIpList = pangIpServcie.pangIpsNoRetry(currentIp, currentProxy);
                    } catch (Exception e) {
                        log.debug("pangIp查询异常，切换代理重试", e);
                        // 失败 → 换代理
                        currentProxy = getAvailableProxy();
                    }
                }
                long segmentTotalCount = 0;
                if (pangIpList.isEmpty()) {
                    log.debug("pang未查询到数据，切换下一段IP：{}", currentIp);
                    isFirstSegment = false;
                    currentIp = IpUtil.nextSegmentIp(currentIp);
                    breakpoint.reset();
                    continue;
                }
                int pangSize = pangIpList.size();
                String endIp = ipSegment.getEndIp();
                log.debug("开始处理pangIp数量:{} , 当前段落IP:{}", pangSize, currentIp);

                for (String pangIp : pangIpList) {
                    // 每次都获取最新
                    taskStopFlag = GlobalTaskManager.TASK_STOP_MAP.get(ipSegment.getSegmentNo());
                    if (taskStopFlag != null && taskStopFlag.get()) {
                        log.info("任务[" + taskId + "]已终止，最后处理ip：" + pangIp);
                        return true;
                    }
                    if (isFirstSegment && IpUtil.ipLessThan(pangIp, startIp)) {
                        log.debug("第一段过滤：{} < {}，跳过", pangIp, startIp);
                        continue;
                    }
                    if (IpUtil.ipGreaterThan(pangIp, endIp)) {
                        log.info("{} -- {} -- {}", ipSegment, pangSize, segmentTotalCount);
                        log.debug("pangIp:{} 超出结束IP:{}, 任务完成", pangIp, endIp);
                        isBatchComplete = true;
                        return true;
                    }
                    breakpoint.reset();
                    boolean ipCrawlSuccess = false;

                    while (!ipCrawlSuccess) {
                        taskStopFlag = GlobalTaskManager.TASK_STOP_MAP.get(ipSegment.getSegmentNo());
                        if (taskStopFlag != null && taskStopFlag.get()) {
                            log.info("任务[" + taskId + "]已终止，最后处理ip：" + pangIp);
                            return true;
                        }

                        // ====================== 【代理为空 → 自动获取新代理】 ======================
                        if (currentProxy== null) {
                            currentProxy = getAvailableProxy();
                        }

                        try {
                            IpReadInfo res = ipCrawlExecutor.crawlIp(
                                    bw,
                                    ipTaskEntity,
                                    pangIp,
                                    currentProxy,
                                    breakpoint
                            );

                            if (res != null && res.isSuccess()) {
                                ipCrawlSuccess = true;
                            }
                        } catch (Throwable e) {
//                            currentProxy = null;
//                            log.debug("采集执行异常, 切换代理重试: {}", e.getMessage());
                            String msg = e.getMessage() == null ? "" : e.getMessage();
                            boolean needSwitchProxy;
                            // ========== 明确代理挂了：必须切 ==========
                            if (msg.contains("connect timed out")
                                    || msg.contains("Connection refused")
                                    || msg.contains("Connection reset")
                                    || msg.contains("Failed to connect")
                                    || msg.contains("No route to host")
                                    || msg.contains("Socket closed")) {
                                needSwitchProxy = true;
                            }else if (msg.contains("Read timed out")// ========== 目标慢/超时：不切 ==========
                                    || msg.contains("SocketTimeout")
                                    || msg.contains("502")
                                    || msg.contains("503")
                                    || msg.contains("504")) {
                                needSwitchProxy = false;
                            }else {
                                needSwitchProxy = true;// ========== 其他所有异常：切，防止卡死 ==========
                            }

                            // 执行切换
                            if (needSwitchProxy) {
                                currentProxy = null;
                                log.debug("采集执行异常, 切换代理重试: {}", e.getMessage());
                            } else {
                                log.debug("采集执行异常(目标慢/波动), 不切换代理继续重试: {}", e.getMessage());
                            }
                        }
                    }
                    segmentTotalCount += breakpoint.getCurrentCount();
                }
                log.info("{} -- {} -- {}", currentIp, pangSize, segmentTotalCount);
                isFirstSegment = false;
                currentIp = IpUtil.nextSegmentIp(currentIp);
            }
            isBatchComplete = true;
        } finally {
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (Exception e) {
                log.error("关闭CSV文件流失败"+e.getMessage());
            }
            if(isBatchComplete)
                FileUtil.renameTmpToCsv(tmpCsvPath, csvPath);
        }
        return true;
    }

    /**
     * 统一获取代理，失败无限重试
     */
    private ProxyIp getAvailableProxy() {
        ProxyIp proxy = proxyPoolManager.takeProxy();
        while (proxy == null) {
            log.debug("代理获取失败，2秒后重试");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            proxy = proxyPoolManager.takeProxy();
        }
        return proxy;
    }



    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return IpUtil.isCurrentIpExceedEndIp(currentIp, endIp);
    }

    /**
     * 终止指定ID的采集任务
     */
//    public boolean stopSpecifiedTask(String taskId) {
//        boolean isStopped = crawlExecutor.stopCrawlTask(taskId);
//        if (isStopped) {
////            taskParamMap.remove(taskId); // 清理任务参数
//            System.out.println("任务[" + taskId + "]终止成功");
//        }
//        return isStopped;
//    }
}
