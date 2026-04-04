package com.ring.cloud.core.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    public static final int BATCH_SIZE = 1000;
    // 全局静态，线程安全，支持两种格式：yyyy-MM-dd 和 yyyyMMdd
    private static final DateTimeFormatter FMT1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT2 = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static Date today() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static boolean isDifferentMonth(Date date) {
        if (date == null) {
            return true; // 空=不同月，直接插入
        }
        YearMonth now = YearMonth.now(ZoneId.systemDefault());
        YearMonth updateMonth = YearMonth.from(
                date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );
        return !now.equals(updateMonth);
    }

    public static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }

        String val = s.trim();
        try {
            // 第一种格式：2024-04-06
            return Date.from(LocalDate.parse(val, FMT1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {}

        try {
            // 第二种格式：20240406 ← 你文件里的坑
            return Date.from(LocalDate.parse(val, FMT2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {}

        // 都解析失败才返回 null
        return null;
    }

}
