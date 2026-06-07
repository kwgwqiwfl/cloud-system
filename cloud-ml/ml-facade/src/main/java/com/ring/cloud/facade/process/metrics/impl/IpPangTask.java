package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.PangIpData;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class IpPangTask extends AbstractTask<TaskEntity> implements StopCondition {

    @Value("${ml.client.pangip.output.path:/}")
    private String ipPangPath;

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_PANG;
    }

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(TaskEntity task) {
        String uniqueKey = task.getTaskType() + ":thread_" + Thread.currentThread().getId();
        try {
            return batchCrawl(task, uniqueKey);
        } catch (Throwable e) {
            log.error("ip pang任务失败："+e.getMessage(),e);
            return false;
        }
    }

    // pang采集控制逻辑 - 主方法
    public boolean batchCrawl(TaskEntity task, String uniqueKey) throws IOException {
        int start = task.getStartPage();
        int end = task.getEndPage();
        // 文件 start + "-" + end
        String csvPath = ipPangPath + "/" + start + "-" + end + ".csv";
        String tmpPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpPath);
        final BufferedWriter bw = initBufferedWriter(tmpPath);

        String errorTxtPath = ipPangPath + "/" + start + "-" + end + "-error.txt";
        String errorTmpPath = errorTxtPath + ".tmp";
        FileUtil.forceCreateFile(errorTmpPath);
        final BufferedWriter errorBw = initBufferedWriter(errorTmpPath);

        try {
            // 执行采集逻辑
            return doBatchCrawl(start, end, bw, errorBw, uniqueKey);
        }catch (Exception e){
            log.error("任务失败，",e);
            return false;
        }finally {
            // 统一关闭文件
            closeFileAndRenameByPath(bw, tmpPath, csvPath, true);
            closeFileAndRenameByPath(errorBw, errorTmpPath, errorTxtPath, true);
        }
    }

    /**
     * 批量采集IP段任务执行器
     */
    private boolean doBatchCrawl(int start, int end, BufferedWriter bw, BufferedWriter errorBw, String uniqueKey) throws Exception {
        // 前置参数校验
        if (start < 0 || end < 0 || end > 255 || start > end || bw == null || errorBw == null) {
            log.warn("任务[{}]参数异常，终止执行", uniqueKey);
            return false;
        }
        ProxyIp currentProxy = getAvailableProxy();
        StringBuilder sb = new StringBuilder(128);
        long totalAllErr = 0;
        long totalAllSucc = 0;

        for (int i = start; i <= end; i++) {
            Set<String> ipSet = IpUtil.ipSegSetByNum(i);
            int totalSeg = ipSet.size(); // 实际剩余有效IP(剔除内网后≠65536)
            long segSucc = 0;
            long segErr = 0;
            log.info("任务[{}]开始处理网段首段：{}，本段待处理IP总数：{}", uniqueKey, i, totalSeg);

            for (String segIp : ipSet) {
                List<PangIpData> pangIpList = queryWithRetry(
                        segIp,
                        currentProxy,
                        3,
                        pangIpServcie::pangIpCountNoRetry,
                        null
                );
                // 接口无数据=失败
                if (pangIpList == null || pangIpList.isEmpty()) {
                    log.info(segIp+"----null");
                    errorBw.write(segIp);
                    errorBw.newLine();
                    segErr++;
                    continue;
                }
                sb.setLength(0);
                for (PangIpData pangIpData : pangIpList) {
                    log.info(segIp+"----"+pangIpList.size());
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(pangIpData.getIp()).append(",").append(pangIpData.getCount());
                }
                bw.write(sb.toString());
                bw.newLine();
                segSucc++;
            }
            // 单段结束刷缓冲区
            bw.flush();
            totalAllSucc += segSucc;
            totalAllErr += segErr;
            log.info("任务[{}]网段{}处理完成，本段总量:{}，成功:{}，失败:{}，成功率:{}%",
                    uniqueKey, i, totalSeg, segSucc, segErr,
                    totalSeg == 0 ? 0 : String.format("%.2f", segSucc * 100.0 / totalSeg));
        }
        // 全部任务收尾落盘
        bw.flush();
        errorBw.flush();
        log.info("任务[{}]全量处理完毕，区间[{}-{}]，总成功:{}，总失败:{}", uniqueKey, start, end, totalAllSucc, totalAllErr);
        return true;
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
    }

}