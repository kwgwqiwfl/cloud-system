package com.ring.cloud.facade.execute.IpDomain;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.entity.ip.IpPageData;
import com.ring.cloud.facade.entity.ip.IpPageResponse;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyBase;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.util.FileUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class IpBaseExecutor {

    @Autowired
    protected OkProxyBase okProxyBase;
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;

    @Value("${ml.client.desc.ip.domain:null}")
    protected String ipDomainUrl;

    @Value("${ml.client.desc.ip.page.infix:null}")
    protected String ipPageInfix;

    // ============== 首页完整请求 + 解析 + 写入（公共，仅段1会调用） ==============
    public IpReadInfo fetchAndParseFirstPage(BufferedWriter bw, String currentIp, ProxyIp proxy) throws IOException {
        String xmlContent = null;
        try {
            xmlContent = ipFirstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl));
            IpReadInfo info = parseFirstPageHtml(bw, xmlContent, currentIp);

            if (info.getBatchSb() != null && info.getBatchSb().length() > 0) {
                batchWrite(bw, info.getBatchSb());
            }
            info.setBatchSb(null);
            return info;
        } finally {
            xmlContent = null;
        }
    }

    // ============== 首页HTML解析 ==============
    public IpReadInfo parseFirstPageHtml(BufferedWriter bw, String xmlContent, String currentIp) throws IOException {
        IpReadInfo info = new IpReadInfo();
        Document doc = Jsoup.parse(xmlContent);
        Elements scriptElements = doc.select("script[type='text/javascript']");
        Elements res = doc.getElementsByClass("result result2");
        Elements trs = res.select("h3");
        Elements uls = res.select("ul");

        info.setToken(parseToken(scriptElements));
        info.setLoc(trs.get(0).text());

        StringBuilder batchSb = new StringBuilder(FileUtil.BATCH_SIZE);
        int size = 0;
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

                appendCsvRow(batchSb, linkElement.text(), currentIp, info.getLoc(), dates[0], dates[1]);
                size++;
            }
        }
        info.setBatchSb(batchSb);
        info.setPageSize(size);
        doc = null;
        return info;
    }


    // ============== 普通页单页爬取 + 写入 ==============
    public int parseSinglePage(BufferedWriter bw, String currentIp, String loc, int page, String token, ProxyIp proxy) throws IOException {
        String url = IpUtil.buildIpUrlPage(page, currentIp, ipDomainUrl, ipPageInfix, token);
        IpPageResponse pageResp = ipNextPage(proxy, url, token);

        if (CollectionUtils.isEmpty(pageResp.getData())) {
            return 0;
        }
        int size = 0;
        StringBuilder batchSb = new StringBuilder(FileUtil.BATCH_SIZE);
        for (IpPageData data : pageResp.getData()) {
            size++;
            appendCsvRow(batchSb, data.getDomain(), currentIp, loc, data.getAddtime(), data.getUptime());
            if (batchSb.length() >= FileUtil.BATCH_SIZE) batchWrite(bw, batchSb);
        }
        if (batchSb.length() > 0) batchWrite(bw, batchSb);
        pageResp = null;
        return size;
    }

    // ============== 底层请求 ==============
    public String ipFirstPage(ProxyIp proxy, String url) {
        String xmlContent = okProxyBase.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
        if (!IpUtil.validStrContains(xmlContent, "script", "result result2", "h3", "ul"))
            throw new IllegalArgumentException("ipFirstPage 数据异常");
        return xmlContent;
    }

    public IpPageResponse ipNextPage(ProxyIp proxy, String url, String token) {
        String jsonContent = okProxyBase.doProxyRequest(proxy.getIp(), proxy.getPort(), url, token);
        if (StringUtils.isEmpty(jsonContent))
            throw new IllegalArgumentException("ipNextPage 数据返回null");
        IpPageResponse pageContent = JSON.parseObject(jsonContent, IpPageResponse.class);
        if (!validIpPage(pageContent)) {
            throw new IllegalArgumentException("ipNextPage 数据解析异常");
        }
        jsonContent = null;
        return pageContent;
    }
    protected boolean validIpPage(IpPageResponse pageContent) {
        return pageContent != null && pageContent.getMsg() != null;
    }

    // ============== 工具 ==============
    protected String parseToken(Elements scriptElements) {
        Pattern tokenPattern = Pattern.compile("var _TOKEN = '(.*?)';");
        for (Element script : scriptElements) {
            Matcher tokenMatcher = tokenPattern.matcher(script.html());
            if (tokenMatcher.find()) return tokenMatcher.group(1);
        }
        return null;
    }
    //token和loc
    protected IpReadInfo parseTokenAndLoc(String xml) {
        IpReadInfo info = new IpReadInfo();
        Document doc = Jsoup.parse(xml);
        info.setToken(parseToken(doc.select("script[type='text/javascript']")));
        info.setLoc(doc.getElementsByClass("result result2").select("h3").text());
        return info;
    }

    protected void appendCsvRow(StringBuilder batchSb, String domain, String ip, String loc, String addTime, String upTime) {
        batchSb.append(ip)
                .append(",").append(loc).append(",").append(domain)
                .append(",").append(addTime).append(",").append(upTime).append('\n');
    }

    protected void batchWrite(BufferedWriter bw, StringBuilder batchSb) throws IOException {
        bw.write(batchSb.toString());
        bw.flush();
        batchSb.setLength(0);
    }


}