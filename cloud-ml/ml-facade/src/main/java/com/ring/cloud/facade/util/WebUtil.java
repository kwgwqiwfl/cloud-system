package com.ring.cloud.facade.util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

public class WebUtil {

    //解压缩
    public static String decompressGzip(byte[] compressedBytes) {
        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";
        }
        try (InputStream gzipIn = new GZIPInputStream(new java.io.ByteArrayInputStream(compressedBytes));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024]; // 1KB缓冲区，适配JDK8
            int len;
            // 循环读取（替代JDK9+的readAllBytes）
            while ((len = gzipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name()); // JDK8推荐写法
        } catch (Exception e) {
            // 未压缩/解压失败，直接转字符串
            return new String(compressedBytes, StandardCharsets.UTF_8);
        }
    }

    public static WebClient getClient(String url) {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);
        // HtmlUnit 模拟浏览器
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(10 * 1000);
        return webClient;
    }
    public static void release(WebClient webClient) {
        webClient.close();
    }
}
