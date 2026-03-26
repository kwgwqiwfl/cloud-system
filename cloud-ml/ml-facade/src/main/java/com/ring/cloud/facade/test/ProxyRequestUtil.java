package com.ring.cloud.facade.test;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.entity.ip.IpPageResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 动态代理请求工具类
 * 核心特性：
 * 1. HttpClient全局仅初始化一次，复用连接池提升效率
 * 2. 每次切换代理新建WebClient，用完即弃无内存泄漏
 * 3. 30秒统一超时控制，异常兜底减少报错
 */
public class ProxyRequestUtil {
    // 30秒最大超时（所有链路统一超时控制）
    private static final int MAX_TIMEOUT_SEC = 10;
    private static final int MAX_TIMEOUT_MS = MAX_TIMEOUT_SEC * 1000;
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(MAX_TIMEOUT_SEC);

    // 全局HttpClient（仅初始化一次，复用核心资源）
    private static final HttpClient BASE_HTTP_CLIENT;

    // 静态初始化：HttpClient仅加载一次
    static {
        try {
            // SSL配置（忽略证书适配测试场景）
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .protocols("TLSv1.2")
                    .build();

            // 初始化HttpClient核心配置
            BASE_HTTP_CLIENT = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, MAX_TIMEOUT_MS)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .responseTimeout(MAX_TIMEOUT)
                    .secure(sslSpec -> sslSpec.sslContext(sslContext))
                    .doOnConnected(conn -> {
                        conn.addHandlerLast(new ReadTimeoutHandler(MAX_TIMEOUT_SEC));
                        conn.addHandlerLast(new WriteTimeoutHandler(MAX_TIMEOUT_SEC));
                    });

            // 异步预热HttpClient，提升首次请求效率
            BASE_HTTP_CLIENT.warmup().subscribe();
        } catch (SSLException e) {
            throw new RuntimeException("HttpClient初始化失败", e);
        }
    }

    /**
     * 根据代理信息创建WebClient（每次切换代理新建）
     * @param proxyHost 代理IP
     * @param proxyPort 代理端口
     * @return 绑定指定代理的WebClient
     */
    private static WebClient createWebClient(String proxyHost, int proxyPort) {
        // 绑定当前代理配置
        HttpClient proxyHttpClient = BASE_HTTP_CLIENT.proxy(proxySpec -> {
            proxySpec.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort)
                    .connectTimeoutMillis(MAX_TIMEOUT_MS);
        });

        // 构建轻量级WebClient（无缓存，用完即弃）
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(proxyHttpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .build();
    }

    /**
     * 执行GET请求（异常分层兜底，减少报错）
     * @param webClient 绑定代理的WebClient
     * @param requestUrl 请求地址
     * @param token 认证令牌
     * @return 响应字节数组
     */
    private static byte[] doRequest(WebClient webClient, String requestUrl, String token) {
        try {
            return webClient.get()
                    .uri(requestUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatus::isError, response -> Mono.empty())
                    .bodyToMono(byte[].class)
                    .timeout(MAX_TIMEOUT)
                    .onErrorResume(WebClientResponseException.class, e -> {
                        System.err.println("请求响应异常：" + e.getStatusCode() + ", URL：" + requestUrl);
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, e -> {
                        System.err.println("请求执行异常：" + requestUrl + ", 原因：" + e.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            System.err.println("请求兜底异常：" + requestUrl + ", 原因：" + e.getMessage());
            return null;
        }
    }

    /**
     * Gzip解压（容错处理，避免解压报错）
     * @param compressedBytes 压缩字节数组
     * @return 解压后的字符串
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
            return new String(compressedBytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 测试方法：高频切换代理场景验证
     */
    public static void main(String[] args) {
//        String proxyUrl = "http://v2.api.juliangip.com/unlimited/getips?auto_white=1&num=1&pt=1&result_type=text&split=1&trade_no=5146320972443658&sign=5c03bbd355bea6ea243ffb6c4a30990a";
//        String pr = ProxyUtil.getProxy(proxyUrl);
//        System.out.println("代理"+ JSON.toJSONString(pr));
//        String[] ipStrs = pr.split(":");
//        String host = ipStrs[0];
//        int port = Integer.parseInt(ipStrs[1]);
//        System.out.println("代理地址===>"+host+"   "+port);
        long start = System.currentTimeMillis();
        String host = "192.168.1.1";
        int port = 8080;
        WebClient webClient=null;
        try {
            String furl="https://site.ip138.com/1.1.1.1";
            webClient = createWebClient(host, port);
            long wend = System.currentTimeMillis();
            System.out.println("webClient耗时："+(wend-start));
            String xml = safeDecompress(doRequest(webClient, furl, ""));
            System.out.println("xml："+xml.length());
            long xend = System.currentTimeMillis();
            System.out.println("xml耗时："+(xend-wend));
            //获得token
            Document doc = Jsoup.parse(xml);
            Elements scriptElements = doc.select("script[type='text/javascript']");//script标签
            String token = parseToken1(scriptElements);
            long tokenend = System.currentTimeMillis();
            System.out.println("第一页拿到token===>"+token+"   解析耗时："+(tokenend-xend));

            String purl="https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=2&token="+token;
            String json = safeDecompress(doRequest(webClient, purl, ""));
            long jend = System.currentTimeMillis();
            System.out.println("json："+json.length());
            System.out.println("json耗时："+(jend-tokenend));
            IpPageResponse page = JSON.parseObject(json, IpPageResponse.class);
            System.out.println("第2拿到数据===>"+ page.getData().size());
        } finally {
//            if(webClient!=null)
//                webClient.delete();
        }
    }

    public static String parseToken1(Elements scriptElements){
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
}