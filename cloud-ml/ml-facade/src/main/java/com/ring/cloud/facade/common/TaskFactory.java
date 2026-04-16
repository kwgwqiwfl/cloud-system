package com.ring.cloud.facade.common;

import com.ring.cloud.facade.process.metrics.ITask;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskFactory implements InitializingBean, ApplicationContextAware {

    private final Map<String, ITask<?>> taskMap = new HashMap<>();
    private ApplicationContext applicationContext;

    public <T> ITask<T> getTask(String type) {
        ITask<?> task = taskMap.get(type);
        if (task == null) {
            throw new IllegalArgumentException("无此任务类型: " + type);
        }
        return (ITask<T>) task;
    }

    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(ITask.class)
                .values()
                .forEach(task -> taskMap.put(task.taskEnum().name(), task));
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}