package com.ring.cloud.facade.execute;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class HandlerRunnable implements Runnable{
    
    private IHandler handler;
    private boolean loop;
//    private long interval;
//    private CountDownLatch downLatch;
//
//    public HandlerRunnable(IHandler handler, long interval, boolean loop, CountDownLatch downLatch) {
//        this.handler = handler;
//        this.loop = loop;
//        this.interval = interval;
//        this.downLatch = downLatch;
//    }
    public HandlerRunnable(IHandler handler, boolean loop) {
        this.handler = handler;
        this.loop = loop;
    }
    @Override
    public void run() {
        if (loop) {
            while (true) {
                if (runOnce())
                    break;
            }
        } else {
            runOnce();
        }
//        if(downLatch!=null)
//            this.downLatch.countDown();
    }
    
    private boolean runOnce(){
        try {
//            Thread.sleep(2000);
            return handler.handle();
        } catch (Exception e) {
            throw new RuntimeException(String.format("JobStatusHandler execute error : %s", e.getMessage()), e);
        }
    }
}
