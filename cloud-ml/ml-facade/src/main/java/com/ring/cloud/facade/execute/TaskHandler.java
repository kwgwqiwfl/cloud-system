package com.ring.cloud.facade.execute;

import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.entity.ip.GlobalProgress;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.ip.TaskIdentity;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskHandler implements IHandler {

    private final TaskFactory factory;
    private final GlobalProgressManager progressManager;
    private final TaskEntity taskEntity;

    public TaskHandler(TaskFactory factory, GlobalProgressManager progressManager, TaskEntity ipTaskEntity) {
        this.factory = factory;
        this.progressManager = progressManager;
        this.taskEntity = ipTaskEntity;
        taskEntity.setStartTime(System.currentTimeMillis());
    }

    @Override
    public boolean handle() {
        TaskIdentity identity = buildTaskIdentity(taskEntity.getTaskType());
        boolean status = executeTask(identity);
        finishTask(identity, status);
        return status;
    }

    private TaskIdentity buildTaskIdentity(String taskType) {
        boolean isLargeIp = TaskTypeEnum.IP_DOMAIN_LARGE.name().equals(taskType);
        boolean isSmallIp = TaskTypeEnum.IP_SINGLE.name().equals(taskType);
        boolean isDomainBatch = TaskTypeEnum.DOMAIN.name().equals(taskType);
        boolean isKeywordBatch = TaskTypeEnum.KEYWORD.name().equals(taskType);
        boolean isIpLoopBatch = TaskTypeEnum.IP_LOOP.name().equals(taskType);
        boolean isDomainSubBatch = TaskTypeEnum.DOMAIN_SUB.name().equals(taskType);
        boolean isIpPangBatch = TaskTypeEnum.IP_PANG.name().equals(taskType);

        String uniqueKey;
        String lockKey = null;

        if (isLargeIp) {
            String handleIp = taskEntity.getHandleKey();
            uniqueKey = taskType + ":" + handleIp;
            lockKey = handleIp;
        }
        else if (isSmallIp) {
            String handleIp = taskEntity.getHandleKey();
            uniqueKey = taskType + ":" + handleIp;
            lockKey = handleIp;
        }
        else if (isDomainBatch) {
            lockKey = "domain_import_task";
            uniqueKey = taskType + ":thread_" + Thread.currentThread().getId();
        }
        else if (isKeywordBatch) {
            lockKey = "key_import_task";
            uniqueKey = taskType + ":thread_" + Thread.currentThread().getId();
        }
        else if (isIpLoopBatch) {
            lockKey = "ip_loop_task";
            uniqueKey = taskType + ":thread_" + Thread.currentThread().getId();
        }
        else if (isIpPangBatch) {
            lockKey = "ip_pang_task";
            uniqueKey = taskType + ":thread_" + Thread.currentThread().getId();
        }
        else if (isDomainSubBatch) {
            lockKey = "domain_sub_import_task";
            uniqueKey = taskType + ":thread_" + Thread.currentThread().getId();
        }
        else {
            String segNo = taskEntity.getIpSegment().getSegmentNo();
            uniqueKey = taskType + ":" + segNo;
            lockKey = segNo;
        }

        TaskIdentity identity = new TaskIdentity();
        identity.setTaskType(taskType);
        identity.setUniqueKey(uniqueKey);
        identity.setLockKey(lockKey);
        identity.setLargeIpTask(isLargeIp);
        identity.setSmallIpTask(isSmallIp);
        identity.setDomainBatchTask(isDomainBatch);
        identity.setKeywordBatchTask(isKeywordBatch);
        identity.setIpLoopBatchTask(isIpLoopBatch);
        identity.setDomainSubBatchTask(isDomainSubBatch);
        identity.setIpPangBatchTask(isIpPangBatch);

        return identity;
    }

    private boolean executeTask(TaskIdentity identity) {
        log.info("任务开始，唯一标识={}", identity.getUniqueKey());
        try {
            GlobalTaskManager.TASK_STOP_MAP.put(identity.getUniqueKey(), new AtomicBoolean(false));
            return factory.getTask(identity.getTaskType()).runTask(taskEntity);
        } catch (Throwable e) {
            log.error("任务执行异常 唯一标识={}", identity.getUniqueKey(), e);
            WsUtil.push(WsMessageType.TASK, identity.getUniqueKey() + "任务失败：" + e.getMessage());
            return false;
        }
    }

    private void finishTask(TaskIdentity identity, boolean status) {
        String uniqueKey = identity.getUniqueKey();
        String lockKey = identity.getLockKey();

        try {
            long cost = System.currentTimeMillis() - taskEntity.getStartTime();
            log.info("任务结束 唯一标识={} 状态：{} 耗时：{}ms", uniqueKey, status, cost);
            WsUtil.push(WsMessageType.TASK, uniqueKey + "任务完成。耗时：" + cost + "ms");

            if (identity.isNeedFinishAllRelease()) {
                progressManager.onSegmentFinish(lockKey, 1);

                GlobalProgress progress = progressManager.getProgress(lockKey);
                if (progress != null) {
                    String msg = "任务进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();

                    if(identity.isDomainBatchTask()){
                        WsUtil.push(WsMessageType.DOMAIN_TASK, msg);
                    }
                    else if(identity.isKeywordBatchTask()){
                        String site = taskEntity.getSite();
                        String keywordMsg = "✅ " + site + " 子任务完成 | 总进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();
                        WsUtil.push(WsMessageType.KEYWORD_TASK, keywordMsg);
                    }
                    else if(identity.isIpLoopBatchTask()){
                        String ipMsg = "✅ IP 子任务完成 | 总进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();
                        WsUtil.push(WsMessageType.LOOP_TASK, ipMsg);
                    }
                    else if(identity.isIpPangBatchTask()){
                        String ipMsg = "✅ pang 子任务完成 | 总进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();
                        WsUtil.push(WsMessageType.PANG_TASK, ipMsg);
                    }
                    else if(identity.isDomainSubBatchTask()){
                        String ipMsg = "✅ 子域名 子任务完成 | 总进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();
                        WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, ipMsg);
                    }
                    if (progress.getFinishedSegments().get() == progress.getTotalSegments().get()) {
                        if (progress.getReleased().compareAndSet(false, true)) {
                            log.info("==========================================================");
                            log.info("✅ 批量任务全部执行完成，准备处理：{}", lockKey);
                            log.info("==========================================================");

                            try {
                                // ====================== 子域名：先合并文件 ======================
                                if (identity.isDomainSubBatchTask()) {
                                    try {
                                        log.info("[子域名全量完成] 开始执行文件合并...");
                                        FileUtil.mergeAllSubdomainFiles(taskEntity);

                                        WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, "✅ 子域名任务全部完成！文件合并处理完毕");
                                    } catch (Exception e) {
                                        log.error("[子域名合并文件] 执行失败，但任务仍会结束", e);
                                        WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, "❌ 子域名文件合并失败，但任务已全部结束");
                                    }
                                }
                            } finally {
                                // ====================== 无论如何 最终一定释放锁 ======================
                                log.info("✅ 最终释放全局锁：{}", lockKey);
                                GlobalTaskManager.releaseSegment(lockKey);
                            }
                        }

                    }
                }
            }
            else {
                if (lockKey != null) {
                    GlobalTaskManager.releaseSegment(lockKey);
                }
            }

        } finally {
            GlobalTaskManager.TASK_STOP_MAP.remove(uniqueKey);
        }
    }
}