package com.ring.cloud.facade.execute.IpDomain;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.cloud.core.util.IpCoreUtils;
import com.ring.cloud.facade.entity.ip.*;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyIp;
import com.ring.cloud.facade.frame.OkProxyPang;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class IpBaseExecutor {

    // ========================= 父类通用成员变量 =========================
    @Autowired
    protected OkProxyPang okProxyPang;
    @Autowired
    protected OkProxyIp okProxyIp;
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;

    @Value("${ml.client.desc.ip.domain:null}")
    protected String ipBaseUrl;
    @Value("${ml.client.desc.ip.page.infix:null}")
    protected String ipPageInfix;
    @Value("${ml.client.desc.domain.page.infix:null}")
    protected String domainPageInfix;
    @Value("${ml.client.domain.subdomain:null}")
    protected String subdomainUrl;

    // ========================= 【通用ip查域名】 =========================
    protected IpReadInfo executeIpCrawl(String currentIp, ProxyIp proxy,
                                        BufferedWriter bw, IpBreakpoint breakpoint, int maxPage) throws IOException {

        String tempXml = firstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipBaseUrl));
        IpReadInfo info = parseTokenAndLoc(tempXml);
        boolean hasMore = false;

        // 首页处理（同原有逻辑）
        if (breakpoint.getCurrentPage() == 1) {
            List<?> homeList = parseHtmlPage(tempXml,
                    "class=\"result result2\"",
                    ".result.result2 li",
                    (linkE, times) -> buildHomeItem(currentIp, info.getLoc(), linkE.text(), times[0], times[1])
            );

            processPageData(homeList, bw, breakpoint);
            breakpoint.setCurrentCount(homeList.size());

            // 不足100条直接结束
            if (homeList.size() < 100) {
                info.setSuccess(true);
                tempXml = null;
                return info;
            }

            breakpoint.setCurrentPage(2);
            hasMore = true;
        } else {
            hasMore = true;
        }
        tempXml = null;

        // 分页循环（同原有逻辑）
        while (hasMore) {
            int page = breakpoint.getCurrentPage();

            // 最大页数限制
            if (maxPage > 0 && page > maxPage) {
                break;
            }

            String url = IpUtil.buildIpUrlPage(page, currentIp, ipBaseUrl, ipPageInfix, info.getToken());
            IpPageResponse resp = ipNextPage(proxy, url, info.getToken());

            List<?> pageList = parseApiPage(resp.getData(),
                    data -> buildPageItem(currentIp, info.getLoc(), data.getDomain(), data.getAddtime(), data.getUptime())
            );

            processPageData(pageList, bw, breakpoint);
            breakpoint.setCurrentCount(breakpoint.getCurrentCount() + pageList.size());

            // 不足100条结束
            if (pageList.size() < 100) {
                hasMore = false;
            }

            breakpoint.setCurrentPage(page + 1);
        }

        info.setSuccess(true);
        return info;
    }
    // ========================= 【通用域名反查】 =========================
    protected <T> List<T> executeDomainCrawl(String domain, ProxyIp proxy, IpBreakpoint breakpoint,
                                             int maxPage, DomainMapper<T> mapper) {
        List<T> resultList = new ArrayList<>();
        boolean hasMore = false;

        String firstPageXml = firstPage(proxy, IpUtil.buildIpUrlFirst(domain, ipBaseUrl));
        String token = extractToken(firstPageXml);
        if (token == null) {
            throw new IllegalArgumentException("token解析失败");
        }
        // 首页
        if (breakpoint.getCurrentPage() == 1) {
            if(firstPageXml.contains("禁止查询该域名")){
                return resultList;
            }
            List<T> homeList = parseHtmlPage(firstPageXml,
                    "id=\"J_ip_history\"",
                    "#J_ip_history p",
                    (linkE, times) -> mapper.map(linkE.text().trim(), domain, times[0], times[1])
            );
            resultList.addAll(homeList);

            if (homeList.size() < 100) {
                return resultList;
            }
            breakpoint.setCurrentPage(2);
            hasMore = true;
        } else {
            hasMore = true;
        }

        // 翻页
        while (hasMore && breakpoint.getCurrentPage() <= maxPage) {
            int page = breakpoint.getCurrentPage();
            String url = IpUtil.buildDomainUrlPage(page, domain, ipBaseUrl, domainPageInfix, token);
            DomainPageResponse resp = domainNextPage(proxy, url, token);

            List<T> pageList = parseApiPage(resp.getData(),
                    data -> mapper.map(IpCoreUtils.longToIp(data.getIp()), domain, data.getAddtime(), data.getUptime())
            );

            resultList.addAll(pageList);
            breakpoint.setCurrentPage(page + 1);
            if (pageList.size() < 100) {
                hasMore = false;
            }
        }
        return resultList;
    }

    // ========================= 【查询子域名】 =========================
    protected List<String> executeSubdomainCrawl(String domain, ProxyIp proxy, IpBreakpoint breakpoint,
                                             int maxPage) {
        List<String> resultList = new ArrayList<>();
        boolean hasMore = false;
        // 首页
        if (breakpoint.getCurrentPage() == 1) {
            String url = IpUtil.buildSubdomainUrlFirst(domain, subdomainUrl);
            String xmlContent = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
            if (xmlContent == null || !xmlContent.contains("子域名查询")) {
                log.debug("页面无效，未找到 子域名查询 标识，url:{}", url);
                throw new IllegalArgumentException("html 数据异常");
            }
            Set<String> domains = IpUtil.parseSubDomains(xmlContent);
            if(domains.size()==0)
                resultList.add(domain);
            else resultList.addAll(domains);
            if (domains.size() < 50) {
                return resultList;
            }
            breakpoint.setCurrentPage(2);
            hasMore = true;
        } else {
            hasMore = true;
        }
        // 翻页
        while (hasMore && breakpoint.getCurrentPage() <= maxPage) {
            int page = breakpoint.getCurrentPage();
            String url = IpUtil.buildSubdomainUrlPage(domain, subdomainUrl, page);
            String jsonContent = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
            if (StringUtils.isEmpty(jsonContent))
                throw new IllegalArgumentException("翻页 数据返回null");
            SubdomainPageResponse pageContent = JSON.parseObject(jsonContent, SubdomainPageResponse.class);
            if (!pageContent.isStatus()) {
                throw new IllegalArgumentException("subdomainNextPage 数据解析异常");
            }
            SubdomainPageData data = pageContent.getData();
            if(data==null || !data.getResult().isEmpty()) break;
            resultList.addAll(data.getResult());
            breakpoint.setCurrentPage(page + 1);
            if (data.getPageSize() < 50) {
                hasMore = false;
            }
        }
        return resultList;
    }

    // ========================= 父类核心公共方法（全部保留） =========================
    // 通用HTML解析（所有首页共用）
    protected <T> List<T> parseHtmlPage(String xml, String checkKey, String selector, HtmlMapper<T> mapper) {
        if (!xml.contains(checkKey)) {
            return new ArrayList<>();
        }
        Document doc = Jsoup.parse(xml);
        Elements elements = doc.select(selector);
        List<T> result = new ArrayList<>();

        for (Element el : elements) {
            Element dateE = el.select(".date").first();
            Element linkE = el.select("a").first();
            if (dateE == null || linkE == null) continue;

            String dateText = dateE.text();
            if (!dateText.contains("-----")) continue;

            String[] times = splitTime(dateText);
            T item = mapper.map(linkE, times);
            if (item != null) result.add(item);
        }
        return result;
    }

    // 通用API分页解析（所有分页共用）
    protected <D, T> List<T> parseApiPage(List<D> dataList, ApiMapper<D, T> mapper) {
        List<T> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(dataList)) return list;
        for (D data : dataList) {
            T item = mapper.map(data);
            if (item != null) list.add(item);
        }
        return list;
    }

    // 通用时间切割
    protected String[] splitTime(String dateText) {
        String[] times = dateText.split("-----");
        String start = times[0].trim();
        String end = (times.length > 1) ? times[1].trim() : start;
        return new String[]{start, end};
    }

    // 函数式接口
    protected interface HtmlMapper<T> {
        T map(Element linkE, String[] times);
    }

    protected interface ApiMapper<D, T> {
        T map(D data);
    }

    // ========================= 父类固定工具方法（全部保留） =========================
    protected String extractToken(String html) {
        String key = "var _TOKEN = '";
        int tokenIndex = html.indexOf(key);
        if (tokenIndex == -1) return null;
        int start = tokenIndex + key.length();
        int end = html.indexOf("'", start);
        return html.substring(start, end);
    }

    protected IpReadInfo parseTokenAndLoc(String xml) {
        IpReadInfo info = new IpReadInfo();
        info.setToken(extractToken(xml));
        int resultNode = xml.indexOf("class=\"result result2\"");
        if (resultNode != -1) {
            int h3Start = xml.indexOf("<h3>", resultNode);
            int h3End = xml.indexOf("</h3>", h3Start);
            info.setLoc(xml.substring(h3Start + 4, h3End).trim());
        }
        return info;
    }

    protected void writeIpDomainCsv(BufferedWriter bw, List<SourceIpDomain> list) throws IOException {
        StringBuilder batchSb = new StringBuilder();
        for (SourceIpDomain ipDomain : list) {
            appendCsvRow(batchSb,
                    ipDomain.getDomain(),
                    ipDomain.getIp(),
                    ipDomain.getLoc(),
                    ipDomain.getAdtimeStr(),
                    ipDomain.getUptimeStr()
            );
        }
        if (batchSb.length() > 0) {
            batchWrite(bw, batchSb);
        }
    }

    protected void appendCsvRow(StringBuilder batchSb, String domain, String ip, String loc, String addTime, String upTime) {
        batchSb.append(ip).append(",").append(loc).append(",").append(domain)
                .append(",").append(addTime).append(",").append(upTime).append('\n');
    }

    protected void batchWrite(BufferedWriter bw, StringBuilder batchSb) throws IOException {
        bw.write(batchSb.toString());
        bw.flush();
        batchSb.setLength(0);
    }

    protected String firstPage(ProxyIp proxy, String url) {
        String xmlContent = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
        if (xmlContent == null || !xmlContent.contains("var _TOKEN =")) {
            log.debug("页面无效，未找到 _TOKEN 标识，url:{}", url);
            throw new IllegalArgumentException("html 数据异常");
        }
        return xmlContent;
    }

    public IpPageResponse ipNextPage(ProxyIp proxy, String url, String token) {
        String jsonContent = nextPage(proxy, url, token);
        IpPageResponse pageContent = JSON.parseObject(jsonContent, IpPageResponse.class);
        if (!validIpPage(pageContent)) {
            throw new IllegalArgumentException("ipNextPage 数据解析异常");
        }
        return pageContent;
    }

    protected DomainPageResponse domainNextPage(ProxyIp proxy, String url, String token) {
        String jsonContent = nextPage(proxy, url, token);
        DomainPageResponse pageContent = JSON.parseObject(jsonContent, DomainPageResponse.class);
        if (pageContent == null || pageContent.getMsg() == null) {
            throw new IllegalArgumentException("domainNextPage 数据解析异常");
        }
        return pageContent;
    }

    protected String nextPage(ProxyIp proxy, String url, String token) {
        String jsonContent = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), url, token);
        if (StringUtils.isEmpty(jsonContent))
            throw new IllegalArgumentException("nextPage 数据返回null");
        return jsonContent;
    }

    protected boolean validIpPage(IpPageResponse pageContent) {
        return pageContent != null && pageContent.getMsg() != null;
    }

    // 极简映射接口（只做对象转换，无业务）
    protected interface DomainMapper<T> {
        T map(String ip, String domain, String adTime, String upTime);
    }

    // 子类重写（无回调）
    protected Object buildHomeItem(String ip, String loc, String domain, String adTime, String upTime) {
        return null;
    }

    protected Object buildPageItem(String ip, String loc, String domain, String adTime, String upTime) {
        return null;
    }

    protected void processPageData(List<?> list, BufferedWriter bw, IpBreakpoint breakpoint) throws IOException {}

}