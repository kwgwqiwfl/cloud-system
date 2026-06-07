package com.ring.cloud.facade.frame;

import com.ring.cloud.facade.util.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkHttpSimpleClient {

    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 8500;

    private static final Headers BROWSER_HEADERS;
    static {
        BROWSER_HEADERS = Headers.of(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8",
                "Accept-Encoding", "gzip, deflate, br",
                "Connection", "close",
                "Upgrade-Insecure-Requests", "1",
                "Sec-Fetch-Dest", "document",
                "Sec-Fetch-Mode", "navigate",
                "Sec-Fetch-Site", "none",
                "Sec-Fetch-User", "?1",
                "DNT", "1",
                "Cache-Control", "max-age=0",
                "sec-ch-ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"",
                "sec-ch-ua-mobile", "?0",
                "sec-ch-ua-platform", "\"Windows\"",
                "Accept-CH", "Sec-CH-UA, Sec-CH-UA-Mobile, Sec-CH-UA-Platform"
        );
    }

    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };
            sslContext.init(null, trustAllCerts, new SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectionPool(new ConnectionPool(300, 10, TimeUnit.SECONDS))
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(false)
                    .cookieJar(CookieJar.NO_COOKIES);

            okHttpClient = builder.build();

        } catch (Exception e) {
            log.error("OkHttp 初始化失败", e);
        }
    }

    /**
     * 纯GET请求，无任何代理逻辑
     */
    public String doGetRequest(String requestUrl, String token) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(requestUrl)
                    .headers(BROWSER_HEADERS);

            if (StringUtils.isNotBlank(token)) {
                builder.header("Authorization", "Bearer " + token);
            }
            Request request = builder.build();

            smartSleep(requestUrl);

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("request_failed");
                }
                byte[] body = response.body() != null ? response.body().bytes() : new byte[0];
                return HttpUtils.safeDecompress(body);
            }

        } catch (Exception e) {
            log.debug("请求失败 url:{}", requestUrl, e);
            throw new IllegalArgumentException("request_failed", e);
        }
    }

    private static void smartSleep(String url) {
        try {
            if (url.contains("......com")) {
                Thread.sleep(20 + ThreadLocalRandom.current().nextInt(40));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}