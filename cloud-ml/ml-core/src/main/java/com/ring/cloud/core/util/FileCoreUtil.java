package com.ring.cloud.core.util;

import com.ring.cloud.core.pojo.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileCoreUtil {
    // 一批 10000 条
    private static final int BATCH_SIZE = 10000;
    // BufferedWriter 缓冲区 256KB
    private static final int WRITER_BUF = 1024 * 256;
    // 预估每行120字符，预分配避免扩容
    private static final int SB_INIT_CAP = BATCH_SIZE * 120;

    // 全局唯一日期格式化
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
    // 批量写入
    public static void writeBatch(BufferedWriter writer, List<String> batch) throws Exception {
        if (writer == null || batch.isEmpty()) return;
        for (String line : batch) {
            writer.write(line);
            writer.newLine();
        }
        batch.clear();
    }
    /**
     * 流式写入 CSV → UTF-8 (无BOM) → 不OOM → 自动关流
     */

    public static void write(String filePath, String[] headers, List<?> dataList) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8),
                WRITER_BUF
        )) {
            // 写表头
            bw.write(String.join(",", headers));
            bw.newLine();

            StringBuilder batchSb = new StringBuilder(SB_INIT_CAP);
            int count = 0;

            for (Object obj : dataList) {
                batchSb.append(buildLine(obj)).append("\r\n");
                count++;

                // 满1万条：一次性写入 + 强制落盘
                if (count >= BATCH_SIZE) {
                    bw.write(batchSb.toString());
                    bw.flush();
                    batchSb.setLength(0);
                    count = 0;
                }
            }

            // 剩余收尾
            if (batchSb.length() > 0) {
                bw.write(batchSb.toString());
                bw.flush();
            }
        }
    }

    // 临时文件重命名为正式文件
    public static void renameToFinal(File tmpFile, String inputDomain, File exportDir) {
        if (tmpFile == null || !tmpFile.exists()) {
            throw new RuntimeException("临时文件不存在，重命名失败");
        }

        File finalFile = new File(exportDir, inputDomain + ".csv");

        // 删除旧文件
        if (finalFile.exists()) {
            if (!finalFile.delete()) {
                throw new RuntimeException("旧正式文件删除失败：" + finalFile);
            }
        }

        // 👇 关键：重命名失败必须抛出异常，不能静默失败
        if (!tmpFile.renameTo(finalFile)) {
            throw new RuntimeException("临时文件重命名失败：" + tmpFile + " -> " + finalFile);
        }
    }


    public static String buildLine(Object obj) {
        if (obj instanceof MlDomain) {
            MlDomain o = (MlDomain) obj;
            return o.getDomains() + "," + fmt(o.getCreateTime()) + "," + fmt(o.getUpdateTime()) + "," + o.getQueryCount();
        }
        if (obj instanceof MlIp) {
            MlIp o = (MlIp) obj;
            return o.getIp() + "," + fmt(o.getCreateTime()) + "," + fmt(o.getUpdateTime()) + "," + o.getQueryCount();
        }
        if (obj instanceof MlIcp) {
            MlIcp o = (MlIcp) obj;
            return o.getDomains() + "," + fmt(o.getCreateTime()) + "," + fmt(o.getUpdateTime()) + "," + o.getQueryCount();
        }
        if (obj instanceof MlSubdomain) {
            MlSubdomain o = (MlSubdomain) obj;
            return o.getDomains() + "," + fmt(o.getCreateTime()) + "," + fmt(o.getUpdateTime()) + "," + o.getQueryCount();
        }
        if (obj instanceof MlDomainAi) {
            MlDomainAi o = (MlDomainAi) obj;
            return o.getDomains() + "," + fmt(o.getAdTime()) + "," + fmt(o.getUpTime()) + "," + o.getTotalCount() + "," + o.getDayCount();
        }
        if (obj instanceof MixIpDomain) {
            MixIpDomain o = (MixIpDomain) obj;
            return o.getIp() + "," + o.getLoc() + "," + o.getDomains() + "," + fmt(o.getAdtime()) + "," + fmt(o.getUptime());
        }
        if (obj instanceof MixDomainIp) {
            MixDomainIp o = (MixDomainIp) obj;
            return o.getIp() + "," + o.getDomains() + "," + fmt(o.getAdtime()) + "," + fmt(o.getUptime());
        }
        return "";
    }

    private static String fmt(Date date) {
        return date == null ? "" : SDF.format(date);
    }
}
