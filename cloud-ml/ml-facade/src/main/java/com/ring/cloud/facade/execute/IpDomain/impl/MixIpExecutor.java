package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.core.pojo.*;
import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.facade.entity.ip.*;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    //根据ip查
    public MixIpRes extByIp(String currentIp, ProxyIp proxy, IpBreakpoint breakpoint) {
        MixIpRes mixIpRes = new MixIpRes();
        boolean hasMore = false;

        String tempXml = ipFirstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl));
        IpReadInfo tempInfo = parseTokenAndLoc(tempXml);
        String token = tempInfo.getToken();
        String loc = tempInfo.getLoc();

        // 首页
        if (breakpoint.getCurrentPage() == 1) {
            List<MixIpDomain> ipDomains = parseHtmlToObj(tempXml, currentIp, loc);
            try {
                mixIpRes.getMixIpDomainList().addAll(ipDomains);
                if (ipDomains.size() < 100) {
                    mixIpRes.setSuccess(true);
                    return mixIpRes;
                }
                breakpoint.setCurrentPage(2);
                hasMore = true;
            } finally {
                tempXml = null;
            }
        } else {
            hasMore = true;
        }
        // 限制最多翻 3 页
        int maxPage = 3;

        while (hasMore && breakpoint.getCurrentPage() <= maxPage) {
            int page = breakpoint.getCurrentPage();
            List<MixIpDomain> ipDomains = parsePageToObj(currentIp, loc, page, token, proxy);
            mixIpRes.getMixIpDomainList().addAll(ipDomains);
            breakpoint.setCurrentPage(page + 1);
            if (ipDomains.size() < 100) {
                hasMore = false;
            }
        }
        mixIpRes.setSuccess(true);
        return mixIpRes;
    }
    //根据domain查
    public MixIpRes extByDomain(String domain, ProxyIp proxy) {
        MixIpRes mixIpRes = new MixIpRes();
        String xmlContent = okProxyBase.doProxyRequest(proxy.getIp(), proxy.getPort(), IpUtil.buildIpUrlFirst(domain, ipDomainUrl), "");
        if (!IpUtil.validStrContains(xmlContent, "script", "J_ip_history"))
            throw new IllegalArgumentException("html 数据异常");
        List<MixDomainIp> domainIps = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(xmlContent);
            Elements historyElements = doc.select("#J_ip_history p");

            int count = 0;
            final int MAX = 5;

            for (Element p : historyElements) {
                if (count >= MAX) {
                    break; // 超过5条直接停止，不解析了
                }

                // 提取IP
                Element ipA = p.selectFirst("a[href^=/]");
                if (ipA == null) continue;

                String ip = ipA.text().trim();
                String dateText = p.select(".date").text().trim();

                // 拆分时间
                String[] times = dateText.split("-----");
                String start = times[0].trim();
                String end = (times.length > 1) ? times[1].trim() : start;

                domainIps.add(toMixDomainIp(ip, domain, "历史", start, end));
                count++;
            }
        } finally {
            xmlContent = null;
        }
        mixIpRes.setMixDomainIpList(domainIps);
        mixIpRes.setSuccess(true);
        return mixIpRes;
    }
    // ============== HTML解析 ==============
    public List<MixIpDomain> parseHtmlToObj(String xmlContent, String currentIp, String loc) {
        Document doc = Jsoup.parse(xmlContent);
        Elements res = doc.getElementsByClass("result result2");
        Elements uls = res.select("ul");

        List<MixIpDomain> ipDomains = new ArrayList<>();
        for (Element ul : uls) {
            Elements lis = ul.select("li");
            for (Element li : lis) {
                Element dateE = li.getElementsByClass("date").first();
                if (dateE == null) continue;
                String dateStr = dateE.text();
                if (!dateStr.contains("-----")) continue;
                String[] dates = dateStr.split("-----");
                Element linkElement = li.select("a").first();
                if (linkElement == null) continue;
                ipDomains.add(toMixIpDomain(currentIp, linkElement.text(), loc, dates[0], dates[1]));
            }
        }
        doc = null;
        return ipDomains;
    }
    // 单页解析
    public List<MixIpDomain> parsePageToObj(String currentIp, String loc, int page, String token, ProxyIp proxy) {
        String url = IpUtil.buildIpUrlPage(page, currentIp, ipDomainUrl, ipPageInfix, token);
        IpPageResponse pageResp = ipNextPage(proxy, url, token);
        List<MixIpDomain> ipDomains = new ArrayList<>();
        if (CollectionUtils.isEmpty(pageResp.getData())) {
            return ipDomains;
        }
        for (IpPageData data : pageResp.getData()) {
            ipDomains.add(toMixIpDomain(currentIp, data.getDomain(), loc, data.getAddtime(), data.getUptime()));
        }
        pageResp = null;
        return ipDomains;
    }

    public MixIpDomain toMixIpDomain(String currentIp, String domain, String loc, String adtime, String uptime) {
        MixIpDomain ipDomain = new MixIpDomain();
        ipDomain.setIp(currentIp);
        ipDomain.setDomain(domain);
        ipDomain.setLoc(loc);
        ipDomain.setAdtime(DateUtil.parseDate(adtime));
        ipDomain.setUptime(DateUtil.parseDate(uptime));
        return ipDomain;
    }

    public MixDomainIp toMixDomainIp(String currentIp, String domain, String loc, String adtime, String uptime) {
        MixDomainIp domainIp = new MixDomainIp();
        domainIp.setIp(currentIp);
        domainIp.setDomain(domain);
        domainIp.setLoc(loc);
        domainIp.setAdtime(DateUtil.parseDate(adtime));
        domainIp.setUptime(DateUtil.parseDate(uptime));
        return domainIp;
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