package com.ring.cloud.facade.proxy;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalProxyHelper {
    @Autowired
    private ProxyPoolManager proxyPoolManager;

    /**
     * 统一获取代理
     */
    public ProxyIp getAvailableProxy() {
        ProxyIp proxy = proxyPoolManager.takeProxy();
        while (proxy == null) {
            log.debug("代理获取失败，50毫秒后重试");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            proxy = proxyPoolManager.takeProxy();
        }
        return proxy;
    }

    /**
     * 判断是否需要切换代理（完全复制你原有逻辑）
     */
    public boolean needSwitchProxy(Throwable e) {
        if (e == null) {
            return true;
        }
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();

        // 代理异常 → 切换
        if (msg.contains("connect timed out")
                || msg.contains("connection refused")
                || msg.contains("connection reset")
                || msg.contains("failed to connect")
                || msg.contains("no route to host")
                || msg.contains("socket closed")
                || msg.contains("unable to resolve host")) {
            return true;
        }

        // 目标波动 → 不切换
        if (msg.contains("read timed out")
                || msg.contains("socket timeout")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("504")) {
            return false;
        }

        return true;
    }
}
