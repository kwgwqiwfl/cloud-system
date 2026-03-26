package com.ring.cloud.facade.common;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum TaskTypeEnum {
    FULL("全量"),
    UPDATE("更新");

    private final String taskType;

    TaskTypeEnum(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskType() {
        return taskType;
    }
}
