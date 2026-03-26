package com.ring.cloud.facade.process.ip;

public interface StopCondition {
    /**
     * 判断是否满足终止条件
     * @param currentIp 当前处理的IP
     * @return true=终止，false=继续
     */
    boolean shouldStop(String currentIp, String endIp);
}
