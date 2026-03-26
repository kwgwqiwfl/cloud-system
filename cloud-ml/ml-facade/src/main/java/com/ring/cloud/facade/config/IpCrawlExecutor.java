package com.ring.cloud.facade.config;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.entity.ip.*;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.RestProxyBase;
import com.ring.cloud.facade.util.IpUtil;
import com.ring.welkin.common.utils.Snowflake;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ring.cloud.facade.util.FileUtil.BATCH_SIZE;

@Slf4j
@Component
public class IpCrawlExecutor {

    @Autowired
    protected RestProxyBase restProxyBase;

    @Value("${ml.client.desc.ip.domain:null}")
    protected String ipDomainUrl;
    @Value("${ml.client.desc.ip.page.infix:null}")
    protected String ipPageInfix;

    //采集处理方法
    public IpReadInfo crawlIp(BufferedWriter bw, IpTaskEntity ipTaskEntity, String currentIp
            , ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo finalReadInfo = new IpReadInfo();
        boolean hasMore = false; // 是否有更多页需要查
            // ========== 第一步：初始化首页查询 ==========
        RestTemplate restTemplate = restProxyBase.createWebClient(proxy.getIp(), proxy.getPort());
        String firstUrl = IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl);
        String xmlContent = ipFirstPage(restTemplate, firstUrl);
        IpReadInfo firstInfo;
        if (breakpoint.getCurrentPage() == 1) {
            breakpoint.setCurrentPage(breakpoint.getCurrentPage()+1);
            firstInfo = parseIpHtmlStr(bw, xmlContent, currentIp, breakpoint);
            if(firstInfo.getPageSize()<100) {//第一页少于100条数据时，认为当前ip任务已完成
                finalReadInfo.setSuccess(true);
                return finalReadInfo;
            }
            hasMore = true;
        } else {
            firstInfo = parseIpHtmlTokenOnly(xmlContent);
            hasMore = true;
        }
        // ========== 第二步：内部循环逐页查询（核心：判断完直接查下一页） ==========
        while (hasMore) {
            String pageUrl = IpUtil.buildIpUrlPage(breakpoint.getCurrentPage(), currentIp, ipDomainUrl, ipPageInfix, firstInfo.getToken());
            IpPageResponse pageContent = ipNextPage(restTemplate, pageUrl, firstInfo.getToken());
            breakpoint.setCurrentPage(breakpoint.getCurrentPage()+1);
            IpReadInfo pageInfo = parseIpPageJson(bw, pageContent, currentIp, firstInfo.getLoc(), breakpoint);

            // 3. 逐页判断：是否需要继续查下一页（核心：判断完直接循环，无需外部调用）
            if (pageInfo.getPageSize()<100)
                hasMore = false;
            pageContent = null;
            pageInfo = null;
        }
        xmlContent = null;
        finalReadInfo.setSuccess(true);
        return finalReadInfo;
    }

    //查询首页
    public String ipFirstPage(RestTemplate restTemplate, String url){
        String xmlContent = restProxyBase.doProxyRequest(restTemplate, url, "");
        if(!IpUtil.validStrContains(xmlContent, "script", "result result2", "h3", "ul"))//校验html字符串
            throw new IllegalArgumentException("ipFirstPage 数据异常");
        return xmlContent;
    }
    //分页查询
    public IpPageResponse ipNextPage(RestTemplate restTemplate, String url, String token){
        String jsonContent = restProxyBase.doProxyRequest(restTemplate, url, token);
        if(StringUtils.isEmpty(jsonContent))
            throw new IllegalArgumentException("ipNextPage 数据返回null");//校验返回对象
        IpPageResponse pageContent = JSON.parseObject(jsonContent, IpPageResponse.class);
        if(!validIpPage(pageContent)) {
//            System.out.println("pageContent=="+JSON.toJSONString(pageContent));
            throw new IllegalArgumentException("ipNextPage 数据解析异常");//校验返回对象
        }
        jsonContent = null;
        return pageContent;
    }

