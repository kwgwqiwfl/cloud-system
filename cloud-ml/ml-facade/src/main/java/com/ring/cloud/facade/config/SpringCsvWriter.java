package com.ring.cloud.facade.config;

import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * csv工具类
 */
@Slf4j
@Component
public class SpringCsvWriter {
    // 存储文件路径-写入器映射
    private final Map<String, BufferedWriter> writerMap = new HashMap<>();
    private final Map<String, Boolean> fileOpenFlag = new HashMap<>();
    private final Object mapLock = new Object();

    // 文件写入器
    public BufferedWriter getWriter(String csvPath) throws IOException {
        synchronized (mapLock) {
            if (!writerMap.containsKey(csvPath)) {
                File csvFile = new File(csvPath);
                OutputStreamWriter osw = new OutputStreamWriter(
                        new FileOutputStream(csvFile, false), StandardCharsets.UTF_8);//第一次打开覆盖模式
                BufferedWriter bw = new BufferedWriter(osw, FileUtil.BUFFER_SIZE);
                //添加 UTF-8 BOM 头（Excel 专属）
                bw.write('\ufeff'); //添加 UTF-8 BOM 头（Excel 专属） 仅在文件覆盖时加一次，追加模式不要重复加！
                writerMap.put(csvPath, bw);
                fileOpenFlag.put(csvPath, true);
            }
            return writerMap.get(csvPath);
        }
    }

    // ========== 手动关闭单个文件（子线程执行完调用） ==========
    public void closeFile(String csvPath) {
        synchronized (mapLock) {
            if (fileOpenFlag.getOrDefault(csvPath, false)) {
                try {
                    BufferedWriter bw = writerMap.get(csvPath);
                    bw.flush(); // 强制刷出缓冲区所有数据（避免丢失）
                    bw.close(); // 关闭流，释放文件句柄
                    writerMap.remove(csvPath); // 移除映射，避免重复操作
                    fileOpenFlag.put(csvPath, false);
                    log.info("文件已关闭：" + csvPath);
                } catch (Throwable e) {
                    log.error("关闭文件失败：{}，{}",csvPath, e.getMessage());
                }
            }
        }
    }

    // Spring销毁时自动关闭所有文件（无需手动调用）
    @PreDestroy
    public void closeAll() {
        synchronized (mapLock) {
            for (String csvPath : writerMap.keySet()) {
                try {
                    BufferedWriter bw = writerMap.get(csvPath);
                    bw.flush();
                    bw.close();
                } catch (Exception e) {
                    System.err.println("关闭文件失败：" + csvPath);
                }
            }
            writerMap.clear();
            fileOpenFlag.clear();
        }
    }

    // 处理CSV特殊字符
    private String escape(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

}
