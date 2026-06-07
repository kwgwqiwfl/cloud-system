package com.ring.cloud.facade.frame;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class OkProxyBase {

    // ======================
    // 全局公用 ！！！
    // ======================
    protected static final ThreadLocal<Proxy> PROXY_THREAD_LOCAL = new ThreadLocal<>();

    protected static class DynamicProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            Proxy proxy = PROXY_THREAD_LOCAL.get();
            return proxy != null ? Arrays.asList(proxy) : Arrays.asList(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }

}
