package com.ring.cloud.facade.service;

import com.ring.cloud.core.service.ExportService;
import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.execute.TaskHandlerExecutor;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
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

import static com.ring.cloud.facade.util.FileUtil.readFileToList;

@Slf4j
public abstract class SeaCommon {
    @Autowired
    protected TaskHandlerExecutor handlerExecutor;
    @Autowired
    protected TaskFactory factory;
    @Autowired
    protected GlobalProgressManager progressManager;
    @Autowired
    protected ExportService exportService;

    /**
     * 通用文件导入：小写 + 去重 + 自定义线程数 + 监控进度 + 线程执行完才结束
     * @param file 上传文件
     * @param taskType 任务类型
     * @param taskKey 任务锁key
     * @param maxThreadCount 最大开启线程数
     * @return 导入数量
     */
    protected int commonImportFile(MultipartFile file, TaskTypeEnum taskType, WsMessageType wsType, String taskKey, int maxThreadCount) {
        List<String> dataList = readFileToList(file);
        int totalCount = dataList.size();
        WsUtil.push(wsType, "采集启动 总量：" + totalCount);

        // ====================== 新增容错：线程数安全限制（1~20） ======================
        maxThreadCount = Math.max(1, Math.min(20, maxThreadCount));
        checkAndLockTask(taskType, taskKey);
        try {

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
            WsUtil.push(wsType, "导入失败！ 信息：" + e.getMessage());
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException(taskType.name() + "导入任务失败：" + e.getMessage(), e);
        }
    }
    /**
     * 通用任务防重复校验
     */
    protected void checkAndLockTask(TaskTypeEnum taskType, String taskKey) {
        // 判断是否正在运行
        if (GlobalTaskManager.isSegmentRunning(taskKey)) {
            throw new IllegalArgumentException(taskType.name() + "导入任务正在运行，禁止重复启动");
        }
        // 尝试加锁
        if (!GlobalTaskManager.occupySegment(taskKey)) {
            throw new IllegalArgumentException(taskType.name() + "导入任务加锁失败");
        }
    }

}
