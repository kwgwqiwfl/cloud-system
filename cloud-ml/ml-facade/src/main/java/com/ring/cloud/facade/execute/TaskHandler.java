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

            // ====================== 批量任务：统一计数 + 最后释放锁 ======================
            if (identity.isNeedFinishAllRelease()) {
                // 每个线程 成功/失败 都只计数1次（标准用法）
                progressManager.onSegmentFinish(lockKey, 1);

                GlobalProgress progress = progressManager.getProgress(lockKey);
                if (progress != null) {
                    // 推送前端进度
                    String msg = "任务进度：" + progress.getFinishedSegments().get() + "/" + progress.getTotalSegments().get();
                    WsUtil.push(WsMessageType.DOMAIN_TASK, msg);

                    // 只有全部完成 + 第一个抢到标记的线程 才释放锁
                    if (progress.getFinishedSegments().get() == progress.getTotalSegments().get()) {
                        if (progress.getReleased().compareAndSet(false, true)) {
                            log.info("==========================================================");
                            log.info("✅ 批量任务全部执行完成，释放全局锁：{}", lockKey);
                            log.info("==========================================================");
                            GlobalTaskManager.releaseSegment(lockKey);
                        }
                    }
                }
            }
            // ====================== 普通单线程任务：立即释放 ======================
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