package com.ring.cloud.facade.support;

import com.ring.cloud.facade.crawl.ProxyUtil;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class ProxyApiClient {
    @Autowired
    protected RestTemplate restTemplate;

    @Value("${ml.client.proxy.url:null}")
    private String proxyUrl;

    /**
     * 重试获取多个代理
     */
    public List<ProxyIp> proxyIpListNoRetry() {
        return doGetProxyIpList();
    }

    private List<ProxyIp> doGetProxyIpList() {
        String proxyStr = restTemplate.getForObject(proxyUrl, String.class);
        if (StringUtils.isEmpty(proxyStr) || !proxyStr.contains(":")) {
            throw new IllegalArgumentException("代理IP格式不正确：" + proxyStr);
        }
        return Stream.of(proxyStr.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ipPort -> {
                    String[] arr = ipPort.split(":");
                    ProxyIp proxyIp = new ProxyIp();
                    proxyIp.setIp(arr[0]);
                    proxyIp.setPort(Integer.parseInt(arr[1]));
                    proxyIp.setCreateTime(System.currentTimeMillis());
                    return proxyIp;
                })
                .collect(Collectors.toList());
    }

    private ProxyIp doGetProxyIp() {
        String ipStr = restTemplate.getForObject(proxyUrl, String.class);
        if (StringUtils.isEmpty(ipStr) || !ipStr.contains(":")) {
            throw new IllegalArgumentException("代理IP格式不正确：" + ipStr);
        }
        return ProxyUtil.convertIp(ipStr);
    }
}
