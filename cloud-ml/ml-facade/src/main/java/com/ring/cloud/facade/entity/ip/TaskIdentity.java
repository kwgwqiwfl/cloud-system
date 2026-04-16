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

    // ===== 统一判断：是否需要【全部线程跑完才释放锁】=====
    public boolean isNeedFinishAllRelease() {
        return largeIpTask || domainBatchTask;
    }
}