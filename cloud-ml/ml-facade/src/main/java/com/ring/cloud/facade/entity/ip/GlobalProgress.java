package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class GlobalProgress {
    private String taskKey;

    private AtomicInteger totalSegments = new AtomicInteger(0);
    private AtomicInteger finishedSegments = new AtomicInteger(0);
    private AtomicInteger currentRunning = new AtomicInteger(0);

    private AtomicLong totalPageEstimate = new AtomicLong(0);
    private AtomicLong totalPageFinished = new AtomicLong(0);

    private volatile boolean stopped = false;

    // 安全释放锁标记（标准必备）
    private AtomicBoolean released = new AtomicBoolean(false);
}