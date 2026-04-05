package com.ring.cloud.facade.config;

import com.ring.cloud.facade.service.MixIpQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ml.ip.new.query", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MixIpQuerySchedule {

    @Resource
    private MixIpQueryService mixIpQueryService;

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);

    // 配置单位：秒 → 自动转毫秒
    @Scheduled(
            initialDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(10)}",  // 首次延迟 10 秒再执行
            fixedDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(${ml.ip.new.query.delay:30})}"
    )
    public void execute() {
        long start = System.currentTimeMillis();
        try {
            log.info("mix ip开始");
            String costs = mixIpQueryService.mixIpStatistics();
            long success = successCount.incrementAndGet();
            log.info("mix ip成功 → 三步耗时：{} ms  成功次数：{}", costs, success);
        } catch (Throwable e) {
            long fail = failCount.incrementAndGet();
            log.error("mix ip失败 → 耗时：{} ms   失败次数：{} 信息：{}", (System.currentTimeMillis() - start), fail, e.getMessage());
        }
    }
}