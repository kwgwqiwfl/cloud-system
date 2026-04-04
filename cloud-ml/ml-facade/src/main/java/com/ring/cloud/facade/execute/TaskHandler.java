package com.ring.cloud.facade.execute;

import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.config.IpGlobalProgressManager;
import com.ring.cloud.facade.entity.ip.IpGlobalProgress;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskHandler implements IHandler {
    private final TaskFactory factory;
    private final IpGlobalProgressManager progressManager;
    private final IpTaskEntity ipTaskEntity;

    public TaskHandler(TaskFactory factory, IpGlobalProgressManager progressManager, IpTaskEntity ipTaskEntity) {
        this.factory = factory;
        this.progressManager = progressManager;
        this.ipTaskEntity = ipTaskEntity;
        ipTaskEntity.setStartTime(System.currentTimeMillis());
    }

    /**
     * @return 是否执行结束循环; true-执行结束；false-未结束
     */
    @Override
    public boolean handle() {
        String taskType = ipTaskEntity.getTaskType();
        String uniqueKey;
        String lockKey = null;
        boolean isLargeIpTask = TaskTypeEnum.IP_DOMAIN_LARGE.name().equals(taskType);
        // ====================== IP单任务标记 ======================
        boolean isSmallIpTask = TaskTypeEnum.IP_DOMAIN_SMALL.name().equals(taskType);
        // 生成唯一KEY + 锁KEY
        if (isLargeIpTask) {
            String handleIp = ipTaskEntity.getHandleIp();
            uniqueKey = taskType + ":" + handleIp;
            lockKey = handleIp;
        }else if (isSmallIpTask) {
            String handleIp = ipTaskEntity.getHandleIp();
            uniqueKey = taskType + ":" + handleIp;
            lockKey = handleIp;
        } else {
            String segNo = ipTaskEntity.getIpSegment().getSegmentNo();
            uniqueKey = taskType + ":" + segNo;
            lockKey = segNo;
        }

        log.info("任务开始，唯一标识={}", uniqueKey);
        boolean status = false;

        try {
            GlobalTaskManager.TASK_STOP_MAP.put(uniqueKey, new AtomicBoolean(false));
            // 执行任务
            status = factory.taskManage(taskType).runTask(ipTaskEntity);

            log.info("任务结束 唯一标识={} 状态：{} 耗时：{} ms",
                    uniqueKey, status, (System.currentTimeMillis() - ipTaskEntity.getStartTime()));

        } catch (Throwable e) {
            log.error("任务执行异常 唯一标识={}", uniqueKey, e);

        } finally {
            // ====================== 统一处理：成功/异常 都走这里（合并重复逻辑） ======================
            if (isLargeIpTask) {
                int finishedPages = ipTaskEntity.getEndPage() - ipTaskEntity.getStartPage() + 1;
                progressManager.onSegmentFinish(lockKey, finishedPages);

                IpGlobalProgress progress = progressManager.getProgress(lockKey);
                if (progress != null && progress.getFinishedSegments().get() == progress.getTotalSegments().get()) {
                    // 统一打印整体完成
                    log.info("==========================================================");
                    log.info("✅ IP【{}】任务【全部完成】总分段:{} | 已完成:{}",
                            lockKey, progress.getTotalSegments().get(), progress.getFinishedSegments().get());
                    log.info("==========================================================");
                    log.info("✅ IP【{}】释放全局锁", lockKey);
                    GlobalTaskManager.releaseSegment(lockKey);
                }
            }

            // 普通任务：仅在这里释放锁
            if (lockKey != null && !isLargeIpTask) {
                GlobalTaskManager.releaseSegment(lockKey);
            }

            GlobalTaskManager.TASK_STOP_MAP.remove(uniqueKey);
        }

        return status;
    }
}
