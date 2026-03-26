package com.ring.cloud.facade.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalTaskManager {
    public static final ConcurrentHashMap<Integer, AtomicBoolean> TASK_STOP_MAP = new ConcurrentHashMap<>();
    private static final Set<Integer> RUNNING_SEGMENTS = ConcurrentHashMap.newKeySet();

    // 判断是否正在运行
    public static boolean isSegmentRunning(int segmentNo) {
        return RUNNING_SEGMENTS.contains(segmentNo);
    }

    // 占用段（加锁）
    public static boolean occupySegment(int segmentNo) {
        return RUNNING_SEGMENTS.add(segmentNo);
    }

    // 释放段（解锁）
    public static void releaseSegment(int segmentNo) {
        RUNNING_SEGMENTS.remove(segmentNo);
    }

    // 所有正在运行的 segNo
    public static Set<Integer> getRunningSegments() {
        return RUNNING_SEGMENTS; // 直接返回你正在运行的集合
    }
}
