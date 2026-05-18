package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.PangIpData;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.IpSegExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        String uniqueKey = "ip_pang_task";
        try {
            return batchCrawl(task, uniqueKey);
        } catch (Throwable e) {
            log.error("分段任务失败："+e.getMessage(),e);
            return false;
        }
    }

    // pang采集控制逻辑 - 主方法
    public boolean batchCrawl(TaskEntity task, String uniqueKey) throws IOException {
        List<Integer> numList = task.getFileNoList();

        // 初始化文件（用第一个seg编号做输出文件名）
        String csvPath = ipPangPath + "/" + numList.get(0) + ".csv";
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);
        final BufferedWriter bw = initBufferedWriter(tmpCsvPath);

        try {
            // 执行采集逻辑
            return doBatchCrawl(numList, bw, uniqueKey);
        }catch (Exception e){
            log.error("任务失败，",e);
            return false;
        }finally {
            // 统一关闭文件
            closeFileAndRenameByPath(bw, tmpCsvPath, csvPath, true);
        }
    }

    /**
     * 批量采集IP段任务执行器
     */
    private boolean doBatchCrawl(List<Integer> numList, BufferedWriter bw, String uniqueKey) throws Exception {
        // 前置参数校验
        if (numList == null || numList.isEmpty() || bw == null) {
            log.warn("任务[{}]参数异常，终止执行", uniqueKey);
            return false;
        }

        ProxyIp proxyIp = getAvailableProxy();
        int lastIndex = numList.size() - 1;
        String currentIpSegment = String.format("%d.0.0.0", numList.get(0));
        String endIpSegment = String.format("%d.255.255.0", numList.get(lastIndex));

        // 批量写入缓冲
        List<String> dataBuffer = new ArrayList<>(FLUSH_BATCH_SIZE);
        while (!isTaskStopped(uniqueKey) && !ipExceedStop(currentIpSegment, endIpSegment)) {
            List<PangIpData> pangIpList = fetchPangIpWithRetry(currentIpSegment, proxyIp);

            if (pangIpList.isEmpty()) {
                log.debug("任务[{}]当前IP段{}无数据，跳过", uniqueKey, currentIpSegment);
                currentIpSegment = IpUtil.nextSegmentIp(currentIpSegment);
                continue;
            }

            // 加入缓冲
            for (PangIpData data : pangIpList) {
                dataBuffer.add(data.getIp() + "," + data.getCount());
            }

            // 达到批量大小，一次性写入
            if (dataBuffer.size() >= FLUSH_BATCH_SIZE) {
                batchWrite(bw, dataBuffer);
                bw.flush();
            }

            // 切换下一段
            currentIpSegment = IpUtil.nextSegmentIp(currentIpSegment);
        }

        // 最后把缓冲里剩余的数据写入
        if (!dataBuffer.isEmpty()) {
            batchWrite(bw, dataBuffer);
            dataBuffer.clear();
        }
        return true;
    }

    /**
     * 带重试获取Pang IP列表
     */
    private List<PangIpData> fetchPangIpWithRetry(String ipSegment, ProxyIp proxyIp) {
        return queryWithRetry(
                ipSegment,
                proxyIp,
                3,
                pangIpServcie::pangIpCountNoRetry,
                null
        );
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
    }

}