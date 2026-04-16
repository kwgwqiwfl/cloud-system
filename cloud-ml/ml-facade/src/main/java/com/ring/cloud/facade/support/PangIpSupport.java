package com.ring.cloud.facade.support;

import com.ring.cloud.facade.entity.ip.PangRequest;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyPang;
import com.ring.cloud.facade.frame.RetryTemplate;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PangIpSupport {
    @Autowired
    private RetryTemplate retryTemplate;
    @Autowired
    protected OkProxyPang okProxyPang;

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
        String xmlContent = okProxyPang.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
        return IpUtil.parsePangValidIps(xmlContent);
    }

    public List<String> pangIpsNoRetry(String currentIp, ProxyIp proxy) {
        String url = IpUtil.buildPangUrl(currentIp, ipPangUrl);
        String xmlContent = okProxyPang.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
        return IpUtil.parsePangValidIps(xmlContent);
    }

}
