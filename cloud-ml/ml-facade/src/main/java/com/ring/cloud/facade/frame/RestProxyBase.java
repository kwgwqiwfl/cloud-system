package com.ring.cloud.facade.frame;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class RestProxyBase {

    private static final Random random = new Random(System.nanoTime());
    private static final int CONNECT_TIMEOUT = 2500;
    private static final int READ_TIMEOUT = 7000;
    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;

    private static final List<String> REFERER_POOL = Arrays.asList(
            "https://www.baidu.com/",
            "https://www.google.com/",
            "https://bing.com/",
            "https://www.so.com/",
            ""
    );

    // 生产环境 50 并发稳定浏览器指纹
    private static final HttpHeaders BROWSER_HEADERS;
    static {
        BROWSER_HEADERS = new HttpHeaders();
        BROWSER_HEADERS.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
        BROWSER_HEADERS.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        BROWSER_HEADERS.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        BROWSER_HEADERS.set("Accept-Encoding", "gzip, deflate, br, zstd");
        BROWSER_HEADERS.set("Upgrade-Insecure-Requests", "1");
        BROWSER_HEADERS.set("Sec-Fetch-Dest", "document");
        BROWSER_HEADERS.set("Sec-Fetch-Mode", "navigate");
        BROWSER_HEADERS.set("Sec-Fetch-Site", "cross-site");
        BROWSER_HEADERS.set("Sec-Fetch-User", "?1");
        BROWSER_HEADERS.set("DNT", "1");
    }

    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;
    private SSLContext sslContext;

    @PostConstruct
    public void init() {
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(300);
            connectionManager.setDefaultMaxPerRoute(100);
            connectionManager.setValidateAfterInactivity(1000);

            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .disableAutomaticRetries()
                    .evictIdleConnections(10, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            log.error("初始化失败", e);
        }
    }

    public RestTemplate createWebClient(String proxyHost, int proxyPort) {
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
        // 解决重复请求、卡顿、代理异常！
        factory.setBufferRequestBody(false);

        factory.setHttpContextFactory((httpMethod, uri) -> {
            HttpClientContext ctx = HttpClientContext.create();
            ctx.setRequestConfig(config);
            return ctx;
        });

        return new RestTemplate(factory);
    }

    private byte[] doRequest(RestTemplate restTemplate, String requestUrl, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(BROWSER_HEADERS);

//            // 随机 Referer，更自然
//            String ref = REFERER_POOL.get(random.nextInt(REFERER_POOL.size()));
//            if (!ref.isEmpty()) {
//                headers.set("Referer", ref);
//            }

            if (StringUtils.isNotBlank(token)) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, byte[].class);
            return resp.getBody();
        } catch (Throwable e) {
            throw new IllegalArgumentException("request_failed");
        }
    }

    private String safeDecompress(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";
        }

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

    public String doProxyRequest(RestTemplate restTemplate, String requestUrl, String token) {
        return safeDecompress(doRequest(restTemplate, requestUrl, token));
    }
}