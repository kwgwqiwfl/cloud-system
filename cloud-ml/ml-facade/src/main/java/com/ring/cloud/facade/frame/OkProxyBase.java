package com.ring.cloud.facade.frame;

import org.apache.commons.lang3.StringUtils;
import org.brotli.dec.BrotliInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class OkProxyBase {

    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;

    // ======================
    // 全局公用 ！！！
    // ======================
    protected static final ThreadLocal<Proxy> PROXY_THREAD_LOCAL = new ThreadLocal<>();

    protected static class DynamicProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            Proxy proxy = PROXY_THREAD_LOCAL.get();
            return proxy != null ? Arrays.asList(proxy) : Arrays.asList(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }

    protected String safeDecompress(byte[] compressedBytes) {
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

    // ======================
    // keyword解压
    // ======================
    protected String keywordDecompress(byte[] compressedBytes, String encoding, Charset charset) {
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
}
