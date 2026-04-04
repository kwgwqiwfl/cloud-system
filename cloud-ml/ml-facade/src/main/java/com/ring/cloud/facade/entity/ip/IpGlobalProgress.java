package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class IpGlobalProgress {
    /**
     * 目标大IP
     */
    private String ip;

    /**
     * 总分段数（探测后确定）
     */
    private AtomicInteger totalSegments = new AtomicInteger(0);

    /**
     * 已完成分段
     */
    private AtomicInteger finishedSegments = new AtomicInteger(0);

    /**
     * 正在运行的分段数
     */
    private AtomicInteger currentRunning = new AtomicInteger(0);

    /**
     * 预估总页数
     */
    private AtomicLong totalPageEstimate = new AtomicLong(0);

    /**
     * 已完成总页数
     */
    private AtomicLong totalPageFinished = new AtomicLong(0);

    /**
     * 任务是否已停止
     */
    private volatile boolean stopped = false;
}