//    //线程断点
//    public void pointMarker(String currentIp, AtomicBoolean isWriting, AtomicBoolean isInterruptedFlag) throws InterruptedException {
//        boolean isInterrupted = Thread.currentThread().isInterrupted() || isInterruptedFlag.get();
//        if (isInterrupted) {
//            isWriting.set(false);
//            throw new InterruptedException(currentIp+" 当前代理被超时中断，子线程自动终止");
//        }
//    }

    //解析html页ip级别信息
    public IpReadInfo parseIpHtmlTokenOnly(String resContent) {
        IpReadInfo info = new IpReadInfo();
        Document doc = Jsoup.parse(resContent);
        Elements scriptElements = doc.select("script[type='text/javascript']");//script标签
        Elements res = doc.getElementsByClass("result result2");//结果
        Elements trs = res.select("h3");//归属
        info.setToken(parseToken(scriptElements));
        info.setLoc(trs.get(0).text());
        doc=null;
        return info;
    }
    //解析首次html页 包含数据
    public IpReadInfo parseIpHtmlStr(BufferedWriter bw, String resContent, String currentIp, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo info = new IpReadInfo();
        Document doc = Jsoup.parse(resContent);
        Elements scriptElements = doc.select("script[type='text/javascript']");//script标签
        Elements res = doc.getElementsByClass("result result2");//结果
        Elements trs = res.select("h3");//归属
        Elements uls = res.select("ul");//列表
        info.setToken(parseToken(scriptElements));
        info.setLoc(trs.get(0).text());
        StringBuilder batchSb = new StringBuilder(BATCH_SIZE);
        int size = 0;
        for(Element ul:uls){
            Elements lis = ul.select("li");
            for (Element li : lis) {
                Element dateE = li.getElementsByClass("date").first();
                if (dateE == null) continue;
                String dateStr = dateE.text(); //获取li的类名
                if (!dateStr.contains("-----")) continue;
                String[] dates = dateStr.split("-----");//2026-03-17-----2026-03-17
                Element linkElement = li.select("a").first(); // 查找li中的第一个<a>标签
                if (linkElement == null) continue;
                appendCsvRow(batchSb, linkElement.text(), currentIp, info.getLoc(), dates[0], dates[1]);
                size++;
                if (batchSb.length() >= BATCH_SIZE)
                    batchWrite(bw, batchSb);
            }
            breakpoint.addCurrentCount(size);
            if (batchSb.length() > 0) batchWrite(bw, batchSb);
        }
        info.setPageSize(size);
        doc = null;
        return info;
    }

    //解析分页
    public IpReadInfo parseIpPageJson(BufferedWriter bw, IpPageResponse pageResponse, String currentIp, String loc, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo info = new IpReadInfo();
        StringBuilder batchSb = new StringBuilder(BATCH_SIZE);
        List<IpPageData> dataList = pageResponse.getData();
        if(CollectionUtils.isEmpty(dataList)){
            info.setPageSize(0);
            return info;
        }
        int size = 0;
        for (IpPageData pageData : dataList) {
            appendCsvRow(batchSb, pageData.getDomain(), currentIp, loc, pageData.getAddtime(), pageData.getUptime());
            size++;
            if (batchSb.length() >= BATCH_SIZE) batchWrite(bw, batchSb);
        }
        breakpoint.addCurrentCount(dataList.size());
        if (batchSb.length() > 0) batchWrite(bw, batchSb);
        info.setPageSize(size);
        dataList = null;
        return info;
    }
    //ip行数据拼接
    private void appendCsvRow(StringBuilder batchSb, String domain, String ip, String loc, String addTime, String upTime) {
        batchSb.append(Snowflake.longId()).append(",").append(ip)
                .append(",").append(loc).append(",").append(domain).append(",").append(addTime).append(",").append(upTime).append('\n');
    }
    //解析token
    public String parseToken(Elements scriptElements){
        // 正则表达式：匹配 var 变量 = '值' 格式
        Pattern tokenPattern = Pattern.compile("var _TOKEN = '(.*?)';");
        // 遍历 script 标签，提取变量值
        for (Element script : scriptElements) {
            String scriptContent = script.html();
            Matcher tokenMatcher = tokenPattern.matcher(scriptContent);
            if (tokenMatcher.find()) {
                return tokenMatcher.group(1);
            }
        }
        return null;
    }

    /**
     * ip校验分页结果
     */
    public boolean validIpPage(IpPageResponse pageContent) {
        return pageContent != null && pageContent.getMsg() != null;
    }
    //批量写入
    protected void batchWrite(BufferedWriter bw, StringBuilder batchSb) throws IOException {
        bw.write(batchSb.toString());
        bw.flush();
        batchSb.setLength(0);
    }
}
