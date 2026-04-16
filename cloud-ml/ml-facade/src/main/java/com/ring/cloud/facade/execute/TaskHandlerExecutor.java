package com.ring.cloud.facade.execute;

import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.config.GlobalProgressManager;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskHandlerExecutor extends AbstractHandlerExecutor {

    public TaskHandlerExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        super(threadPoolTaskExecutor);
    }

//    public void execHandler(TaskExecutionPojo taskExecution, CountDownLatch latch){
////        log.debug("start get job state.....");
//        if(taskExecution.getType()== Constants.TASK_FLOW_TYPE)
//            execute(new JobStateHandler(taskExecution), DateUtil.FLOW_METRIC_INTERVAL, latch);
//    }

    public void execHandler(TaskFactory factory, GlobalProgressManager progressManager, TaskEntity ipTaskEntity){
//        log.debug("start get job state.....");
            execute(new TaskHandler(factory, progressManager, ipTaskEntity));
    }

    /**
     * 判断队列是否还能容纳指定数量的任务
     * 保证大IP任务要么全进，要么全不进
     */
    public boolean canAccept(int taskCount) {
        if (threadPoolTaskExecutor == null) {
            return false;
        }
        int remaining = threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity();
        return remaining >= taskCount;
    }

}
