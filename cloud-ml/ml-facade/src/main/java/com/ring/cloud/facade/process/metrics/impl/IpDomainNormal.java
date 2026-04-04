package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.core.frame.IpRouteInit;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpNormalCrawlExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.TaskOperate;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class IpDomainNormal extends TaskOperate<IpTaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_DOMAIN_NORMAL;
    }

    @Autowired
    private IpNormalCrawlExecutor normalExecutor;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(IpTaskEntity ipTaskEntity) {
        // ======================
        // 通用唯一KEY
        // ======================
        String segmentNo = ipTaskEntity.getIpSegment().getSegmentNo();
        String uniqueKey = taskEnum().name() + ":" + segmentNo;

        try {
            return batchCrawl(ipTaskEntity, uniqueKey);
        } catch (Throwable e) {
            log.error("分段任务失败："+e.getMessage(),e);
            return false;
        }
    }

    // 批量采集控制逻辑 - 主方法
    public boolean batchCrawl(IpTaskEntity ipTaskEntity, String uniqueKey) throws IOException {
        Long taskId = ipTaskEntity.getTaskId();
        IpSegment ipSegment = ipTaskEntity.getIpSegment();
        String startIp = ipSegment.getStartIp();
        String currentIp = IpUtil.getSegmentStartIp(startIp);

        // 初始化文件
        String csvPath = FileUtil.ipCsvFileName(ipFilePath, ipFileNamePrefix, ipSegment.getSegmentNo());
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);
        final BufferedWriter bw = initCsvWriter(tmpCsvPath);

        try {
            // 执行采集逻辑
            return doBatchCrawl(ipTaskEntity, ipSegment, startIp, currentIp, bw, uniqueKey);
        } finally {
            // 统一关闭文件
            closeCsv(bw, tmpCsvPath, csvPath, true);
        }
    }
    // 核心采集循环
    private boolean doBatchCrawl(IpTaskEntity ipTaskEntity, IpSegment ipSegment, String startIp, String currentIp, BufferedWriter bw, String uniqueKey) {
        boolean isFirstSegment = true;
        IpBreakpoint breakpoint = new IpBreakpoint();
        ProxyIp currentProxy = getAvailableProxy();
        String endIp = ipSegment.getEndIp();

        while (true) {
            // 任务停止判断
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + ipTaskEntity.getTaskId() + "]已终止，最后处理ip段：" + currentIp);
                return true;
            }
            if (ExceedStop(currentIp, endIp)) {
                break;
            }

            // 获取Pang列表
            List<String> pangIpList = getPangIpListWithRetry(currentIp, currentProxy);
            if (pangIpList.isEmpty()) {
                log.debug("pang未查询到数据，切换下一段IP：{}", currentIp);
                isFirstSegment = false;
                currentIp = IpUtil.nextSegmentIp(currentIp);
                breakpoint.reset();
                continue;
            }

            // 处理单个PangIp
            long totalCount = processPangIpList(
                    ipTaskEntity, startIp, endIp, isFirstSegment,
                    pangIpList, breakpoint, bw, currentProxy, uniqueKey
            );

            // 日志 & 切换下一段
            log.info("{} -- {} -- {}", currentIp, pangIpList.size(), totalCount);
            isFirstSegment = false;
            currentIp = IpUtil.nextSegmentIp(currentIp);
        }

        return true;
    }

    // 处理单个PangIp列表循环
    private long processPangIpList(
            IpTaskEntity ipTaskEntity, String startIp, String endIp,
            boolean isFirstSegment, List<String> pangIpList,
            IpBreakpoint breakpoint, BufferedWriter bw, ProxyIp currentProxy, String uniqueKey
    ) {
        long segmentTotalCount = 0;
        int pangSize = pangIpList.size();

        for (String pangIp : pangIpList) {
            // 任务停止
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + ipTaskEntity.getTaskId() + "]已终止，最后处理ip：" + pangIp);
                return segmentTotalCount;
            }
            // 起始IP过滤
            if (isFirstSegment && IpUtil.ipLessThan(pangIp, startIp)) {
                log.debug("第一段过滤：{} < {}，跳过", pangIp, startIp);
                continue;
            }
            // 结束IP判断
            if (IpUtil.ipGreaterThan(pangIp, endIp)) {
                log.info("段落完成 -- 数量:{} -- 总计:{}", pangSize, segmentTotalCount);
                log.debug("pangIp:{} 超出结束IP:{}", pangIp, endIp);
                return segmentTotalCount;
            }
            //跳过大ip
            if(IpRouteInit.IP_LEVEL_MAP.containsKey(pangIp))
                continue;
            // 采集单个IP
            breakpoint.reset();
            crawlSingleIpWithRetry(ipTaskEntity, pangIp, bw, currentProxy, breakpoint, uniqueKey);
            segmentTotalCount += breakpoint.getCurrentCount();
        }

        return segmentTotalCount;
    }
    // 单个IP采集 + 异常重试
    private void crawlSingleIpWithRetry(IpTaskEntity ipTaskEntity, String pangIp,
            BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint, String uniqueKey
    ) {
        boolean ipCrawlSuccess = false;

        while (!ipCrawlSuccess) {
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + ipTaskEntity.getTaskId() + "]已终止，最后处理ip：" + pangIp);
                return;
            }
            if (currentProxy == null) {
                currentProxy = getAvailableProxy();
            }

            try {
                IpReadInfo res = normalExecutor.crawlIp(bw, ipTaskEntity, pangIp, currentProxy, breakpoint);
                if (res != null && res.isSuccess()) {
                    ipCrawlSuccess = true;
                }
            } catch (Throwable e) {
                boolean needSwitch = needSwitchProxy(e);
                if (needSwitch) {
                    currentProxy = null;
                    log.debug("【代理异常】切换代理: {}", e.getMessage());
                } else {
                    log.debug("【目标波动】不切换代理: {}", e.getMessage());
                }
            }
        }
    }
    //查询pangList列表
    private List<String> getPangIpListWithRetry(String currentIp, ProxyIp currentProxy) {
        int maxRetry = 3;
        int retryCount = 0;
        List<String> pangIpList = null;

        while (retryCount < maxRetry) {
            try {
                pangIpList = pangIpServcie.pangIpsNoRetry(currentIp, currentProxy);
                if (pangIpList != null) {
                    break;
                }
            } catch (Throwable e) {
                log.debug("查询pangList异常 将切换代理重试 当前重试次数 {}", retryCount);
            }

            retryCount++;
            if (retryCount < maxRetry) {
                currentProxy = getAvailableProxy();
            }
        }

        // 都失败 → 使用默认IP列表
        if (pangIpList == null) {
            log.warn("查询失败，默认ip全段:{}", currentIp);
            pangIpList = IpUtil.generateFixedIpList(currentIp);
        }

        return pangIpList;
    }
    public boolean ExceedStop(String currentIp, String endIp) {
        return IpUtil.isCurrentIpExceedEndIp(currentIp, endIp);
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
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
