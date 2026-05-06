package com.ring.cloud.facade.socket;

public enum WsMessageType {
    LOG,    // 日志
    ERROR,  // 错误
    TASK,   // 任务进度
    NORMAL_TASK,   // 正常任务进度
    LOOP_TASK,   // 循环任务进度
    SCHEDULE_TASK,   // 定时任务进度
//    ML_DOMAIN_AI_TASK,   // 最新ai域名任务进度
    DOMAIN_TASK,   // 域名任务进度
    KEYWORD_TASK,   // 关键词任务进度
    NOTIFY  // 通知
}