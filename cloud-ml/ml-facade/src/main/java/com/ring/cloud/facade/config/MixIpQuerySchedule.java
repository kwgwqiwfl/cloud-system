package com.ring.cloud.facade.config;

import com.ring.cloud.facade.service.MixIpQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ml.ip.new.query", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MixIpQuerySchedule {

    @Resource
    private MixIpQueryService mixIpQueryService;

    // 配置单位：秒 → 自动转毫秒
    @Scheduled(fixedDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(${ml.ip.new.query.delay:30})}")
    public void execute() {
        try {
            mixIpQueryService.autoUpdateIpStatistics();
        } catch (Exception e) {
            log.error("定时任务执行失败：", e);
        }
    }
}