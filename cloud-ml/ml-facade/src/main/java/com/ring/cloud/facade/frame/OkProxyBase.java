package com.ring.cloud.facade.frame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class OkProxyBase {

    private static final int MAX_GZIP_SIZE = 10 * 1024 * 1024;

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
}
