package com.ring.cloud.facade.test;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.util.IpUtil;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 独立测试工具
 */
public class OkPangTest {

    private static final OkHttpClient CLIENT = createClient();

    public static void main(String[] args) {
        String url = IpUtil.buildPangUrl("3.1.1.0", "https://chapangzhan.com");
        try {
//            url = "http://httpbin.org/ip";
            List<ProxyIp> plist = RestTest.doGetProxyIpList();
            ProxyIp p1 = plist.get(0);


            System.out.println("=== 代理 ==="+ JSON.toJSONString(p1));
            String html1 = get(url, p1.getIp(), p1.getPort());
            System.out.println("html1 = " + html1);

        } catch (Exception e) {
            System.err.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 核心GET请求 ====================
    private static String get(String url, String proxyHost, int proxyPort) throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        Headers BROWSER_HEADERS = Headers.of(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8",
//                "Accept-Encoding", "gzip, deflate, br",
                "Accept-Encoding", "gzip",//继续优化空间共3项，第1项
                "Connection", "keep-alive",
                "Upgrade-Insecure-Requests", "1",
                "Sec-Fetch-Dest", "document",
                "Sec-Fetch-Mode", "navigate",
                "Sec-Fetch-Site", "none",
                "Sec-Fetch-User", "?1",
                "DNT", "1",
                "Cache-Control", "max-age=0",
                "sec-ch-ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"",
                "sec-ch-ua-mobile", "?0",
                "sec-ch-ua-platform", "\"Windows\""
//                "Accept-CH", "Sec-CH-UA, Sec-CH-UA-Mobile, Sec-CH-UA-Platform"//继续优化空间共3项，第2项
        );

        Request.Builder builder = new Request.Builder()
                .url(url)
                .headers(BROWSER_HEADERS);
        if (url.contains("chapangzhan.com")) {
            builder.addHeader("Referer", "https://chapangzhan.com/");//继续优化空间共3项，第3项
        }
        Request request = builder.build();
        try (Response response = CLIENT.newBuilder().proxy(proxy).build().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("请求失败：" + response.code());
            System.out.println("=== response ==="+ JSON.toJSONString(response));
            byte[] body = response.body() != null ? response.body().bytes() : new byte[0];
            return safeDecompress(body);
        }
    }
    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;
    private static String safeDecompress(byte[] compressedBytes) {
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

    // ==================== OkHttp 信任所有SSL ====================
    private static OkHttpClient createClient() {
        try {
            SSLContext ssl = SSLContext.getInstance("TLS");
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                @Override public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                @Override public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {return new java.security.cert.X509Certificate[0];}
            }};
            ssl.init(null, trustAll, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .sslSocketFactory(ssl.getSocketFactory(), (X509TrustManager) trustAll[0])
                    .hostnameVerifier((h, s) -> true)
                    .proxySelector(new ProxySelector() {
                        @Override public List<Proxy> select(URI uri) {return Collections.singletonList(Proxy.NO_PROXY);}
                        @Override public void connectFailed(URI uri, SocketAddress sa, java.io.IOException ioe) {}
                    })
            .followRedirects(true)    // 必须开
                    .followSslRedirects(true) // 必须开
                    .retryOnConnectionFailure(false)
                    .cookieJar(CookieJar.NO_COOKIES)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}