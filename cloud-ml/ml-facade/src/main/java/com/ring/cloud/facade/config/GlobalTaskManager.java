package com.ring.cloud.facade.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalTaskManager {
    public static final ConcurrentHashMap<String, AtomicBoolean> TASK_STOP_MAP = new ConcurrentHashMap<>();
    // 改为 String 类型
    private static final Set<String> RUNNING_SEGMENTS = ConcurrentHashMap.newKeySet();

    // 判断是否正在运行
    public static boolean isSegmentRunning(String segmentNo) {
        return RUNNING_SEGMENTS.contains(segmentNo);
    }

    // 占用段（加锁）
    public static boolean occupySegment(String segmentNo) {
        return RUNNING_SEGMENTS.add(segmentNo);
    }

    // 释放段（解锁）
    public static void releaseSegment(String segmentNo) {
        RUNNING_SEGMENTS.remove(segmentNo);
    }

    // 所有正在运行的 segNo（现在返回 String 集合）
    public static Set<String> getRunningSegments() {
        return RUNNING_SEGMENTS;
    }
}
