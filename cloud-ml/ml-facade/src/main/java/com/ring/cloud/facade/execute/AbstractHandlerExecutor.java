package com.ring.cloud.facade.execute;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;


@Slf4j
public abstract class AbstractHandlerExecutor {
    
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    public AbstractHandlerExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }
    
//    public void execute(IHandler handler, long interval, boolean loop){
//        if (log.isDebugEnabled()) {
//            log.debug("execute Handler:{}, loop :{}", handler.getClass().getName(), loop);
//        }
//        threadPoolTaskExecutor.execute(new HandlerRunnable(handler, interval, loop, null));
//    }
//
//    public void execute(IHandler handler, long interval, CountDownLatch latch){
//        threadPoolTaskExecutor.execute(new HandlerRunnable(handler, interval, true, latch));
//    }

    public void execute(IHandler handler){
        threadPoolTaskExecutor.execute(new HandlerRunnable(handler, false));
    }

    public void execute(Runnable runnable) {
        threadPoolTaskExecutor.execute(runnable);
    }
}
