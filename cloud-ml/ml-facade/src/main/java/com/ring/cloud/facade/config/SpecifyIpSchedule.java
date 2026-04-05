package com.ring.cloud.facade.config;

import com.ring.cloud.facade.service.MixIpQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "ml.ip.specify.query",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SpecifyIpSchedule {

    @Resource
    private MixIpQueryService mixIpQueryService;

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);

    // 线程安全Set，天然去重
    private static final Set<String> IP_SET = ConcurrentHashMap.newKeySet();

    static {
        // 初始化固定IP（已去重）
        IP_SET.addAll(Arrays.asList(
                "221.228.32.13",
                "127.0.1.1",
                "127.0.0.2",
                "127.0.0.1",
                "0.0.0.0",
                "182.43.124.6",
                "182.43.124.7",
                "124.236.16.201",
                "183.192.65.101",
                "61.160.148.90",
                "106.74.25.198",
                "111.32.136.194",
                "183.213.92.2",
                "120.204.204.201",
                "183.252.183.9",
                "111.31.192.110",
                "183.252.183.98"
        ));
    }

    public static void addIp(String ip) {
        IP_SET.add(ip.trim());
        log.info("动态添加IP成功：{}，当前IP总数：{}", ip, IP_SET.size());
    }

    /**
     * 🔥 获取当前所有IP（不可变List）
     */
    public static List<String> getIpList() {
        return Collections.unmodifiableList(new ArrayList<>(IP_SET));
    }

    // ====================== 定时任务 ======================
    @Scheduled(cron = "${ml.ip.specify.query.cron:0 0 0,8,16 * * ?}")
    public void execute() {
        long start = System.currentTimeMillis();
        // 获取当前全量IP（包含动态添加的）
        List<String> ipList = getIpList();
        try {
            log.info("specify ip 定时任务开始，待查询IP数量：{}", ipList.size());
            // 传入IP列表执行
            String costs = mixIpQueryService.specifyIpQuery(ipList);
            long success = successCount.incrementAndGet();
            log.info("specify ip 定时任务成功 → 耗时：{} ms  成功次数：{}", costs, success);
        } catch (Throwable e) {
            long fail = failCount.incrementAndGet();
            log.error("specify ip 定时任务失败 → 耗时：{} ms  失败次数：{} 信息：{}",
                    (System.currentTimeMillis() - start), fail, e.getMessage());
        }
    }
}