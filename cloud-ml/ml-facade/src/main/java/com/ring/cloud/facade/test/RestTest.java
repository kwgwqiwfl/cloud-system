package com.ring.cloud.facade.test;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.util.IpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class RestTest {
    public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ProxyIp proxy = doGetProxyIpList().get(0);
        String url = IpUtil.buildPangUrl("1.65.83.0", "https://chapangzhan.com");
//        url = "https://httpbin.org/get";
        RestTemplate restTemplate = createWebClient(proxy.getIp(), proxy.getPort());
        String xmlContent = doProxyRequest(restTemplate, url, "");
        System.out.println("---"+xmlContent);
    }

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

    // 固定超时（你要求：去掉随机）
    private static final int CONNECT_TIMEOUT = 2500;
    private static final int READ_TIMEOUT = 7000;
    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;

    private PoolingHttpClientConnectionManager connectionManager;
    private SSLContext sslContext;

    public static RestTemplate createWebClient(String proxyHost, int proxyPort) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        // 全局连接池 + 全局 HttpClient，只初始化一次
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(300);
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setValidateAfterInactivity(1000);

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .disableAutomaticRetries()
                .evictIdleConnections(10, TimeUnit.SECONDS)
                .build();
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setProxy(new HttpHost(proxyHost, proxyPort))
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(1500)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
//        factory.setConnectTimeout(CONNECT_TIMEOUT);
//        factory.setReadTimeout(READ_TIMEOUT);

        // 修正 Lambda 参数：接收 HttpMethod 和 URI 两个参数
        factory.setHttpContextFactory((httpMethod, uri) -> {
            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(config);
            return context;
        });

        return new RestTemplate(factory);
    }

    private static byte[] doRequest(RestTemplate restTemplate, String requestUrl, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
//            headers.set("Referer", "https://www.baidu.com/"); // 模拟从百度跳转
            headers.set("Upgrade-Insecure-Requests", "1");
            headers.set("Sec-Fetch-Dest", "document");
            headers.set("Sec-Fetch-Mode", "navigate");
            headers.set("Sec-Fetch-Site", "cross-site");
            headers.set("Sec-Fetch-User", "?1");
            headers.set("DNT", "1"); //  Do Not Track，更像人类

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("request_error");
            }
            return response.getBody();

        } catch (Throwable e) {
            throw new IllegalArgumentException("request_failed");
        }
    }

    // ====================== 优化解压防卡死，逻辑完全不变 ======================
    private static String safeDecompress(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";  // 这里不会再触发了
        }

        // 判断是否GZIP
        boolean isGzip = compressedBytes.length >= 2 &&
                (compressedBytes[0] & 0xFF) == 0x1F &&
                (compressedBytes[1] & 0xFF) == 0x8B;

        if (!isGzip) {
            return new String(compressedBytes, StandardCharsets.UTF_8);
        }

        try (InputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            long total = 0;
            while ((len = gzipIn.read(buffer)) != -1) {
                total += len;
                if (total > MAX_GZIP_SIZE) {
                    throw new IllegalArgumentException("decompress_failed");
                }
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(compressedBytes, StandardCharsets.UTF_8);
        }
    }

    public static String doProxyRequest(RestTemplate restTemplate, String requestUrl, String token) {
        return safeDecompress(doRequest(restTemplate, requestUrl, token));
    }

    public static List<ProxyIp> doGetProxyIpList() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(5000);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 解决中文乱码问题
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        String proxyStr = restTemplate.getForObject("http://v2.api.juliangip.com/unlimited/getips?auto_white=1&num=10&pt=1&result_type=text&split=1&trade_no=5146320972443658&sign=6004c288e00c37af81095e47defee8d7", String.class);
        if (StringUtils.isEmpty(proxyStr) || !proxyStr.contains(":")) {
            throw new IllegalArgumentException("代理IP格式不正确：" + proxyStr);
        }
        return Stream.of(proxyStr.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ipPort -> {
                    String[] arr = ipPort.split(":");
                    ProxyIp proxyIp = new ProxyIp();
                    proxyIp.setIp(arr[0]);
                    proxyIp.setPort(Integer.parseInt(arr[1]));
                    return proxyIp;
                })
                .collect(Collectors.toList());
    }
}
