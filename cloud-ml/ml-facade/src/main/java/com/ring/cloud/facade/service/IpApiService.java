package com.ring.cloud.facade.service;

import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.facade.entity.api.DomainResult;
import com.ring.cloud.facade.support.IpApiClient;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class IpApiService extends SeaCommon {

    @Autowired
    private IpApiClient ipApiClient;

    @Value("${ml.client.apiip.file.path:/}")
    private String apiFilePath;

    // 常量
    private static final int BATCH_WRITE_SIZE = 1000;//写入条数
    private static final int PAGE_LIMIT = 100;//每页数据量
    private static final int RETRY_DELAY_MS = 300;//重试间隔
    private static final int MAX_RETRY = 2;//网络波动重试3次

    /**
     * 分页查询IP反查域名
     */
    public String queryDomainByIp(String ip, int startPage, int endPage) {
        String timestamp = DateUtil.fileSuffixSDF.format(new Date());
        String baseFileName = ip + "-" + timestamp;

        String finalFilePath = apiFilePath + File.separator + baseFileName + ".csv";
        String tmpFilePath = apiFilePath + File.separator + baseFileName + ".tmp";

        List<DomainResult> cacheList = new ArrayList<>(BATCH_WRITE_SIZE);
        BufferedWriter writer = null;

        log.info("开始处理IP={}，页码从{}到{}，临时文件：{}", ip, startPage, endPage, tmpFilePath);

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tmpFilePath), StandardCharsets.UTF_8)
            );

            // 写入表头
            writer.write("ip,domain,adtime,uptime");
            writer.newLine();

            // 分页查询
            for (int currentPage = startPage; currentPage <= endPage; currentPage++) {
                List<DomainResult> pageResult = null;

                // 查询重试机制
                for (int retry = 0; retry <= MAX_RETRY; retry++) {
                    try {
                        log.debug("查询IP={} 第{}页", ip, currentPage);
                        pageResult = ipApiClient.queryDomainByIp(ip, currentPage);
                        break;
                    } catch (Exception e) {
                        if (retry == MAX_RETRY) {
                            log.error("IP={} 第{}页重试失败，终止任务", ip, currentPage, e);
                            throw e;
                        }
                        log.warn("IP={} 第{}页查询失败，{}ms后重试", ip, currentPage, RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    }
                }

                // ====================== 修复开始 ======================
                // 先把当前页数据加入缓存
                if (pageResult != null && !pageResult.isEmpty()) {
                    cacheList.addAll(pageResult);
                }

                // 不足100条直接终止
                if (pageResult == null || pageResult.size() < PAGE_LIMIT) {
                    log.info("IP={} 第{}页数据不足{}条，终止查询", ip, currentPage, PAGE_LIMIT);
                    break;
                }
                // ====================== 修复结束 ======================

                // 满1000条批量写入
                if (cacheList.size() >= BATCH_WRITE_SIZE) {
                    writeBatch(writer, ip, cacheList);
                    cacheList.clear();
                }
            }

            // 写入剩余数据
            if (!cacheList.isEmpty()) {
                writeBatch(writer, ip, cacheList);
            }

            // 刷入磁盘
            writer.flush();

            // 关闭流再改名
            if (writer != null) {
                writer.close();
                writer = null;
            }

            // 临时文件改名
            FileUtil.renameTmpToFile(tmpFilePath, finalFilePath);
            log.info("处理完成，文件已生成：{}", finalFilePath);

        } catch (Exception e) {
            log.error("IP={} 处理异常", ip, e);
            FileUtil.deleteTmpFile(tmpFilePath);
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            // 安全关闭流
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
        return finalFilePath;
    }

    /**
     * 批量写入行（逗号分隔）
     */
    private void writeBatch(BufferedWriter writer, String ip, List<DomainResult> list) throws IOException {
        if (list == null || list.isEmpty()) return;

        for (DomainResult res : list) {
            String line = new StringBuilder()
                    .append(ip).append(",")
                    .append(res.getDomain() == null ? "" : res.getDomain()).append(",")
                    .append(res.getAddtime() == null ? "" : res.getAddtime()).append(",")
                    .append(res.getUptime() == null ? "" : res.getUptime())
                    .toString();

            writer.write(line);
            writer.newLine();
        }
    }

    /**
     * 查询IP归属地并写入单列CSV文件
     * 文件名：ip-loc-时间戳.csv
     * 文件格式：仅一列，每行一个data数据
     */
    public String ipdata(String ip) {
        String timestamp = DateUtil.fileSuffixSDF.format(new Date());
        // 文件名格式：ip-loc-时间戳.csv
        String fileName = ip + "-loc-" + timestamp + ".csv";
        String filePath = apiFilePath + File.separator + fileName;

        BufferedWriter writer = null;
        try {
            // 调用接口获取数据
            List<String> dataList = ipApiClient.ipdata(ip);
            if (dataList == null || dataList.isEmpty()) {
                log.warn("IP={} 查询归属地数据为空", ip);
                throw new IllegalArgumentException("查询归属地数据为空");
            }

            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)
            );

            // 逐行写入文件（单列，无表头）
            for (String data : dataList) {
                // 过滤 null 和空字符串，不写入
                if (data == null || data.trim().isEmpty()) {
                    continue;
                }
                writer.write(data);
                writer.newLine();
            }

            writer.flush();
            log.info("IP归属地文件写入完成：{}", filePath);

        } catch (Exception e) {
            log.error("IP={} 查询或写入归属地文件失败", ip, e);
            throw new IllegalArgumentException("查询失败，"+e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
        return filePath;
    }
}