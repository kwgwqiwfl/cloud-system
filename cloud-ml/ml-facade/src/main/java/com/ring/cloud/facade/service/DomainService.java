package com.ring.cloud.facade.service;

import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;

import static com.ring.cloud.facade.util.FileUtil.readFileToList;

@Slf4j
@Component
public class DomainService extends SeaCommon {

    @Autowired
    private GlobalProgressManager progressManager;

    @Value("${ml.client.domain.file.path:/}")
    private String exportDomainDir;
    @Value("${ml.client.domain.file.allname:/}")
    private String exportAllDomainFilename;
    @Value("${ml.client.subdomain.output.path:/}")
    private String subdomainOutPath;

    /**
     * 导入域名文件 → 小写 + 去重 + 最多10线程 + 监控进度 + 每个线程执行完成才结束
     */
    public int subDomainsByDomain(MultipartFile file) {
        String taskKey = "domain_sub_import_task";
        List<String> dataList = readFileToList(file);
        int totalCount = dataList.size();
        WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, "采集启动 总量：" + totalCount);

        int maxThreadCount = 20;
        checkAndLockTask(TaskTypeEnum.DOMAIN_SUB, taskKey);
        try {

            // ====================== 最终线程数（取 数据量、限制线程数 最小值） ======================
            int threadCount = Math.min(totalCount, maxThreadCount);

            // 初始化进度
            progressManager.initTask(taskKey, threadCount, totalCount);

            int batchSize = totalCount / threadCount;
            String timeStamp = DateUtil.fileSuffixSDF.format(new Date());
            // 分片提交线程
            for (int i = 0; i < threadCount; i++) {
                int start = i * batchSize;
                int end = (i == threadCount - 1) ? totalCount : (i + 1) * batchSize;
                List<String> subList = dataList.subList(start, end);

                TaskEntity task = new TaskEntity();
                task.setTaskType(TaskTypeEnum.DOMAIN_SUB.name());
                task.setHandleKeyList(subList);
                task.setSite(String.valueOf(i));
                task.setTimeStamp(timeStamp);
                task.setOutPath(subdomainOutPath);
                handlerExecutor.execHandler(factory, progressManager, task);
            }
            return dataList.size();
        } catch (Exception e) {
            WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, "导入失败！ 信息：" + e.getMessage());
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException(TaskTypeEnum.DOMAIN_SUB.name() + "导入任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 导入域名文件 → 小写 + 去重 + 最多10线程 + 监控进度 + 每个线程插入数据完成才结束
     */
    public int importDomainFile(MultipartFile file) {
        return commonImportFile(file, TaskTypeEnum.DOMAIN, WsMessageType.DOMAIN_TASK, "domain_import_task", 10);
    }
    /**
     * 导出域名数据 按域名分文件
     */
    public String exportDomainData(List<String> inputDomainList) {
        exportService.exportDomainData(inputDomainList, exportDomainDir);
        return exportDomainDir;
    }
    /**
     * 导出域名数据
     */
    public String exportAllDomainData() {
        exportService.exportAllDomainData(exportAllDomainFilename);
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