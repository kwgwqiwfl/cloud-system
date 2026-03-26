package com.ring.cloud.facade.common;

import com.ring.cloud.facade.process.metrics.ITaskManage;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class TaskFactory implements InitializingBean, ApplicationContextAware {
    private static final Map<String, Supplier<ITaskManage>> taskMap = new HashMap<>();
    private ApplicationContext applicationContext;

    public ITaskManage taskManage(String type) {
        Supplier<ITaskManage> p = taskMap.get(type);
        if (p != null) {
            return p.get();
        }
        throw new IllegalArgumentException("No such ITaskManage by type:" + type);
    }


    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(ITaskManage.class)
                .values()
                .forEach(c -> taskMap.put(c.taskEnum().name(), () -> c));
    }
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
