package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.KeywordExecutor;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Slf4j
@Component
public class KeywordTask extends AbstractTask<TaskEntity> {

    @Autowired
    private KeywordExecutor keywordExecutor;

//    @Autowired(required = false)
    @Autowired
    private RedissonClient redissonClient;

    @Value("${ml.client.keyword.output.path:/}")
    private String keywordOutPath;

    private static final int FLUSH_BATCH_SIZE = 1000;

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.KEYWORD;
    }

    @Override
    public boolean runTask(TaskEntity task) {
        List<String> keywordList = task.getHandleKeyList();
        String uniqueKey = "key_import_task";
        try {
            return batchCrawl(task, uniqueKey, keywordList);
        } catch (Throwable e) {
            log.error("[关键词抓取-异常] 数量={} 异常：{}", keywordList.size(), e.getMessage(), e);
            return false;
        }
    }

    private boolean batchCrawl(TaskEntity task, String uniqueKey, List<String> keywordList) {
        ProxyIp currentProxy = getAvailableProxy();
        String site = task.getSite();
        List<String> dataBuffer = new ArrayList<>(FLUSH_BATCH_SIZE);

        File tmpFile = new File(keywordOutPath, site + ".tmp");
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(tmpFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    StandardCharsets.UTF_8));

            RBloomFilter<String> bloom = redissonClient.getBloomFilter("bf:kw:" + site);
            log.info("[关键词抓取] 站点{}开始执行，总种子词数量：{}", site, keywordList.size());

            for (String keyword : keywordList) {
                if (isTaskStopped(uniqueKey)) {
                    log.info("任务[{}]已终止", uniqueKey);
                    break;
                }

                long start = System.currentTimeMillis();
                if (bloom.contains(keyword)) continue;

                // ------------- 你的原有代码 -------------
                Set<String> dropKeywordSet = queryWithRetry(
                        keyword,
                        currentProxy,
                        10,
                        (kw, p) -> doCrawlDrop(kw, site, p),  // <-- 只改这里
                        k -> new HashSet<>()
                );

                if (dropKeywordSet != null && !dropKeywordSet.isEmpty()) {
                    dataBuffer.add(keyword);
                    dataBuffer.addAll(dropKeywordSet);
                }
                bloom.add(keyword);

                if (dataBuffer.size() >= FLUSH_BATCH_SIZE) {
                    batchWrite(bw, dataBuffer);
                }

                long cost = System.currentTimeMillis() - start;
                log.info("[{}] 完成，耗时：{}ms", keyword, cost);
                WsUtil.push(WsMessageType.KEYWORD_TASK, "关键词：" + keyword+" size: "+dropKeywordSet.size());
            }

            // 最后刷盘
            if (!dataBuffer.isEmpty()) {
                batchWrite(bw, dataBuffer);
            }

        } catch (Exception e) {
            log.error("[{}]文件写入异常", site, e);
        } finally {
            // ===================== 【核心：所有收尾都放这里】 =====================
            try {
                // 1. 关闭流
                if (bw != null) {
                    bw.close();
                }

                // 2. 去重 + 合并 + 改名
                if (tmpFile.exists()) {
                    distinctAndRenameFile(site, tmpFile);
                }

            } catch (Exception e) {
                log.error("[{}] finally 流关闭/文件合并异常", site, e);
            }
        }

        return true;
    }

    // 批量写入
    private void batchWrite(BufferedWriter writer, List<String> buffer) throws Exception {
        for (String word : buffer) {
            writer.write(word);
            writer.newLine();
        }
        buffer.clear();
    }

    private Set<String> doCrawlDrop(String keyword, String site, ProxyIp proxy) {
        return keywordExecutor.execute(keyword, site, proxy);
    }

    // ===================== finally 统一调用：去重 + 覆盖正式文件 =====================
    private void distinctAndRenameFile(String site, File tmpFile) throws Exception {
        File finalFile = new File(keywordOutPath, site + ".txt");
        File tempNewFile = new File(keywordOutPath, site + "_new.tmp");

        // 流式去重，不占内存
        Set<String> allLines = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(tmpFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                allLines.add(line.trim().toLowerCase());
            }
        }

        // 写入新文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempNewFile))) {
            for (String line : allLines) {
                writer.write(line);
                writer.newLine();
            }
        }

        // 原子覆盖
        if (finalFile.exists()) {
            finalFile.delete();
        }
        tempNewFile.renameTo(finalFile);
        tmpFile.delete();

        log.info("[{}] 文件去重完成 → 正式文件：{}", site, finalFile.getName());
    }
}