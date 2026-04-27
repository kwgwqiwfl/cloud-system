package com.ring.cloud.facade.entity.ip;

import lombok.Data;

@Data
public class TaskIdentity {
    private String taskType;
    private String uniqueKey;
    private String lockKey;

    // 任务类型标记
    private boolean largeIpTask;
    private boolean smallIpTask;
    private boolean domainBatchTask;
    private boolean keywordBatchTask;  // 【只加这一行】

    // ===== 统一判断：是否需要【全部线程跑完才释放锁】=====
    public boolean isNeedFinishAllRelease() {
        // 【只加 keywordBatchTask】其他完全不动
        return largeIpTask || domainBatchTask || keywordBatchTask;
    }
}