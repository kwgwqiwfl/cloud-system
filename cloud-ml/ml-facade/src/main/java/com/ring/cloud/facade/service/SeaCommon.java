package com.ring.cloud.facade.service;

import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.execute.TaskHandlerExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class SeaCommon {
    @Autowired
    protected TaskHandlerExecutor handlerExecutor;
    @Autowired
    protected TaskFactory factory;
    @Autowired
    protected GlobalProgressManager progressManager;

    /**
     * 通用文件导入：小写 + 去重 + 自定义线程数 + 监控进度 + 线程执行完才结束
     * @param file 上传文件
     * @param taskType 任务类型
     * @param taskKey 任务锁key
     * @param maxThreadCount 最大开启线程数
     * @return 导入数量
     */
    protected int commonImportFile(MultipartFile file, TaskTypeEnum taskType, String taskKey, int maxThreadCount) {

        // ====================== 新增容错：文件不能为空 ======================
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 1. 读取文件 → 小写 → 去重
        Set<String> dataSet = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String data = line.trim().toLowerCase();
                if (!data.isEmpty()) {
                    dataSet.add(data);

                    // ====================== 新增容错：最大行数 50 万 ======================
                    if (dataSet.size() > 500000) {
                        throw new RuntimeException("文件有效行数超出限制，最大允许导入 50 万行");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("导入文件读取失败：" + e.getMessage(), e);
        }

        List<String> dataList = new ArrayList<>(dataSet);
        if (dataList.isEmpty()) {
            throw new RuntimeException("文件中无有效数据");
        }

        // ====================== 新增容错：线程数安全限制（1~10） ======================
        if (maxThreadCount < 1) {
            maxThreadCount = 1;
        }
        if (maxThreadCount > 10) {
            maxThreadCount = 10;
        }

        // ====================== 全局导入任务防重 ======================
        if (GlobalTaskManager.isSegmentRunning(taskKey)) {
            throw new IllegalArgumentException(taskType.name() + "导入任务正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(taskKey)) {
            throw new IllegalArgumentException(taskType.name() + "导入任务加锁失败");
        }

        try {
            int totalCount = dataList.size();

            // ====================== 最终线程数（取 数据量、限制线程数 最小值） ======================
            int threadCount = Math.min(totalCount, maxThreadCount);

            // 初始化进度
            progressManager.initTask(taskKey, threadCount, totalCount);

            int batchSize = totalCount / threadCount;

            // 分片提交线程
            for (int i = 0; i < threadCount; i++) {
                int start = i * batchSize;
                int end = (i == threadCount - 1) ? totalCount : (i + 1) * batchSize;
                List<String> subList = dataList.subList(start, end);

                TaskEntity task = new TaskEntity();
                task.setTaskType(taskType.name());
                task.setHandleKeyList(subList);

                handlerExecutor.execHandler(factory, progressManager, task);
            }
            return dataList.size();
        } catch (Exception e) {
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException(taskType.name() + "导入任务失败：" + e.getMessage(), e);
        }
    }

}
