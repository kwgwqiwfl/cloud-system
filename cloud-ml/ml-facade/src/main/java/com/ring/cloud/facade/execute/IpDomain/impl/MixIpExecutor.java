package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.core.pojo.*;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.MixIpInfo;
import com.ring.cloud.facade.entity.ip.MixIpRes;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MixIpExecutor extends IpBaseExecutor {

    @Value("${ml.client.desc.ip.domain:}")
    protected String mixUrl;
    @Value("${ml.client.desc.ai.domain:}")
    protected String aiUrl;

    // ai domain 最新查询
    public List<MlDomainAi> queryDoaminAi() {
        ProxyIp proxy = globalProxyHelper.getAvailableProxy();
        String content = null;
        try {
            content = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), aiUrl, "");
            if (content == null || content.trim().isEmpty())
                throw new IllegalArgumentException("返回内容为空");

            Document doc = Jsoup.parse(content);
            List<String> domainList  = extractAiList(doc, "最近查询");
            return domainList.stream().map(MlDomainAi::new).collect(Collectors.toList());
        }finally {
            content = null;
        }
    }

    // ip 最新查询 4项
    public MixIpInfo queryMixInfo() {
        ProxyIp proxy = globalProxyHelper.getAvailableProxy();
        String content = null;
        try {
            content = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), mixUrl, "");
            if (content == null || content.trim().isEmpty())
                throw new IllegalArgumentException("返回内容为空");

            Document doc = Jsoup.parse(content);
            List<String> domainStrList  = extractIpList(doc, "最新域名查询");
            List<String> ipStrList      = extractIpList(doc, "最新iP查询");
            List<String> icpStrList     = extractIpList(doc, "最新备案查询");
            List<String> subStrList     = extractIpList(doc, "最新子域名查询");

            // 一次流完成：过滤 + 转换 → 最高效率
            List<MlDomain> domainList   = domainStrList.stream().filter(s -> s.length() <= 191).map(MlDomain::new).collect(Collectors.toList());
            List<MlIp> ipList           = ipStrList.stream().map(MlIp::new).collect(Collectors.toList());
            List<MlIcp> icpList         = icpStrList.stream().filter(s -> s.length() <= 191).map(MlIcp::new).collect(Collectors.toList());
            List<MlSubdomain> subList   = subStrList.stream().filter(s -> s.length() <= 191).map(MlSubdomain::new).collect(Collectors.toList());

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

    // 业务方法
    public MixIpRes extByIp(String currentIp, ProxyIp proxy, IpBreakpoint breakpoint, MixIpRes mixIpRes) throws IOException {
        executeIpCrawl(currentIp, proxy, null, breakpoint, 3);
        @SuppressWarnings("unchecked")
        List<MixIpDomain> resultList = (List<MixIpDomain>) (List<?>) breakpoint.getList();
        mixIpRes.getMixIpDomainList().addAll(resultList);
        mixIpRes.setSuccess(true);
        return mixIpRes;
    }
    @Override
    protected Object buildHomeItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new MixIpDomain(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected Object buildPageItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new MixIpDomain(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected void processPageData(List<?> list, BufferedWriter bw, IpBreakpoint breakpoint) {
        if (list == null || list.isEmpty()) return;
        breakpoint.getList().addAll(list);
    }
    // domain查询ip
    public MixIpRes extByDomain(String domain, ProxyIp proxy, IpBreakpoint breakpoint, MixIpRes mixIpRes) {
        List<MixDomainIp> list = executeDomainCrawl(domain, proxy, breakpoint, 5, MixDomainIp::new);
        mixIpRes.getMixDomainIpList().addAll(list);
        mixIpRes.setSuccess(true);
        return mixIpRes;
    }

    // 解析 ip四项
    private List<String> extractIpList(Document doc, String title) {
        for (Element ul : doc.select("ul")) {
            Element span = ul.select("li.title span").first();
            if (span != null && span.text().contains(title)) {
                List<String> list = new ArrayList<>();
                for (Element a : ul.select("li:not(.title) a")) {
                    list.add(a.text().trim());
                }
                if (list.isEmpty()) throw new IllegalArgumentException("列表为空: " + title);
                return list;
            }
        }
        throw new IllegalArgumentException("提取失败: " + title);
    }
    //解析ai domain
    private List<String> extractAiList(Document doc, String title) {
        // 专门匹配爱站网的最近查询模块
        Element lastSearch = doc.select("div.last-search").first();
        if (lastSearch == null) {
            throw new IllegalArgumentException("提取失败，未找到最近查询模块: " + title);
        }

        List<String> list = new ArrayList<>();
        for (Element a : lastSearch.select("ul li a")) {
            list.add(a.text().trim());
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("最近查询列表为空: " + title);
        }
        return list;
    }

}