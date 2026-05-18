package com.ring.cloud.facade.config;

import com.ring.cloud.facade.service.MixIpQueryService;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ml.domain.query.ai.new", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MlDomainAiQuerySchedule {

    @Resource
    private MixIpQueryService mixIpQueryService;
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);

    /**
     * 正确：执行完后 间隔 8 秒再执行下一次
     * 不重叠、不并发、最安全
     */
    @Scheduled(
            initialDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(10)}",
            fixedDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(${ml.domain.query.ai.new.delay:8})}"
    )
    public void execute() {
        long start = System.currentTimeMillis();
        try {
            log.info("AI任务开始");
            WsUtil.push(WsMessageType.SCHEDULE_TASK, "AI开始");
            String costs = mixIpQueryService.mlDomainAi();
            long success = successCount.incrementAndGet();
            log.info("AI完成 → 耗时：{} ms  成功次数：{}", costs, success);
            WsUtil.push(WsMessageType.SCHEDULE_TASK, "AI完成。耗时："+costs+"ms  次数："+success);
        } catch (Throwable e) {
            long fail = failCount.incrementAndGet();
            long cost = System.currentTimeMillis() - start;
            log.error("AI失败 → 耗时：{} ms  失败次数：{}  错误信息：{}", cost, fail, e.getMessage());
            WsUtil.push(WsMessageType.SCHEDULE_TASK, "AI失败!! 信息："+e.getMessage()+"  次数："+fail);
        }
    }
}