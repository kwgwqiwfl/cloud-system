package com.ring.cloud.facade.crawl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 抓取赛程表
 */
@Slf4j
public class IpUtilTest {
    public static final long wait_home = 3*1000;
    public static final long wait_page = 2*1000;

    //生成ip domain url
    public static String buildIpUrl(int page, String ip, String ipDomainUrl, String ipPageInfix, String dynamicToken) {
        StringBuilder url = new StringBuilder();
//        url="https://site.ip138.com/1.1.1.1";
//        url="https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=2&token=2ea08c94ef895a05b7df3182717f8dc2";
        if(page==1){
            url.append(ipDomainUrl).append("/").append(ip);
            return url.toString();
        }
        url.append(ipDomainUrl).append("/").append(ipPageInfix).append("/?ip=").append(ip).append("&page=").append(page).append("&token=").append(dynamicToken);
        return url.toString();
    }

    public static void main(String[] args) throws IOException {
        String filePath = "D:\\crawl\\xml\\138.txt";

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // 添加换行符以保持文件的原格式
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String content = stringBuilder.toString(); // 将StringBuilder转换为String
//        System.out.println("*****==>"+content); // 打印或使用文件内容
        Document doc = Jsoup.parse(content, "");
        Elements res = doc.getElementsByClass("result result2");
//            List<CourseEntity> courses = new ArrayList<>();
        assert res != null;
        Elements trs = res.select("h3");
        for(Element tr:trs){
            System.out.println("*****==>"+tr.text());
        }
        Elements uls = res.select("ul");
        for(Element ul:uls){
            Elements lis = ul.select("li");
            for(Element li:lis){
                String className = "";
                Element dateE = li.getElementsByClass("date").first();
                if(dateE!=null)
                    className = dateE.text(); //获取li的类名
                String text = li.text(); // 获取li的文本内容
                String link = ""; // 初始化链接为空
                Element linkElement = li.select("a").first(); // 查找li中的第一个<a>标签
                if (linkElement != null) {
//                    link = linkElement.attr("href"); // 获取<a>标签的href属性
                    link = linkElement.text();
                }
                System.out.println("==>"+className+"   "+link);
            }
        }
//        Document doc = Jsoup.parse(pageAsXml, url);
    }

