package com.ring.cloud.facade.frame;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * 代理rest
 */
@Slf4j
@Component
public class RestProxyBase {
    // ========== 完全保留你原来的配置 ==========
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

    // 初始化 SSL（忽略证书，和你原来完全一样）
    @PostConstruct
    public void init() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {return new java.security.cert.X509Certificate[0];}
            }}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            log.error("SSL初始化失败", e);
        }
    }

    // ========== 保留方法名、参数、返回值 100% 兼容 ==========
    public RestTemplate createWebClient(String proxyHost, int proxyPort) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 你的超时：2500~3500ms
        factory.setConnectTimeout(2500 + random.nextInt(1001));
        // 读超时 8~10s
        factory.setReadTimeout((8 + random.nextInt(3)) * 1000);

        // 代理设置
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        factory.setProxy(proxy);

        return new RestTemplate(factory);
    }

    // ========== 核心请求：100% 等价原来逻辑 ==========
    private byte[] doRequest(RestTemplate restTemplate, String requestUrl, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", UA_POOL.get(random.nextInt(UA_POOL.size())));
            headers.set("Accept", ACCEPT_POOL.get(random.nextInt(ACCEPT_POOL.size())));
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Encoding", "gzip, deflate");
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

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

    // ========== GZIP 解压：完全不变 ==========
    private String safeDecompress(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) return "";
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

    // ========== 对外入口：完全不变 ==========
    public String doProxyRequest(RestTemplate restTemplate, String requestUrl, String token) {
        return safeDecompress(doRequest(restTemplate, requestUrl, token));
    }

}
