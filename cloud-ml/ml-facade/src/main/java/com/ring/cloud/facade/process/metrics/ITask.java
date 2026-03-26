package com.ring.cloud.facade.process.metrics;

import com.ring.cloud.facade.common.TaskTypeEnum;

public interface ITask<T> {

    default boolean runTask(T t) {
        return false;
    }

    default TaskTypeEnum taskEnum(){
        return null;
    }
    
}