    //    public boolean runTask1(IpTaskEntity ipTaskEntity) {
//        log.info("runTask start ===================================");
//        startBatchCrawl(ipTaskEntity);
//        //代理
//        ProxyIp proxyIp = proxyClient.proxyIpWithRetry();
//        if(proxyIp==null){
//            log.warn("获取代理ip失败，子任务终止");
//            return true;
//        }
//        ProxyIp proxyIp = new ProxyIp();//////////////////////////////////////////////////////////////////////////////
//        String csvPath = "D:\\crawl\\xml\\test.csv";//////////////////////////////////////////////////////////////////////////////////////////
////        log.info("代理ip==>"+ JSON.toJSONString(proxyIp));
//        WebClient webClient=null;
//        try {
//            IpReadInfo info;
//            String dynamicToken = "";
//            // 初始化 WebClient（带请求头，防反爬）
//            webClient = webClientProxyConfig.createWebClientWithProxy(proxyIp.getIp(), proxyIp.getPort());
//            BufferedWriter bw = csvWriter.getWriter(csvPath);
//            // 循环请求每一页
//            for (int page = 1; page <= 1; page++) {
//                ip = "1.1.1.1";///////////////////////////////////////////////////////////////////////////////////////
//                String url = IpUtilTest.buildIpUrl(page, ip, ipDomainUrl, ipPageInfix, dynamicToken);
//                System.out.println("正在爬取第 " + page + " 页：" + url);
////                // 发送请求获取 JSON
////                String json = webClient.get()
////                        .uri(url)
////                        .header("Authorization", "Bearer " + dynamicToken)
////                        .retrieve()
////                        .bodyToMono(String.class).block();
////                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
////                        .defaultHeader("Referer", "")
//                //解析数据
//
//                //第一页数据
////                if(page==1){
////
////                }else{
////
////                }
//                info = parseIpHtmlStr(bw, ip);
//                log.info("返回信息1："+JSON.toJSONString(info));
//                parseIpPageJson(bw, ip, info);
//                log.info("返回信息2："+JSON.toJSONString(info));
////                System.out.println(page + "===============>" + json);
//                //            // 解析 JSON 提取列表数据
//                //            List<Object> pageData = parseJsonToList(json);
//                //            allData.addAll(pageData);
//
//                // 防止请求过快被封 IP
//                Thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            csvWriter.closeFile(csvPath);
//        }
//        return true;
//    }
//
//    //解析首次html页
//    public IpReadInfo parseIpHtmlStr(BufferedWriter bw, String resContent, String currentIp) throws IOException {
//        IpReadInfo info = new IpReadInfo();
//        String filePath = "D:\\crawl\\xml\\138.txt";
//        StringBuilder stringBuilder = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line).append("\n"); // 添加换行符以保持文件的原格式
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String content = stringBuilder.toString(); //将StringBuilder转换为String
//
//        Document doc = Jsoup.parse(resContent);
//        Elements scriptElements = doc.select("script[type='text/javascript']");//script标签
//        Elements res = doc.getElementsByClass("result result2");//结果
//        Elements trs = res.select("h3");//归属
//        Elements uls = res.select("ul");//列表
//        if(CollectionUtils.isEmpty(scriptElements) && CollectionUtils.isEmpty(res) && CollectionUtils.isEmpty(trs) && CollectionUtils.isEmpty(uls))
//            return info;
//        info.setToken(parseToken(scriptElements));
//        info.setLoc(trs.get(0).text());
//        StringBuilder batchSb = new StringBuilder(BATCH_SIZE);
//        int size = 0;
//        for(Element ul:uls){
//            Elements lis = ul.select("li");
//            for(Element li:lis){
//                Element dateE = li.getElementsByClass("date").first();
//                if(dateE==null) continue;
//                String dateStr = dateE.text(); //获取li的类名
//                if(!dateStr.contains("-----")) continue;
//                String[] dates = dateStr.split("-----");//2026-03-17-----2026-03-17
//                Element linkElement = li.select("a").first(); // 查找li中的第一个<a>标签
//                if (linkElement == null) continue;
//                appendCsvRow(batchSb, dates[0], linkElement.text(), currentIp, info.getLoc(), dates[1]);
//                size++;
//                if (batchSb.length() >= BATCH_SIZE)
//                    batchWrite(bw, batchSb);
//            }
//            if (batchSb.length() > 0) batchWrite(bw, batchSb);
//        }
//        info.setPageSize(size);
//        info.setSuccess(true);
//        return info;
//    }
//
//    //解析分页
//    public IpReadInfo parseIpPageJson(BufferedWriter bw, IpPageResponse pageResponse, String currentIp, String loc) throws IOException {
////        String filePath = "D:\\crawl\\xml\\138json.txt";
////        StringBuilder stringBuilder = new StringBuilder();
////        try (BufferedReader reader = new BufferedReader(
////                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
////            String line;
////            while ((line = reader.readLine()) != null) {
////                stringBuilder.append(line).append("\n"); // 添加换行符以保持文件的原格式
////            }
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////        String content = stringBuilder.toString(); //将StringBuilder转换为String
//
////        IpPageResponse pageResponse = JSON.parseObject(content, IpPageResponse.class);
//        IpReadInfo info = new IpReadInfo();
//        if(pageResponse == null || CollectionUtils.isEmpty(pageResponse.getData())){
//            return info;
//        }
//        StringBuilder batchSb = new StringBuilder(BATCH_SIZE);
//        List<IpPageData> dataList = pageResponse.getData();
//        int size = 0;
//        for(IpPageData pageData:dataList){
//            appendCsvRow(batchSb, pageData.getAddtime(), pageData.getDomain(), currentIp, loc, pageData.getUptime());
//            size++;
//            if (batchSb.length() >= BATCH_SIZE) batchWrite(bw, batchSb);
//        }
//        if (batchSb.length() > 0) batchWrite(bw, batchSb);
//        info.setPageSize(size);
//        info.setSuccess(true);
//        return info;
//    }

}
