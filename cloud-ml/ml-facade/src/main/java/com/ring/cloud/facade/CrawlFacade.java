package com.ring.cloud.facade;

import com.ring.welkin.common.utils.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableScheduling
@ComponentScan(basePackages = {"com.ring.cloud", "com.ring.welkin","com.ring.welkin.**.pojo","com.ring.welkin.**.entity"})
@EntityScan(basePackages = {"com.ring.cloud.**.pojo","com.ring.cloud.**.entity"})
@MapperScan(basePackages = {"com.ring.cloud.core.**.mapper", "com.ring.welkin.data.**.mapper"})
@PropertySource(value = {"classpath:common.properties", "classpath:crawl.properties"}, ignoreResourceNotFound = true)
public class CrawlFacade {
    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }

    public static void main(String[] args) {
        String pid = SystemUtil.getPid();
        MDC.put("pid", pid);
        // SystemUtil.storePid(pidDir, pidFile, pid);
        log.info("JAVA CLASS PATH = " + System.getProperty("java.class.path"));
        SpringApplication.run(CrawlFacade.class, args);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("schedule-");
        scheduler.initialize();
        return scheduler;
    }

}
