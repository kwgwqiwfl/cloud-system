package com.ring.cloud.facade.crawl;

import com.alibaba.fastjson.JSON;
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

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抓取赛程表
 */
@Slf4j
public class WebClientUtil {
    // 目标 API 模板（page 会动态替换）
    private static final String API_URL = "https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=%d";
    // 总爬取页数
    private static final int TOTAL_PAGE = 1;

    public static void main(String[] args) {
        String proxyUrl = "http://v2.api.juliangip.com/unlimited/getips?auto_white=1&num=1&pt=1&result_type=text&split=1&trade_no=5146320972443658&sign=5c03bbd355bea6ea243ffb6c4a30990a";
        String pr = "ProxyUtil.getProxy(proxyUrl)";
        System.out.println("代理"+JSON.toJSONString(pr));
        String[] ipStrs = pr.split(":");
        String host = ipStrs[0];
        int port = Integer.parseInt(ipStrs[1]);
        System.out.println("代理地址===>"+host+"   "+port);
        long start = System.currentTimeMillis();
        WebClient webClient=null;
        try {
            // 初始化 WebClient（带请求头，防反爬）
//            webClient = create(host, port, "");
//            webClient = createWebClientWithProxy(host, port);
            String furl="https://site.ip138.com/1.1.1.1";

            try {
//                warmupWebClient(webClient);
                // 第一步：获取字节数组（避免字符串乱码）
                byte[] responseBytes = webClient.get()
                        .uri(furl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                        .retrieve()
                        .onStatus(HttpStatus::isError, resp ->
                                Mono.error(new WebClientResponseException(
                                        resp.statusCode().value(),
                                        "IP接口返回错误状态码",
                                        resp.headers().asHttpHeaders(),
                                        null,
                                        null
                                ))
                        )
                        .bodyToMono(byte[].class)
                        .block(Duration.ofSeconds(5));

                // 第二步：手动解压（复用工具方法，零报红）
                String xml = "";
                System.out.println("xml："+xml);
                long xend = System.currentTimeMillis();
                System.out.println("xml耗时："+(xend-start));
                //获得token
                Document doc = Jsoup.parse(xml);
                Elements scriptElements = doc.select("script[type='text/javascript']");//script标签
                String token = parseToken1(scriptElements);
                long tokenend = System.currentTimeMillis();
                System.out.println("第一页拿到token===>"+token+"   解析耗时："+(tokenend-xend));
            } catch (Exception e) {
                log.error("失败，"+e.getMessage(), e);
            }finally {
                // 3. 销毁连接池
            }
//            // 发送请求获取 JSON
//            String xml = webClient.get()
//                    .uri(furl)
//                    .retrieve()
//                    .bodyToMono(String.class).block();

//            String purl="https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=2&token="+token;
//            byte[] responseBytes2 = webClient.get()
//                    .uri(purl)
////                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                    .retrieve()
//                    .onStatus(HttpStatus::isError, resp ->
//                            Mono.error(new WebClientResponseException(
//                                    resp.statusCode().value(),
//                                    "IP接口返回错误状态码",
//                                    resp.headers().asHttpHeaders(),
//                                    null,
//                                    null
//                            ))
//                    )
//                    .bodyToMono(byte[].class) // 改：获取字节数组
//                    .block();
//
//            String json = WebUtil.decompressGzip(responseBytes2);
//            long jend = System.currentTimeMillis();
//            System.out.println("json耗时："+(jend-tokenend));
////            // 发送请求获取 JSON
////            String json = webClient.get()
////                    .uri(purl)
////                    .retrieve()
////                    .bodyToMono(String.class).block();
//            IpPageResponse page = JSON.parseObject(json, IpPageResponse.class);
//            System.out.println("第2拿到数据===>"+ JSON.toJSONString(page));
        } finally {
            if(webClient!=null)
                webClient.delete();
        }
    }
    //windows 代理webclient
    public static WebClient create(String host, int port, String token) {
        HttpClient httpClient = HttpClient.create()
                // ==================== 代理配置（Windows 本地代理） ====================
                .proxy(proxy -> proxy
                                // 1. 选一种：HTTP 或 SOCKS5
                                .type(ProxyProvider.Proxy.HTTP)
                                // .type(ProxyProvider.Proxy.SOCKS5)

                                .host(host)    // Windows 本地代理几乎都是这个
                                .port(port)           // 你代理的端口：7890 / 10809 / 8888
                        // 有账号密码才加
                        // .username("user")
                        // .password("pass")
                )
                .secure(t -> t.sslContext(
                        SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 忽略证书
                ))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15))
                        .addHandlerLast(new WriteTimeoutHandler(15))
                )
                .keepAlive(false); // 关键：关闭长连接，解决提前关闭错误

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .defaultHeader("Referer", "")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
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
    // 在createWebClient后、真实请求前加：
    public static void warmupWebClient(WebClient webClient) {
        try {
            // 预热超时从4秒→8秒，且用subscribe非阻塞执行，不阻塞主流程
            webClient.get().uri("https://site.ip138.com/")
                    .retrieve().bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(8)) // 匹配读写超时
                    .subscribe(null, e -> {}); // 非阻塞执行，预热失败不影响
        } catch (Exception e) {}
    }


}
