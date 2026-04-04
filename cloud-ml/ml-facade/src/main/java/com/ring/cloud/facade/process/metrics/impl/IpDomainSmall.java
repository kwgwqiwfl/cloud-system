package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpSmallExecutor;
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
public class IpDomainSmall extends TaskOperate<IpTaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_DOMAIN_SMALL;
    }

    @Autowired
    private IpSmallExecutor smallExecutor;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(IpTaskEntity ipTaskEntity) {
        String handleIp = ipTaskEntity.getHandleIp();
        // 唯一标识：类型 + IP
        String uniqueKey = taskEnum().name() + ":" + handleIp;
        try {
            // 标记分段开始
            return crawlSingleIp(ipTaskEntity, uniqueKey);
        } catch (Throwable e) {
            log.error("小ip运行失败 ip={}  异常：{}",
                    handleIp,
                    e.getMessage(), e);
            return false;
        }
    }

    // ========== 小ip 主逻辑 ==========
    private boolean crawlSingleIp(IpTaskEntity ipTaskEntity, String uniqueKey) throws IOException {
        String handleIp = ipTaskEntity.getHandleIp();
        // 独立文件名：前缀_IP.csv   test_112.112.112.112.csv
        String fileName = ipFileNamePrefix + "_" + handleIp + ".csv";
        String csvPath = FileUtil.ipCsvFileName(ipFilePath, fileName);
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);

        try (BufferedWriter bw = initCsvWriter(tmpCsvPath)) {
            // 执行分段采集（带重试 + 代理切换）
            crawlSingleIpWithRetry(ipTaskEntity.getLoc(), handleIp, bw, uniqueKey);
            return true;
        } finally {
            closeCsv(null, tmpCsvPath, csvPath, true);
        }
    }

    private void crawlSingleIpWithRetry(String loc,
                                       String handleIp,
                                       BufferedWriter bw,
                                       String uniqueKey) {
        boolean success = false;
        ProxyIp currentProxy = null;
        IpBreakpoint breakpoint = new IpBreakpoint();

        while (!success) {
            if (isTaskStopped(uniqueKey)) {
                log.info("【小ip任务】已停止 uniqueKey={}", uniqueKey);
                return;
            }

            if (currentProxy == null) {
                currentProxy = getAvailableProxy();
            }

            try {
                success = smallExecutor.singleIp(
                        loc, handleIp, currentProxy, bw, breakpoint);
                if (success) {
                    return;
                }
                currentProxy = null;
                log.debug("【小ip】从断点页 {} 继续", breakpoint.getCurrentPage());
            } catch (Throwable e) {
                boolean needSwitch = needSwitchProxy(e);
                if (needSwitch) {
                    currentProxy = null;
                } else {
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
