package com.ring.cloud.facade.config;

import com.ring.cloud.facade.entity.ip.GlobalProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GlobalProgressManager {

    private final ConcurrentHashMap<String, GlobalProgress> progressMap = new ConcurrentHashMap<>();

    public void initTask(String key, int totalSegments, long totalPageEstimate) {
        GlobalProgress progress = new GlobalProgress();
        progress.setTaskKey(key);
        progress.getTotalSegments().set(totalSegments);
        progress.getTotalPageEstimate().set(totalPageEstimate);
        progressMap.put(key, progress);
    }

    public void onSegmentStart(String key) {
        GlobalProgress progress = progressMap.get(key);
        if (progress != null) {
            progress.getCurrentRunning().incrementAndGet();
        }
    }

    public void onSegmentFinish(String key, int finishedPages) {
        GlobalProgress progress = progressMap.get(key);
        if (progress == null) return;

        // 标准 CAS 线程安全 +1，绝不会超量
        int current;
        int total = progress.getTotalSegments().get();
        do {
            current = progress.getFinishedSegments().get();
            if (current >= total) {
                return;
            }
        } while (!progress.getFinishedSegments().compareAndSet(current, current + 1));

        progress.getCurrentRunning().decrementAndGet();
        progress.getTotalPageFinished().addAndGet(finishedPages);
    }

    public GlobalProgress getProgress(String key) {
        return progressMap.get(key);
    }

    public void stopTask(String key) {
        GlobalProgress progress = progressMap.get(key);
        if (progress != null) {
            progress.setStopped(true);
        }
    }
}