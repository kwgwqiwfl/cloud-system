package com.ring.cloud.facade.proxy;

import com.ring.cloud.facade.support.ProxyApiClient;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ProxyPoolManager {
    @Resource
    private ProxyApiClient proxyApiClient;

    // 直接存 ProxyIp，不搞任何包装类
    private final Queue<ProxyIp> queue = new ConcurrentLinkedQueue<>();

    // 代理最大有效使用时间：35秒（比你30-60s短，留安全余量）
    private static final long MAX_PROXY_AGE = 35 * 1000;

    public ProxyIp takeProxy() {
        while (true) {
            // 先清掉队列里已经过期的代理
            clearExpiredProxies();
            if (queue.isEmpty()) {
                List<ProxyIp> proxyList = null;

                // 1. 调用一次，捕获所有异常
                try {
                    proxyList = proxyApiClient.proxyIpListNoRetry();
                } catch (Throwable e) {
                    // 异常捕获，不抛出
                    proxyList = null;
                }

                // 2. 拉不到 → 等1秒再循环
                if (proxyList == null || proxyList.isEmpty()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }

                // 3. 加入队列
                queue.addAll(proxyList);
            }

            // 4. 拿一个代理返回（只出不进）
            return queue.poll();
        }
    }

    /**
     * 清理队列中过期的代理
     */
    private void clearExpiredProxies() {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            ProxyIp head = queue.peek();
            if (head.getCreateTime() + MAX_PROXY_AGE < now) {
                queue.poll();
            } else {
                break;
            }
        }
    }
}
