package com.ring.cloud.facade.config;

import com.ring.cloud.facade.entity.ip.PangRequest;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.RestProxyBase;
import com.ring.cloud.facade.frame.RetryTemplate;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
public class PangIpServcie {
    @Autowired
    private RetryTemplate retryTemplate;
    @Autowired
    protected RestProxyBase restProxyBase;
    @Autowired
    private ProxyPoolManager proxyPoolManager;

    @Value("${ml.client.desc.ip.pangzhan:null}")
    protected String ipPangUrl;
    @Value("${ml.client.pang.maxRetry:10}")
    private int maxRetry;
    @Value("${ml.client.pang.intervalSec:4}")
    private int intervalSec;

    public List<String> pangIpsWithRetry(String currentIp, ProxyIp proxy) {
        PangRequest request = new PangRequest(currentIp, proxy);
        return retryTemplate.execute(
                maxRetry,
                intervalSec,
                this::crawlPang,  // 方法引用：接收 PangRequest，返回 List<String>
                request,
                "查询pang"
        );
    }

    public List<String> crawlPang(PangRequest request) {
        ProxyIp proxy = request.getProxy();
        String url = IpUtil.buildPangUrl(request.getCurrentIp(), ipPangUrl);
        RestTemplate restTemplate = restProxyBase.createWebClient(proxy.getIp(), proxy.getPort());
        String xmlContent = restProxyBase.doProxyRequest(restTemplate, url, "");
        return IpUtil.parsePangValidIps(xmlContent);
    }

    public List<String> pangIpsNoRetry(String currentIp, ProxyIp proxy) {
        String url = IpUtil.buildPangUrl(currentIp, ipPangUrl);
        RestTemplate restTemplate = restProxyBase.createWebClient(proxy.getIp(), proxy.getPort());
        String xmlContent = restProxyBase.doProxyRequest(restTemplate, url, "");
        return IpUtil.parsePangValidIps(xmlContent);
    }

}
