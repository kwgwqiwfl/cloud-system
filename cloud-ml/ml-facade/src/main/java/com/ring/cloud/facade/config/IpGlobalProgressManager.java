package com.ring.cloud.facade.config;

import com.ring.cloud.facade.entity.ip.IpGlobalProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class IpGlobalProgressManager {

    private final ConcurrentHashMap<String, IpGlobalProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * 初始化大IP任务进度（在【启动入口】调用）
     */
    public void initTask(String ip, int totalSegments, long totalPageEstimate) {
        IpGlobalProgress progress = new IpGlobalProgress();
        progress.setIp(ip);
        progress.getTotalSegments().set(totalSegments);
        progress.getTotalPageEstimate().set(totalPageEstimate);
        progressMap.put(ip, progress);
    }

    /**
     * 分段任务开始
     */
    public void onSegmentStart(String ip) {
        IpGlobalProgress progress = progressMap.get(ip);
        if (progress != null) {
            progress.getCurrentRunning().incrementAndGet();
        }
    }

    /**
     * 分段任务结束（成功/失败/停止都要调）
     */
    public void onSegmentFinish(String ip, int finishedPages) {
        IpGlobalProgress progress = progressMap.get(ip);
        if (progress == null) return;

        progress.getFinishedSegments().incrementAndGet();
        progress.getCurrentRunning().decrementAndGet();
        progress.getTotalPageFinished().addAndGet(finishedPages);
    }

    /**
     * 获取进度
     */
    public IpGlobalProgress getProgress(String ip) {
        return progressMap.get(ip);
    }

    /**
     * 停止任务
     */
    public void stopTask(String ip) {
        IpGlobalProgress progress = progressMap.get(ip);
        if (progress != null) {
            progress.setStopped(true);
        }
    }
}