package com.ring.cloud.facade.test;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.config.PangIpServcie;
import com.ring.cloud.facade.crawl.ProxyUtil;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.util.IpUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 抓取赛程表
 */
@Slf4j
public class PangUtilTest {

    public static void main(String[] args) {
        String proxyUrl = "http://v2.api.juliangip.com/unlimited/getips?auto_white=1&num=1&pt=1&result_type=text&split=1&trade_no=5146320972443658&sign=5c03bbd355bea6ea243ffb6c4a30990a";
        String pr = "ProxyUtil.getProxy(proxyUrl)";
        System.out.println("代理"+ JSON.toJSONString(pr));

        String[] ipStrs = pr.split(":");
        String host = ipStrs[0];
        int port = Integer.parseInt(ipStrs[1]);
        System.out.println("代理地址===>"+host+"   "+port);
        long start = System.currentTimeMillis();
        WebClient webClient = createWebClient(host, port);
        String purl="https://chapangzhan.com/3.1.1.0/24";
        String xml = doProxyRequest(webClient,purl,"");
        System.out.println("xml："+xml);
        List<String> pangIpList = IpUtil.parsePangValidIps(xml);
        System.out.println("pangIpList："+pangIpList);
    }
    public static HttpClient initHttpClient() {
        try {
            // SSL上下文配置：忽略证书校验（适配测试/内网场景）
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .protocols("TLSv1.2")
                    .build();

            // 初始化HttpClient核心参数
            return HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .responseTimeout(Duration.ofSeconds(10))
                    .secure(sslSpec -> sslSpec.sslContext(sslContext))
                    .doOnConnected(conn -> {
                        conn.addHandlerLast(new ReadTimeoutHandler(10));
                        conn.addHandlerLast(new WriteTimeoutHandler(10));
                    });

        } catch (SSLException e) {
            throw new RuntimeException("HttpClient初始化失败", e);
        }
    }
    public static WebClient createWebClient(String proxyHost, int proxyPort) {
        // 绑定代理配置到HttpClient
        HttpClient proxyHttpClient = initHttpClient().proxy(proxySpec -> {
            proxySpec.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort)
                    .connectTimeoutMillis(10000);
        });

        // 构建WebClient（仅包装配置，无核心资源）
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(proxyHttpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .build();
    }

    /**
     * 执行GET请求（异常直接抛出，便于上层处理）
     * @param webClient 绑定代理的WebClient
     * @param requestUrl 请求地址
     * @param token 认证令牌
     * @return 响应字节数组
     * @throws Exception 抛出请求相关所有异常
     */
    private static byte[] doRequest(WebClient webClient, String requestUrl, String token) {
        try {
            return webClient.get()
                    .uri(requestUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatus::isError, response ->
                            Mono.error(new IllegalArgumentException("请求状态码异常：" + response.statusCode() + "，URL：" + requestUrl))
                    )
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(WebClientResponseException.class, e ->
                            Mono.error(new IllegalArgumentException("响应异常：" + e.getStatusCode() + "，URL：" + requestUrl, e))
                    )
                    .onErrorResume(Exception.class, e ->
                            Mono.error(new IllegalArgumentException("请求执行异常：" + requestUrl + "，原因：" + e.getMessage(), e))
                    )
                    .block();
        } catch (Throwable e) {
            throw new IllegalArgumentException("请求失败：" + e.getMessage());
        }
    }

    /**
     * Gzip解压（容错处理，异常抛出）
     * @param compressedBytes 压缩字节数组
     * @return 解压后的字符串
     * @throws Exception 抛出解压相关异常
     */
    private static String safeDecompress(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";
        }
        try (InputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Gzip解压失败", e);
        }
    }

    public static String doProxyRequest(WebClient webClient, String requestUrl, String token){
        return safeDecompress(doRequest(webClient, requestUrl, token));
    }


//    public static void main(String[] args) throws IOException {
//        try{
//            String filePath = "D:\\crawl\\xml\\pang.txt";
//            StringBuilder stringBuilder = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    stringBuilder.append(line).append("\n"); // 添加换行符以保持文件的原格式
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            String content = stringBuilder.toString(); // 将StringBuilder转换为String
//        System.out.println("content==>"+content.length());
//
//            Document doc = Jsoup.parse(content);
//
////            // 1. 定位表格
//            Element table = doc.selectFirst("div.c-bd table");
////            if (table == null) {
////                System.out.println("未找到表格");
////                return;
////            }
//            assert table != null;
////            // ====================== 解析表头 ======================
////            List<String> headers = new ArrayList<>();
////            Elements thList = table.select("thead tr th");
////            for (Element th : thList) {
////                headers.add(th.text().trim());
////            }
////            System.out.println("表头：" + headers);
//            // 输出：[iP, 历史发现, 半年内, 一个月内]
//
//
//            // ====================== 解析tbody里的所有行 ======================
//            Elements trList = table.select("tbody tr.J_link");
//            for (Element tr : trList) {
//                Elements tdList = tr.select("td");
//                // 第1列：IP
//                String ip = tdList.get(0).text();
//                // 第2列：历史发现
//                String history = tdList.get(1).text();
//                // 第3列：半年内
//                String halfYear = tdList.get(2).text();
//                // 第4列：一个月内
//                String month = tdList.get(3).text();
//
//                System.out.println("IP：" + ip + " | 历史发现：" + history + " | 半年内：" + halfYear + " | 一个月内：" + month);
//            }
//
////            // ====================== 解析tfoot总计 ======================
////            Element tfoot = table.selectFirst("tfoot tr");
////            if (tfoot != null) {
////                String totalInfo = tfoot.text();
////                System.out.println("总计信息：" + totalInfo);
////            }
//
//        }finally {
//        }
//    }

}
