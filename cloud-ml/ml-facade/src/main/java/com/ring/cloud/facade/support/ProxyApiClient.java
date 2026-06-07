package com.ring.cloud.facade.support;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkHttpSimpleClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class ProxyApiClient {
//    @Autowired
//    protected RestTemplate restTemplate;
    @Autowired
    protected OkHttpSimpleClient okHttpSimpleClient;

    @Value("${ml.client.proxy.url:null}")
    private String proxyUrl;

    /**
     * 重试获取多个代理
     */
    public List<ProxyIp> proxyIpListNoRetry() {
        return doGetProxyIpList();
    }

    private List<ProxyIp> doGetProxyIpList() {
//        String proxyStr = restTemplate.getForObject(proxyUrl, String.class);

        String content = okHttpSimpleClient.doGetRequest(proxyUrl, "");
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("返回内容为空");

        if (!content.contains(":")) {
            throw new IllegalArgumentException("返回代理IP格式不正确：" + content);
        }
        //解析ip 字符串形式
        return Stream.of(content.split("\\n"))
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

}
