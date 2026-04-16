package com.ring.cloud.facade.config;

import com.ring.cloud.facade.entity.ip.GlobalProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GlobalProgressManager {

    private final ConcurrentHashMap<String, GlobalProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * 初始化大IP任务进度（在【启动入口】调用）
     */
    public void initTask(String key, int totalSegments, long totalPageEstimate) {
        GlobalProgress progress = new GlobalProgress();
        progress.setTaskKey(key);
        progress.getTotalSegments().set(totalSegments);
        progress.getTotalPageEstimate().set(totalPageEstimate);
        progressMap.put(key, progress);
    }

    /**
     * 分段任务开始
     */
    public void onSegmentStart(String key) {
        GlobalProgress progress = progressMap.get(key);
        if (progress != null) {
            progress.getCurrentRunning().incrementAndGet();
        }
    }

    /**
     * 分段任务结束（成功/失败/停止都要调）
     */
    public void onSegmentFinish(String key, int finishedPages) {
        GlobalProgress progress = progressMap.get(key);
        if (progress == null) return;

        progress.getFinishedSegments().incrementAndGet();
        progress.getCurrentRunning().decrementAndGet();
        progress.getTotalPageFinished().addAndGet(finishedPages);
    }

    /**
     * 获取进度
     */
    public GlobalProgress getProgress(String key) {
        return progressMap.get(key);
    }

    /**
     * 停止任务
     */
    public void stopTask(String key) {
        GlobalProgress progress = progressMap.get(key);
        if (progress != null) {
            progress.setStopped(true);
        }
    }
}