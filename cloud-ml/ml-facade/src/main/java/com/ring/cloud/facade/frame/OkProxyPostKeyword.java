package com.ring.cloud.facade.frame;

import com.ring.cloud.facade.util.HttpHeaderUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkProxyPostKeyword extends OkProxyBase {

    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 8500;

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

            OkHttpClient.Builder baseBuilder = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectionPool(new ConnectionPool(300, 10, TimeUnit.SECONDS))
                    .proxySelector(new DynamicProxySelector())
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(false)
                    .cookieJar(CookieJar.NO_COOKIES);

            okHttpClient = baseBuilder.build();

        } catch (Exception e) {
            log.error("OkHttp POST 初始化失败", e);
        }
    }

    public String doProxyPostRequest(String proxyHost, int proxyPort, String requestUrl, String jsonBody, String token) {
        try {
            if (StringUtils.isNotBlank(proxyHost) && proxyPort > 0) {
                PROXY_THREAD_LOCAL.set(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            }

            Request.Builder builder = new Request.Builder().url(requestUrl);
            setSiteHeaders(builder, requestUrl);

            if (StringUtils.isNotBlank(token)) {
                builder.header("Authorization", "Bearer " + token);
            }

            // 正确顺序 OkHttp3
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);
            builder.post(body);

            Request request = builder.build();
            smartSleep(requestUrl);

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("request_failed");
                }
                byte[] bodyBytes = response.body() != null ? response.body().bytes() : new byte[0];
                String encoding = response.header("Content-Encoding", "");
                Charset charset = StandardCharsets.UTF_8;
                return keywordDecompress(bodyBytes, encoding, charset);
            }

        } catch (Exception e) {
            log.debug("POST代理请求失败 url:{}", requestUrl, e);
            throw new IllegalArgumentException("request_failed", e);
        } finally {
            PROXY_THREAD_LOCAL.remove();
        }
    }

    private void setSiteHeaders(Request.Builder builder, String url) {
        builder.headers(HttpHeaderUtils.COMMON);
        if (url.contains("qbbusi.html5.qq.com")) {
            builder.headers(HttpHeaderUtils.SOGOU);
        }
    }

    private static void smartSleep(String url) {
        try {
            Thread.sleep(60 + ThreadLocalRandom.current().nextInt(90));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}