package com.ring.cloud.facade.support;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyPang;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SubDomainSupport {
    @Autowired
    protected OkProxyPang okProxyPang;

    @Value("${ml.client.domain.subdomain:null}")
    protected String subdomainUrl;

    public List<String> subdomainsNoRetry(String domain, ProxyIp proxy) {
//        String url = IpUtil.buildSubdomainUrlFirst(domain, subdomainUrl);
//        String xmlContent = okProxyPang.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
//        return IpUtil.parseSubDomains(xmlContent);
        return null;
    }

}
