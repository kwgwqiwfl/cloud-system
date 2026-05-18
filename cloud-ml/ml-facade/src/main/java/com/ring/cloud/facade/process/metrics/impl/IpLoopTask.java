package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class IpLoopTask extends AbstractTask<TaskEntity> implements StopCondition {

    // ====================== 【你必须配置的IP文件根目录】 ======================
    @Value("${ml.client.loopip.file.path:/}")
    private String ipLoopPath;

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.IP_LOOP;
    }

    @Autowired
    private IpSegExecutor normalExecutor;

    // ========== ITask 接口实现 ==========
    @Override
    public boolean runTask(TaskEntity task) {
        String uniqueKey = "ip_loop_task";
        try {
            return batchCrawl(task, uniqueKey);
        } catch (Throwable e) {
            log.error("分段任务失败："+e.getMessage(),e);
            return false;
        }
    }

    // loop采集控制逻辑 - 主方法
    public boolean batchCrawl(TaskEntity task, String uniqueKey) throws IOException {
        List<Integer> fileNoList = task.getFileNoList();

        // 初始化文件（用第一个文件编号做输出文件名）
        String csvPath = ipFilePath + "/" + fileNoList.get(0) + ".csv";
        String tmpCsvPath = csvPath + ".tmp";
        FileUtil.forceCreateFile(tmpCsvPath);
        final BufferedWriter bw = initBufferedWriter(tmpCsvPath);

        try {
            // 执行采集逻辑
            return doBatchCrawl(fileNoList, bw, uniqueKey);
        } finally {
            // 统一关闭文件
            closeFileAndRenameByPath(bw, tmpCsvPath, csvPath, true);
        }
    }

    // 核心采集循环
    private boolean doBatchCrawl(List<Integer> fileNoList, BufferedWriter bw, String uniqueKey) {
        IpBreakpoint breakpoint = new IpBreakpoint();
        ProxyIp currentProxy = getAvailableProxy();

        for(Integer fileNo : fileNoList){
            // 任务停止判断
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理文件编号：" + fileNo);
                return true;
            }

            // ====================== 【补全】根据文件编号读取IP文件 ======================
            List<String> ipList = readIpFileByNo(fileNo);

            // 处理单个文件
            long totalCount = processFileIpList(fileNo, ipList, breakpoint, bw, currentProxy, uniqueKey);

            // 日志 & 推送
            log.info("文件编号：{} -- 处理IP数：{} -- 采集成功数：{}", fileNo, ipList.size(), totalCount);
            WsUtil.push(WsMessageType.LOOP_TASK, "文件编号："+ fileNo+" -- 处理IP数："+ipList.size()+" -- 成功："+totalCount);
        }

        return true;
    }

    // 处理单文件列表循环
    private long processFileIpList(int fileNo, List<String> ipList,
                                   IpBreakpoint breakpoint, BufferedWriter bw, ProxyIp currentProxy, String uniqueKey
    ) {
        long segmentTotalCount = 0;
        int count = 0;
        for (String currentIp : ipList) {
            // 任务停止
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理ip：" + currentIp);
                return segmentTotalCount;
            }
            if (IpUtil.isInternalIp(currentIp)) {
                log.debug("内网IP跳过查询: {}", currentIp);
                continue;
            }
            // 采集单个IP
            breakpoint.reset();
            retryExecute(uniqueKey, currentProxy, breakpoint, currentIp, bw, 15);
            segmentTotalCount += breakpoint.getCurrentCount();

            count++;
            if (count % 100 == 0) {
                log.info("文件[{}]进度：{}，最近IP=" +
                                " {}，累计结果={}",
                        fileNo, count, currentIp, segmentTotalCount);
            }
        }

        return segmentTotalCount;
    }


    @Override
    protected boolean doExecute(String ip, BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint) throws IOException {
        return normalExecutor.execute(ip, bw, currentProxy, breakpoint);
    }

    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
    }

    private List<String> readIpFileByNo(Integer fileNo) {
        List<String> ipList = new ArrayList<>();
        String fileName = String.format("ip_%03d.txt", fileNo);
        String filePath = ipLoopPath + "/" + fileName;

        // ====================== 修复：文件不存在直接跳过，不报错 ======================
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("IP文件不存在，跳过处理：{}", filePath);
            return ipList; // 返回空集合，不影响流程
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String ip = line.trim();
                if (!ip.isEmpty()) {
                    ipList.add(ip);
                }
            }

        } catch (Exception e) {
            log.error("读取IP文件失败：{} 错误：{}", filePath, e.getMessage(), e);
            throw new RuntimeException("读取IP文件失败：" + filePath, e);
        }

        if (ipList.isEmpty()) {
            log.warn("IP文件无有效数据：{}", filePath);
        }

        return ipList;
    }

}