package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.core.pojo.MlDomain;
import com.ring.cloud.core.pojo.MlIcp;
import com.ring.cloud.core.pojo.MlIp;
import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.cloud.facade.entity.ip.MixIpInfo;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MixIpExecutor extends IpBaseExecutor {

    @Value("${ml.client.desc.ip.domain:}")
    protected String mixUrl;

    public MixIpInfo queryMixInfo(ProxyIp proxy) {
        String content = null;
        try {
            content = okProxyBase.doProxyRequest(proxy.getIp(), proxy.getPort(), mixUrl, "");
            if (content == null || content.trim().isEmpty())
                throw new IllegalArgumentException("返回内容为空");

            Document doc = Jsoup.parse(content);
            List<String> domainStrList  = extractList(doc, "最新域名查询");
            List<String> ipStrList      = extractList(doc, "最新iP查询");
            List<String> icpStrList     = extractList(doc, "最新备案查询");
            List<String> subStrList     = extractList(doc, "最新子域名查询");

            // 一行构造对象列表（最干净）
            List<MlDomain> domainList   = domainStrList.stream().map(MlDomain::new).collect(Collectors.toList());
            List<MlIp> ipList           = ipStrList.stream().map(MlIp::new).collect(Collectors.toList());
            List<MlIcp> icpList         = icpStrList.stream().map(MlIcp::new).collect(Collectors.toList());
            List<MlSubdomain> subList   = subStrList.stream().map(MlSubdomain::new).collect(Collectors.toList());

            MixIpInfo info = new MixIpInfo();
            info.setDomainList(domainList);
            info.setIpList(ipList);
            info.setIcpList(icpList);
            info.setSubdomainList(subList);
            return info;
        }finally {
            content = null;
        }
    }

    // 提取不到直接抛异常，一定会重试
    private List<String> extractList(Document doc, String title) {
        for (Element ul : doc.select("ul")) {
            Element span = ul.select("li.title span").first();
            if (span != null && span.text().contains(title)) {
                List<String> list = new ArrayList<>();
                for (Element a : ul.select("li:not(.title) a")) {
                    list.add(a.text().trim());
                }
                if (list.isEmpty()) throw new RuntimeException("列表为空: " + title);
                return list;
            }
        }
        throw new RuntimeException("提取失败: " + title);
    }
}