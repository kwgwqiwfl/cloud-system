package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpSegExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
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
public class IpSegTask extends AbstractTask<TaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_SEG;
    }

    @Autowired
    private IpSegExecutor normalExecutor;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(TaskEntity ipTaskEntity) {
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
    public boolean batchCrawl(TaskEntity ipTaskEntity, String uniqueKey) throws IOException {
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
            return doBatchCrawl(ipSegment, startIp, currentIp, bw, uniqueKey);
        } finally {
            // 统一关闭文件
            closeCsv(bw, tmpCsvPath, csvPath, true);
        }
    }
    // 核心采集循环
    private boolean doBatchCrawl(IpSegment ipSegment, String startIp, String currentIp, BufferedWriter bw, String uniqueKey) {
        boolean isFirstSegment = true;
        IpBreakpoint breakpoint = new IpBreakpoint();
        ProxyIp currentProxy = getAvailableProxy();
        String endIp = ipSegment.getEndIp();

        while (true) {
            // 任务停止判断
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理ip段：" + currentIp);
                return true;
            }
            if (ExceedStop(currentIp, endIp)) {
                break;
            }
            if (IpUtil.isInternalIp(currentIp)) {
                log.debug("内网IP跳过查询: {}", currentIp);
                isFirstSegment = false;
                currentIp = IpUtil.nextSegmentIp(currentIp);
                breakpoint.reset();
                continue;
            }
            // 获取Pang列表
            List<String> pangIpList = queryWithRetry(
                    currentIp,
                    currentProxy,
                    3,
                    pangIpServcie::pangIpsNoRetry,
                    IpUtil::generateFixedIpList
            );
            if (pangIpList.isEmpty()) {
                log.debug("pang未查询到数据，切换下一段IP：{}", currentIp);
                isFirstSegment = false;
                currentIp = IpUtil.nextSegmentIp(currentIp);
                breakpoint.reset();
                continue;
            }

            // 处理单个PangIp
            long totalCount = processPangIpList(startIp, endIp, isFirstSegment,
                    pangIpList, breakpoint, bw, currentProxy, uniqueKey
            );

            // 日志 & 切换下一段
            log.info("{} -- {} -- {}", currentIp, pangIpList.size(), totalCount);
            WsUtil.push(WsMessageType.NORMAL_TASK, "ip段："+ currentIp+" -- "+pangIpList.size()+" -- "+totalCount);
            isFirstSegment = false;
            currentIp = IpUtil.nextSegmentIp(currentIp);
        }

        return true;
    }

    // 处理单个PangIp列表循环
    private long processPangIpList(String startIp, String endIp,
            boolean isFirstSegment, List<String> pangIpList,
            IpBreakpoint breakpoint, BufferedWriter bw, ProxyIp currentProxy, String uniqueKey
    ) {
        long segmentTotalCount = 0;
        int pangSize = pangIpList.size();

        for (String pangIp : pangIpList) {
            // 任务停止
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理ip：" + pangIp);
                return segmentTotalCount;
            }
            if (IpUtil.isInternalIp(pangIp)) {
                log.debug("内网IP跳过查询: {}", pangIp);
                continue;
            }
            // 起始IP过滤
            if (isFirstSegment && IpUtil.ipLessThan(pangIp, startIp)) {
                log.debug("第一段过滤：{} < {}，跳过", pangIp, startIp);
                continue;
            }
            // 结束IP判断
            if (IpUtil.ipGreaterThan(pangIp, endIp)) {
                log.info("终点完成  -- {} -- 数量:{} -- 总计:{}", endIp, pangSize, segmentTotalCount);
                log.debug("pangIp:{} 超出结束IP:{}", pangIp, endIp);
                return segmentTotalCount;
            }
//            //跳过大ip 暂时先不用跳过了
//            if(IpRouteInit.IP_LEVEL_MAP.containsKey(pangIp))
//                continue;
            // 采集单个IP
            breakpoint.reset();
            retryExecute(uniqueKey, currentProxy, breakpoint, pangIp, bw, 15);
            segmentTotalCount += breakpoint.getCurrentCount();
        }

        return segmentTotalCount;
    }


    @Override
    protected boolean doExecute(String ip, BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint) throws IOException {
        return normalExecutor.execute(ip, bw, currentProxy, breakpoint);
    }

    public boolean ExceedStop(String currentIp, String endIp) {
        return IpUtil.isCurrentIpExceedEndIp(currentIp, endIp);
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
    }

}