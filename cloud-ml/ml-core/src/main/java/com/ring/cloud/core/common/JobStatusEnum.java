package com.ring.cloud.core.common;

import java.util.Arrays;
import java.util.List;

public enum JobStatusEnum {
    RUNNING,
    FINISHED,
    CREATED,
    UNKNOWN,
    CANCELED,
    FAILED,
    CLEANED,
    CANCELLING,
    INCOMPLETE;

    public static final List<String> runningStatus = Arrays.asList(RUNNING.name(), CREATED.name(), UNKNOWN.name(), CANCELLING.name());
    public static final List<String> scheduleRunningStatus = Arrays.asList(RUNNING.name(), UNKNOWN.name(), CANCELLING.name());
    public static final List<String> noDeleteStatus = Arrays.asList(RUNNING.name(), CANCELLING.name());
    public static final List<String> finishStatus = Arrays.asList(FINISHED.name(), FAILED.name(), CANCELED.name());

}
