package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.IpGlobalProgressManager;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpLargeIpExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.TaskOperate;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;

@Slf4j
@Component
public class IpDomainLarge extends TaskOperate<IpTaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_DOMAIN_LARGE;
    }

    @Autowired
    private IpLargeIpExecutor largeExecutor;
    @Autowired
    private IpGlobalProgressManager progressManager;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(IpTaskEntity ipTaskEntity) {
        String handleIp = ipTaskEntity.getHandleIp();
        // 唯一标识：类型 + IP
        String uniqueKey = taskEnum().name() + ":" + handleIp;

        try {
            // 标记分段开始
            progressManager.onSegmentStart(handleIp);
            return batchCrawlLarge(ipTaskEntity, uniqueKey);
        } catch (Throwable e) {
            log.error("[大IP分段-异常缺数] ip={} page={}-{} 异常：{}",
                    handleIp,
                    ipTaskEntity.getStartPage(),
                    ipTaskEntity.getEndPage(),
                    e.getMessage(), e);
            // 失败：进度+0
            progressManager.onSegmentFinish(handleIp, 0);
            return false;
        }
    }

    // ========== 大IP 单个分段 主逻辑 ==========
    private boolean batchCrawlLarge(IpTaskEntity ipTaskEntity, String uniqueKey) throws IOException {
        String handleIp = ipTaskEntity.getHandleIp();
        Integer startPage = ipTaskEntity.getStartPage();
        Integer endPage = ipTaskEntity.getEndPage();

        int pages = endPage - startPage + 1;

        log.debug("【大IP分段开始】ip={} | {}-{}", handleIp, startPage, endPage);

        // 独立文件名：前缀_IP_页码.csv   test_112.112.112.112_1-500.csv
        String fileName = ipFileNamePrefix + "_" + handleIp + "_" + startPage + "-" + endPage + ".csv";
        String csvPath = FileUtil.ipCsvFileName(ipFilePath, fileName);
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);

        try (BufferedWriter bw = initCsvWriter(tmpCsvPath)) {
            // 执行分段采集（带重试 + 代理切换）
            crawlSegmentWithRetry(ipTaskEntity.getLoc(), handleIp, startPage, endPage, bw, uniqueKey);
            log.debug("【大IP分段任务完成】 ip={} | page={}-{}",
                     handleIp, startPage, endPage);
            // 标记分段完成 + 上报页数
            progressManager.onSegmentFinish(handleIp, pages);
            return true;

        } finally {
            closeCsv(null, tmpCsvPath, csvPath, true);
        }
    }

    private void crawlSegmentWithRetry(String loc,
                                       String handleIp,
                                       int startPage,
                                       int endPage,
                                       BufferedWriter bw,
                                       String uniqueKey) {
        boolean success = false;
        ProxyIp currentProxy = null;
        int currentStartPage = startPage;
        IpBreakpoint breakpoint = new IpBreakpoint();

        while (!success) {
            if (isTaskStopped(uniqueKey)) {
                log.debug("【大IP分段任务】已停止 uniqueKey={}", uniqueKey);
                progressManager.onSegmentFinish(handleIp, 0);
                return;
            }

            if (currentProxy == null) {
                currentProxy = getAvailableProxy();
            }

            try {
                success = largeExecutor.crawlSegment(
                        loc, handleIp, currentProxy, bw, currentStartPage, endPage, breakpoint);
                if (success) {
                    log.info("【分段】ip={} | page={}-{} | 总条数：{}",
                            handleIp, startPage, endPage, breakpoint.getCurrentCount());
                    log.debug("【大IP分段任务完成】 ip={} | page={}-{}", handleIp, startPage, endPage);
                    return;
                }
                currentProxy = null;
                log.debug("【大IP续爬】从断点页 {} 继续", breakpoint.getCurrentPage());
            } catch (Throwable e) {
                boolean needSwitch = needSwitchProxy(e);
                if (needSwitch) {
                    currentProxy = null;
                    log.debug("【大IP分段-代理异常】切换代理: {}", e.getMessage());
                } else {
                    log.debug("【大IP分段-目标波动】不切换代理: {}", e.getMessage());
                }
            }
        }
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;//预留外部停止
    }
}
