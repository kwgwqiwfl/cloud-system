package com.ring.cloud.facade.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReadLastLineOfAllFiles {

    public static void main(String[] args) {
        // 改成你的文件目录
        String dirPath = "E:\\ml_cloud开发\\tmp文件\\0404";

        try {
            Files.list(Paths.get(dirPath))
                    .filter(Files::isRegularFile)
                    .forEach(ReadLastLineOfAllFiles::processFile);
        } catch (IOException e) {
            System.err.println("遍历目录异常: " + e.getMessage());
        }
    }

    private static void processFile(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                System.out.println(file.getFileName() + " => 文件为空");
                return;
            }

            // 1. 获取最后一行
            String lastLine = lines.get(lines.size() - 1).trim();
            String[] parts = lastLine.split(",");

            if (parts.length < 2) {
                System.out.println(file.getFileName() + " => 格式不正确");
                return;
            }

            // 2. 取出第二列 IP
            String originalIp = parts[1].trim();
            // 3. 处理 IP
            String newIp = processIpAddress(originalIp);

//            System.out.println("文件：" + file.getFileName());
//            System.out.println("原始IP：" + originalIp);
//            System.out.println("处理后IP：" + newIp);
//            System.out.println("--------------------------------------------------");
            System.out.println(newIp);

        } catch (Exception e) {
            System.err.println("处理文件失败：" + file.getFileName() + " => " + e.getMessage());
        }
    }

    /**
     * IP 处理核心方法
     * 规则：
     * 1. 最后一段 置为 0
     * 2. 第三段 +1
     * 3. 第三段=255 → 置0，第二段+1
     */
    private static String processIpAddress(String ip) {
        try {
            String[] segments = ip.split("\\.");
            if (segments.length != 4) return ip;

            int p1 = Integer.parseInt(segments[0]);
            int p2 = Integer.parseInt(segments[1]);
            int p3 = Integer.parseInt(segments[2]);
            int p4 = Integer.parseInt(segments[3]);

            // 最后一段直接变0
            p4 = 0;

            // 第三段 +1，处理 255 进位
            p3 += 1;
            if (p3 > 255) {
                p3 = 0;
                p2 += 1;
            }

            return p1 + "." + p2 + "." + p3 + "." + p4;
        } catch (Exception e) {
            return ip;
        }
    }
}