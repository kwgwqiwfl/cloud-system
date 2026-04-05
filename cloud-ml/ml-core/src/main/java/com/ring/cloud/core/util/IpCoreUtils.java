package com.ring.cloud.core.util;

public class IpCoreUtils {
    /**
     * 把 192.168.1.1 转成 long 数字
     * 全项目统一用这一个方法！！！
     */
    public static long ipToLong(String ip) {
        String[] arr = ip.split("\\.");
        long p0 = Long.parseLong(arr[0]);
        long p1 = Long.parseLong(arr[1]);
        long p2 = Long.parseLong(arr[2]);
        long p3 = Long.parseLong(arr[3]);
        return (p0 << 24) | (p1 << 16) | (p2 << 8) | p3;
    }

    public static int crc32(String str) {
        if (str == null) return 0;
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(str.getBytes());
        return (int) crc32.getValue();
    }
}
