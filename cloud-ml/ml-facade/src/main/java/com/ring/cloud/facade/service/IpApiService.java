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
     *
     */
    public String queryDomainByIp(String ip, String endAddTime) {
        String timestamp = DateUtil.fileSuffixSDF.format(new Date());
        String baseFileName = timestamp+"_"+ip + "_" + endAddTime;

        String finalFilePath = apiFilePath + File.separator + baseFileName + ".csv";
        String tmpFilePath = apiFilePath + File.separator + baseFileName + ".tmp";

        List<DomainResult> cacheList = new ArrayList<>(BATCH_WRITE_SIZE);
        BufferedWriter writer = null;

        log.info("开始处理IP={}，addtime从当前时间向前到{}，临时文件：{}", ip, endAddTime, tmpFilePath);

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tmpFilePath), StandardCharsets.UTF_8)
            );

            writer.write("ip,domain,adtime,uptime");
            writer.newLine();

            // 分页查询 —— 【绝对安全，不会死循环】
            for (int currentPage = 1; currentPage < 30000; currentPage++) {
                List<DomainResult> pageResult = null;
                boolean querySuccess = false;

                // 重试
                for (int retry = 0; retry <= MAX_RETRY; retry++) {
                    try {
                        log.debug("查询IP={} 第{}页", ip, currentPage);
                        pageResult = ipApiClient.queryDomainByIp(ip, currentPage);
                        querySuccess = true;
                        break;
                    } catch (Exception e) {
                        if (retry == MAX_RETRY) {
                            log.error("IP={} 第{}页达到最大重试次数，停止查询", ip, currentPage, e);
                            throw new IllegalArgumentException("第"+currentPage+"页重试3次查询失败！");
                        }
                        log.warn("IP={} 第{}页查询失败，{}ms后重试", ip, currentPage, RETRY_DELAY_MS);
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!querySuccess) {
                    log.error("IP={} 第{}页查询失败，终止", ip, currentPage);
                    break;
                }

                if (pageResult == null || pageResult.isEmpty()) {
                    log.warn("IP={} 第{}页返回数据未空，终止查询", ip, currentPage);
                    break;
                }

                // ==============================================
                // 逐条判断时间（正确顺序，不丢数据）
                // ==============================================
                boolean needStop = false;
                for (DomainResult res : pageResult) {
                    String addtime = res.getAddtime();

                    // 空值 → 终止
                    if (addtime == null || addtime.trim().isEmpty()) {
                        log.error("IP={} 存在addtime为空的数据，终止任务", ip);
                        needStop = true;
                        break;
                    }

                    String pureAddTime = addtime.replace("-", "");

                    // 时间小于截止 → 终止
                    if (pureAddTime.compareTo(endAddTime) < 0) {
                        log.info("IP={} addtime={} 小于截止时间{}，正常结束", ip, addtime, endAddTime);
                        needStop = true;
                        break;
                    }

                    // 合格数据加入缓存
                    cacheList.add(res);

                    // 批量写入
                    if (cacheList.size() >= BATCH_WRITE_SIZE) {
                        writeBatch(writer, ip, cacheList);
                        cacheList.clear();
                    }
                }

                if (needStop) {
                    break;
                }

                if (pageResult.size() < PAGE_LIMIT) {
                    log.info("IP={} 第{}页数据不足{}条，已全部拉取完成", ip, currentPage, PAGE_LIMIT);
                    break;
                }
                // ====================== 每10页打印一次 ======================
                if (currentPage % 10 == 0) {
                    log.info("IP={} 已成功查询到第 {} 页", ip, currentPage);
                }
            }

            // 写入剩余数据
            if (!cacheList.isEmpty()) {
                writeBatch(writer, ip, cacheList);
            }

            writer.flush();
            writer.close();

            FileUtil.renameTmpToFile(tmpFilePath, finalFilePath);
            log.info("查询完成，文件已生成：{}", finalFilePath);

        } catch (Exception e) {
            log.error("IP={} 查询异常", ip, e);
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
            FileUtil.deleteTmpFile(tmpFilePath);
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
        return finalFilePath;
    }

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
        String fileName = timestamp+"_"+ip + "_loc.csv";
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