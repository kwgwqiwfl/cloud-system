package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpSingleExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;

@Slf4j
@Component
public class ipSingleTask extends AbstractTask<TaskEntity> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_SINGLE;
    }

    @Autowired
    private IpSingleExecutor smallExecutor;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(TaskEntity ipTaskEntity) {
        String handleIp = ipTaskEntity.getHandleKey();
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
    private boolean crawlSingleIp(TaskEntity ipTaskEntity, String uniqueKey) throws IOException {
        String handleIp = ipTaskEntity.getHandleKey();
        // 独立文件名：前缀_IP.csv   test_112.112.112.112.csv
        String fileName = ipFileNamePrefix + "_" + handleIp + ".csv";
        String csvPath = FileUtil.ipCsvFileName(ipFilePath, fileName);
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);
        BufferedWriter bw = null;
        try{
            bw = initCsvWriter(tmpCsvPath);
            IpBreakpoint breakpoint = new IpBreakpoint();
            retryExecute(uniqueKey, null, breakpoint, handleIp, bw, 15);
//            crawlSingleIpWithRetry(handleIp, bw, null, breakpoint, uniqueKey);
            return true;
        } finally {
            closeFileAndRenameByPath(bw, tmpCsvPath, csvPath, true);
        }
    }
    @Override
    protected boolean doExecute(String ip, BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint) throws IOException {
        return smallExecutor.execute(ip, bw, currentProxy, breakpoint);
    }


    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;//预留外部停止
    }
}