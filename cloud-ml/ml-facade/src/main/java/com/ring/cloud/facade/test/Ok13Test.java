package com.ring.cloud.facade.test;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyBase;
import com.ring.cloud.facade.util.IpUtil;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 独立测试工具
 * 验证：切换代理后，Token 是否可以继续使用
 * 无Spring、无注入、直接运行
 */
public class Ok13Test {

    // ====================== 【你只需要配置这里】 ======================
    private static final String TEST_IP = "36.156.212.181";
    private static final String FIRST_URL = "https://site.ip138.com/" + TEST_IP;
    //生成ip domain url 分页查询 url="https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=2&token=2ea08c94ef895a05b7df3182717f8dc2"
    private static final String PU = "https://site.ip138.com/index/querybyip/?ip="+TEST_IP;
    private static final OkHttpClient CLIENT = createClient();

    public static void main(String[] args) {
        try {
            List<ProxyIp> plist = RestTest.doGetProxyIpList();
            ProxyIp p1 = plist.get(0);

            System.out.println("=== 1. 代理1 请求首页 ===");
            String html1 = get(FIRST_URL, p1.getIp(), p1.getPort());
            System.out.println("html1 = " + html1);
            String token = extractToken(html1);
            System.out.println("Token = " + token);
            Thread.sleep(1000);
            String page2Url = PU+"&page=500&token="+token;
            System.out.println("url200="+page2Url);
            String page2 = get(page2Url, p1.getIp(), p1.getPort());
            System.out.println("\n=== 验证结果 ==="+page2);



        } catch (Exception e) {
            System.err.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 核心GET请求 ====================
    private static String get(String url, String proxyHost, int proxyPort) throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        Headers BROWSER_HEADERS = Headers.of(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8",
                "Accept-Encoding", "gzip, deflate, br",
                "Connection", "keep-alive",
                "Upgrade-Insecure-Requests", "1",
                "Sec-Fetch-Dest", "document",
                "Sec-Fetch-Mode", "navigate",
                "Sec-Fetch-Site", "same-origin",
                "Sec-Fetch-User", "?1",
                "DNT", "1",
                "Cache-Control", "max-age=0"
        );
        Request.Builder builder = new Request.Builder()
                .url(url)
                .headers(BROWSER_HEADERS);
//            builder.addHeader("Referer", "https://site.ip138.com/");
        Request request = builder.build();

        try (Response response = CLIENT.newBuilder().proxy(proxy).build().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("请求失败：" + response.code());
            byte[] body = response.body() != null ? response.body().bytes() : new byte[0];
            return safeDecompress(body);
        }
    }

    // ==================== 提取Token（和你项目一致） ====================
    private static String extractToken(String html) {
        Document doc = Jsoup.parse(html);
        Pattern pattern = Pattern.compile("var _TOKEN = '(.*?)';");
        for (Element script : doc.select("script")) {
            Matcher m = pattern.matcher(script.html());
            if (m.find()) return m.group(1);
        }
        return null;
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
                    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                    .sslSocketFactory(ssl.getSocketFactory(), (X509TrustManager) trustAll[0])
                    .hostnameVerifier((h, s) -> true)
                    .proxySelector(new ProxySelector() {
                        @Override public List<Proxy> select(URI uri) {return Collections.singletonList(Proxy.NO_PROXY);}
                        @Override public void connectFailed(URI uri, SocketAddress sa, java.io.IOException ioe) {}
                    }).followRedirects(true)    // 必须开
                    .followSslRedirects(true) // 必须开
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;
    private static String safeDecompress(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) return "";

        boolean isGzip = compressedBytes.length >= 2 &&
                (compressedBytes[0] & 0xFF) == 0x1F &&
                (compressedBytes[1] & 0xFF) == 0x8B;

        if (!isGzip) return new String(compressedBytes, StandardCharsets.UTF_8);

        try (InputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            long total = 0;

            while ((len = gzipIn.read(buffer)) != -1) {
                total += len;
                if (total > MAX_GZIP_SIZE) throw new IllegalArgumentException("decompress_failed");
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(compressedBytes, StandardCharsets.UTF_8);
        }
    }
}