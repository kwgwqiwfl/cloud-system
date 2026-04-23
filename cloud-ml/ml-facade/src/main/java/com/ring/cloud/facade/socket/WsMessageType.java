package com.ring.cloud.facade.socket;

public enum WsMessageType {
    LOG,    // 日志
    ERROR,  // 错误
    TASK,   // 任务进度
    NORMAL_TASK,   // 正常任务进度
    MIX_TASK,   // 最新查询任务进度
//    ML_DOMAIN_AI_TASK,   // 最新ai域名任务进度
    SPECIFY_TASK,   // 指定ip任务进度
    DOMAIN_TASK,   // 域名任务进度
    NOTIFY  // 通知
}