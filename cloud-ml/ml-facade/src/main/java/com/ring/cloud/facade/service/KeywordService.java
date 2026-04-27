package com.ring.cloud.facade.service;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KeywordService extends SeaCommon {

    @Value("${ml.client.keyword.desc.site}")
    private List<String> siteList;

    public int importKeywordFile(MultipartFile file) {
        String taskKey = "key_import_task";
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        List<String> dataList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String data = line.trim().toLowerCase();
                if (!data.isEmpty()) {
                    dataList.add(data);

                    // 行数限制
                    if (dataList.size() > 1000000) {
                        throw new RuntimeException("文件有效行数超出限制，最大允许导入 100 万行");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("导入文件读取失败：" + e.getMessage(), e);
        }

        if (dataList.isEmpty()) {
            throw new RuntimeException("文件中无有效数据");
        }

        // 全局任务防重
        if (GlobalTaskManager.isSegmentRunning(taskKey)) {
            throw new IllegalArgumentException(TaskTypeEnum.KEYWORD.name() + "导入任务正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(taskKey)) {
            throw new IllegalArgumentException(TaskTypeEnum.KEYWORD.name() + "导入任务加锁失败");
        }

        try {
            int totalCount = dataList.size();
            int threadCount = siteList.size();
            progressManager.initTask(taskKey, threadCount, totalCount);

            for (String site : siteList) {
                TaskEntity task = new TaskEntity();
                task.setTaskType(TaskTypeEnum.KEYWORD.name());
                task.setHandleKeyList(dataList);
                task.setSite(site);

                handlerExecutor.execHandler(factory, progressManager, task);
            }

            return dataList.size();
        } catch (Exception e) {
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException(TaskTypeEnum.KEYWORD.name() + "导入任务失败：" + e.getMessage(), e);
        }
    }

    public void startKeywordTask() {
    }

    //测试接口
    public void test(List<String> dataList) {
        String taskKey = "key_import_task";
        // 全局任务防重
        if (GlobalTaskManager.isSegmentRunning(taskKey)) {
            throw new IllegalArgumentException(TaskTypeEnum.KEYWORD.name() + "导入任务正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(taskKey)) {
            throw new IllegalArgumentException(TaskTypeEnum.KEYWORD.name() + "导入任务加锁失败");
        }

        try {
            int totalCount = dataList.size();
            int threadCount = siteList.size();
            progressManager.initTask(taskKey, threadCount, totalCount);

            for (String site : siteList) {
                TaskEntity task = new TaskEntity();
                task.setTaskType(TaskTypeEnum.KEYWORD.name());
                task.setHandleKeyList(dataList);
                task.setSite(site);

                handlerExecutor.execHandler(factory, progressManager, task);
            }

        } catch (Exception e) {
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException(TaskTypeEnum.KEYWORD.name() + "导入任务失败：" + e.getMessage(), e);
        }
    }
}