package com.ring.cloud.facade.frame;

import com.ring.cloud.facade.util.HttpHeaderUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.brotli.dec.BrotliInputStream;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Slf4j
@Component
public class OkProxyKeyword extends OkProxyBase {

    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 8500;
    private static final int YANDEX_READ_TIMEOUT = 20000;
    private static final long MAX_GZIP_SIZE = 10 * 1024 * 1024;

    private OkHttpClient okHttpClient;
    private OkHttpClient yandexHttpClient;
    private static final ThreadLocal<Proxy> PROXY_THREAD_LOCAL = new ThreadLocal<>();

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

            // 通用客户端配置
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

            // 普通客户端（默认8.5秒超时）
            okHttpClient = baseBuilder.build();

            // Yandex专用客户端（单独20秒超时，重新构建配置）
            OkHttpClient.Builder yandexBuilder = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(YANDEX_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(YANDEX_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectionPool(new ConnectionPool(300, 10, TimeUnit.SECONDS))
                    .proxySelector(new DynamicProxySelector())
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(false)
                    .cookieJar(CookieJar.NO_COOKIES);

            yandexHttpClient = yandexBuilder.build();

        } catch (Exception e) {
            log.error("OkHttp 初始化失败", e);
        }
    }

    public String doProxyRequest(String proxyHost, int proxyPort, String requestUrl, String token) {
        try {
            if (StringUtils.isNotBlank(proxyHost) && proxyPort > 0) {
                PROXY_THREAD_LOCAL.set(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            }

            Request.Builder builder = new Request.Builder().url(requestUrl);
            setSiteHeaders(builder, requestUrl);

            if (StringUtils.isNotBlank(token)) {
                builder.header("Authorization", "Bearer " + token);
            }

            Request request = builder.build();
            smartSleep(requestUrl);

            OkHttpClient client = requestUrl.contains("yandex.com") ? yandexHttpClient : okHttpClient;

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("request_failed");
                }
                byte[] body = response.body() != null ? response.body().bytes() : new byte[0];
                String encoding = response.header("Content-Encoding", "");
                Charset charset = getCharsetByUrl(requestUrl);
                return safeDecompress(body, encoding, charset);
            }

        } catch (Exception e) {
            log.debug("代理请求失败 url:{}", requestUrl, e);
            throw new IllegalArgumentException("request_failed", e);
        } finally {
            PROXY_THREAD_LOCAL.remove();
        }
    }

    // ======================
    // 从工具类取头，代码极干净
    // ======================
    private void setSiteHeaders(Request.Builder builder, String url) {
        builder.headers(HttpHeaderUtils.COMMON);
        if (url.contains("baidu.com")) {
            builder.headers(HttpHeaderUtils.BAIDU);
        } else if (url.contains("sug.so.360.cn")) {
            builder.headers(HttpHeaderUtils.SO_COM);
        } else if (url.contains("cn.bing.com")) {
            builder.headers(HttpHeaderUtils.BING);
        } else if (url.contains("yandex.com")) {
            builder.headers(HttpHeaderUtils.YANDEX);
        } else if (url.contains("suggestqueries.google.com")) {
            builder.headers(HttpHeaderUtils.GOOGLE);
        }
    }

    // ======================
    // 解压
    // ======================
    protected String safeDecompress(byte[] compressedBytes, String encoding, Charset charset) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";
        }
        if (StringUtils.isBlank(encoding)) {
            return new String(compressedBytes, charset);
        }

        InputStream inputStream = null;
        try {
            String enc = encoding.toLowerCase().trim();
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);

            if ("gzip".equals(enc)) {
                inputStream = new GZIPInputStream(bais);
            } else if ("deflate".equals(enc)) {
                inputStream = new InflaterInputStream(bais);
            } else if ("br".equals(enc)) {
                inputStream = new BrotliInputStream(bais);
            } else {
                return new String(compressedBytes, charset);
            }

            byte[] buffer = new byte[4096];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            long total = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                total += len;
                if (total > MAX_GZIP_SIZE) {
                    throw new IllegalArgumentException("decompress_failed");
                }
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), charset);
        } catch (Exception e) {
            return new String(compressedBytes, charset);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static class DynamicProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            Proxy proxy = PROXY_THREAD_LOCAL.get();
            return proxy != null ? Arrays.asList(proxy) : Arrays.asList(Proxy.NO_PROXY);
        }
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}
    }

    private static void smartSleep(String url) {
        try {
            Thread.sleep(30 + ThreadLocalRandom.current().nextInt(70));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Charset getCharsetByUrl(String url) {
        if (url.contains("suggestion.baidu.com")) {
            return Charset.forName("GBK");
        }
        return StandardCharsets.UTF_8;
    }
}