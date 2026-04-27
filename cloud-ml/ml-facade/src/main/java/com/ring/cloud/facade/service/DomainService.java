package com.ring.cloud.facade.service;

import com.ring.cloud.core.service.DomainInoutService;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DomainService extends SeaCommon {

    @Autowired
    private DomainInoutService domainInoutService;
    @Autowired
    private GlobalProgressManager progressManager;

    @Value("${ml.client.domain.file.path:/}")
    private String exportDomainDir;
    @Value("${ml.client.domain.file.allname:/}")
    private String exportAllDomainFilename;

    /**
     * 导入域名文件 → 小写 + 去重 + 最多10线程 + 监控进度 + 每个线程插入数据完成才结束
     */
    public int importDomainFile(MultipartFile file) {
        return commonImportFile(file, TaskTypeEnum.DOMAIN, "domain_import_task", 10);
    }

    /**
     * 导出域名数据 按域名分文件
     */
    public String exportDomainData(List<String> inputDomainList) {
        domainInoutService.exportDomainData(inputDomainList, exportDomainDir);
        return exportDomainDir;
    }
    /**
     * 导出域名数据
     */
    public String exportAllDomainData() {
        domainInoutService.exportAllDomainData(exportAllDomainFilename);
        return exportAllDomainFilename;
    }

    public void test(List<String> domainList) {
        if (domainList.isEmpty()) {
            throw new RuntimeException("文件中无有效域名");
        }

        // ====================== 全局导入任务防重 ======================
        String taskKey = "domain_import_task";
        if (GlobalTaskManager.isSegmentRunning(taskKey)) {
            throw new IllegalArgumentException("域名导入任务正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(taskKey)) {
            throw new IllegalArgumentException("域名导入任务加锁失败");
        }

        try {
            int totalCount = domainList.size();
            // 最多 10 个线程，不足则用实际数量
            int threadCount = Math.min(totalCount, 10);

            // ====================== 初始化通用进度 ======================
            progressManager.initTask(taskKey, threadCount, totalCount);

            int batchSize = totalCount / threadCount;

            // ====================== 平均拆分提交线程 ======================
            for (int i = 0; i < threadCount; i++) {
                int start = i * batchSize;
                int end = (i == threadCount - 1) ? totalCount : (i + 1) * batchSize;
                List<String> subDomains = domainList.subList(start, end);

                // 只保留你有的字段
                TaskEntity task = new TaskEntity();
                task.setTaskType(TaskTypeEnum.DOMAIN.name());
                task.setHandleKeyList(subDomains);

                // 提交线程池
                handlerExecutor.execHandler(factory, progressManager, task);
            }

        } catch (Exception e) {
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException("域名导入任务失败", e);
        }
    }
}