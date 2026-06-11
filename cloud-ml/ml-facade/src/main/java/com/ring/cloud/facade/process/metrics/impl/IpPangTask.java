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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
        // 输出文件 start + "-" + end
        String csvPath = ipPangPath + "/" + start + "-" + end + ".csv";
        String tmpPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpPath);
        final BufferedWriter bw = initBufferedWriter(tmpPath);

        // 记录成功文件 start + "-" + end
        String succPath = ipPangPath + "/" + start + "-" + end + "-succ.csv";
        String succTmpPath = succPath + ".tmp";
        FileUtil.forceCreateFile(succTmpPath);
        final BufferedWriter succBw = initBufferedWriter(succTmpPath);

        // 记录失败文件 start + "-" + end
        String errorTxtPath = ipPangPath + "/" + start + "-" + end + "-error.txt";
        String errorTmpPath = errorTxtPath + ".tmp";
        FileUtil.forceCreateFile(errorTmpPath);
        final BufferedWriter errorBw = initBufferedWriter(errorTmpPath);

        try {
            // 执行采集逻辑
            return doBatchCrawl(start, end, bw, succBw, errorBw, uniqueKey);
        }catch (Exception e){
            log.error("任务失败，",e);
            return false;
        }finally {
            // 统一关闭文件
            closeFileAndRenameByPath(bw, tmpPath, csvPath, true);
            closeFileAndRenameByPath(succBw, succTmpPath, succPath, true);
            closeFileAndRenameByPath(errorBw, errorTmpPath, errorTxtPath, true);
        }
    }

    /**
     * 批量采集IP段任务执行器
     */
    private boolean doBatchCrawl(int start, int end, BufferedWriter bw, BufferedWriter succBw, BufferedWriter errorBw, String uniqueKey) throws Exception {
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
            log.info("[{}]开始处理ip首段：{}，待处理IP段数：{}", uniqueKey, i, totalSeg);

            AtomicBoolean isFallback = new AtomicBoolean(false);//标记最终重试失败
            for (String segIp : ipSet) {
                isFallback.set(false);
                List<PangIpData> pangIpList = queryWithRetry(
                        segIp,
                        currentProxy,
                        3,
                        (k, p) -> pangIpServcie.pangIpCountNoRetry(k, p),
                        // 兜底逻辑，命中则标记为查询失败
                        k -> {
                            isFallback.set(true);
                            return new ArrayList<>();
                        }
                );
                if (isFallback.get()) {// 记录失败
                    errorBw.write(segIp);
                    errorBw.newLine();
                    segErr++;
                    continue;
                }
                segSucc++;
                int size = pangIpList.size();
                //记录数据量
                succBw.write(segIp+","+size);
                succBw.newLine();

                if (size>0) {// 记录输出
                    sb.setLength(0);
                    for (PangIpData pangIpData : pangIpList) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(pangIpData.getIp()).append(",").append(pangIpData.getCount());
                    }
                    bw.write(sb.toString());
                    bw.newLine();
                }

                if (segSucc % 20 == 0) {
                    log.info("首段{}成功处理{}个ip段", i, segSucc);
                }
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