package com.ring.cloud.facade.service;

import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

import static com.ring.cloud.facade.util.FileUtil.readFileToList;

@Slf4j
@Component
public class KeywordService extends SeaCommon {

    @Value("${ml.client.keyword.desc.site}")
    private List<String> siteList;

    public int importKeywordFile(MultipartFile file) {
        String taskKey = "key_import_task";
        List<String> dataList = readFileToList(file);
        int totalCount = dataList.size();
        WsUtil.push(WsMessageType.KEYWORD_TASK, "🟢 关键词任务开始 | 总个数：" + totalCount);
        checkAndLockTask(TaskTypeEnum.KEYWORD, taskKey);
        try {
            int threadCount = siteList.size();
            progressManager.initTask(taskKey, threadCount, totalCount);
            String timeStamp = DateUtil.fileSuffixSDF.format(new Date());
            for (String site : siteList) {
                TaskEntity task = new TaskEntity();
                task.setTaskType(TaskTypeEnum.KEYWORD.name());
                task.setHandleKeyList(dataList);
                task.setSite(site);
                task.setTimeStamp(timeStamp);

                handlerExecutor.execHandler(factory, progressManager, task);
            }

            return totalCount;
        } catch (Exception e) {
            WsUtil.push(WsMessageType.KEYWORD_TASK, "🔴 关键词采集导入失败 | 原因：" + e.getMessage());
            GlobalTaskManager.releaseSegment(taskKey);
            throw new RuntimeException("关键词导入任务失败：" + e.getMessage(), e);
        }
    }

    public void startKeywordTask() {
    }

}