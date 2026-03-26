package com.ring.cloud.facade.execute;

import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

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

    public void execHandler(IpTaskEntity ipTaskEntity){
//        log.debug("start get job state.....");
            execute(new TaskHandler(ipTaskEntity));
    }

}
