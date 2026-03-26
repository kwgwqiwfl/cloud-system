package com.ring.cloud.facade.frame;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * 代理webclient
 */
@Slf4j
@Component
public class WebClientProxyBase {
//    @Value("${ml.client.proxy.connect.timeout:10}")
//    private int maxTimeoutSec;// 超时配置：10秒（连接/读写/响应统一超时）
//    private int maxTimeoutMs;
//    private Duration maxTimeout;
    private SslContext sslContext;
    // 用于预热的【无代理模板】
    private HttpClient warmupTemplate;

    private static final List<String> UA_POOL = Collections.unmodifiableList(Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    ));
    private static final List<String> ACCEPT_POOL = Collections.unmodifiableList(Arrays.asList(
            "application/json, text/plain, */*",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    ));
    private static final Random random = new Random(System.nanoTime());

    /**
     * Spring初始化后赋值：保证@Value已生效
     */
    @PostConstruct
    public void initTimeoutParams() throws SSLException {
//        maxTimeoutMs = maxTimeoutSec * 1000;
//        maxTimeout = Duration.ofSeconds(maxTimeoutSec);
        // SSL 只创建一次
        sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .protocols("TLSv1.2")
                .build();
        // ==========================
        // 预热模板
        // ==========================
        warmupTemplate = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)//全局无代理连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2500 + random.nextInt(1001))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
//                .responseTimeout(Duration.ofSeconds(9))
                // 整体响应超时：8~10秒随机
                .responseTimeout(Duration.ofSeconds(8 + random.nextInt(3)))
                .secure(spec -> spec.sslContext(sslContext))
                .doOnConnected(conn -> {
//                    conn.addHandlerLast(new ReadTimeoutHandler(4));
//                    conn.addHandlerLast(new WriteTimeoutHandler(2));
                    // 读超时：3~5秒随机
                    conn.addHandlerLast(new ReadTimeoutHandler(3 + random.nextInt(3)));
                    // 写超时：1~3秒随机
                    conn.addHandlerLast(new WriteTimeoutHandler(1 + random.nextInt(3)));
                });

        // 预热！！！
        warmupTemplate.warmup().subscribe();
    }

    /**
     * 创建绑定指定代理的WebClient（每次切换代理新建，轻量级无内存泄漏）
     * @param proxyHost 代理IP
     * @param proxyPort 代理端口
     * @return 绑定代理的WebClient实例
     */
    public WebClient createWebClient(String proxyHost, int proxyPort) {
        HttpClient finalClient = warmupTemplate
                .proxy(spec -> {
                    spec.type(ProxyProvider.Proxy.HTTP)
                            .host(proxyHost)
                            .port(proxyPort)
                            .connectTimeoutMillis(2500 + random.nextInt(1001));
//                            .connectTimeoutMillis(3000);
                });

        // 构建WebClient（仅包装配置，无核心资源）
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(finalClient))
//                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                // 随机UA，不暴露固定指纹
                .defaultHeader(HttpHeaders.USER_AGENT, UA_POOL.get(random.nextInt(UA_POOL.size())))
//                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_POOL.get(random.nextInt(ACCEPT_POOL.size())))
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
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
    private byte[] doRequest(WebClient webClient, String requestUrl, String token) {
        try {
            return webClient.get()
                    .uri(requestUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatus::isError, response ->
//                            Mono.error(new IllegalArgumentException("请求状态码异常：" + response.statusCode() + "，URL：" + requestUrl))
                            Mono.error(new IllegalArgumentException("request_error"))
                    )
                    .bodyToMono(byte[].class)
//                    .timeout(maxTimeout)
//                    .onErrorResume(WebClientResponseException.class, e ->
//                            Mono.error(new IllegalArgumentException("响应异常：" + e.getStatusCode() + "，URL：" + requestUrl, e))
//                    )
//                    .onErrorResume(Exception.class, e ->
//                            Mono.error(new IllegalArgumentException("请求执行异常：" + requestUrl + "，原因：" + e.getMessage(), e))
//                    )

                    .block(Duration.ofSeconds(14 + random.nextInt(3)));
        } catch (Throwable e) {
            throw new IllegalArgumentException("request_failed");
        }
    }

    /**
     * Gzip解压（容错处理，异常抛出）
     * @param compressedBytes 压缩字节数组
     * @return 解压后的字符串
     * @throws Exception 抛出解压相关异常
     */
    private String safeDecompress(byte[] compressedBytes) {
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
            throw new IllegalArgumentException("decompress_failed");
        }
    }

    public String doProxyRequest(WebClient webClient, String requestUrl, String token){
        return safeDecompress(doRequest(webClient, requestUrl, token));
    }

}
