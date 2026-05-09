package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.KeywordExecutor;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${ml.client.keyword.output.path:/}")
    private String keywordOutPath;

    private static final int FLUSH_BATCH_SIZE = 1000;

    // 大文件去重用的配置
    private static final int BUFFER_SIZE = 4 * 512 * 1024;
    private static final int CHUNK_LINES = 500000;

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

        final int MAX_KEYWORD_PER_PROXY = 9;
        int proxyKeywordCount = 0;

        File tmpFile = new File(keywordOutPath, site + "_" + task.getTimeStamp() + ".tmp");
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(tmpFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    StandardCharsets.UTF_8));

            log.info("[关键词抓取] 站点{}开始执行，总种子词数量：{}", site, keywordList.size());
            int siteKeywordIndex = 0;
            for (String keyword : keywordList) {
                if (isTaskStopped(uniqueKey)) {
                    log.info("任务[{}]已终止", uniqueKey);
                    break;
                }

                if (proxyKeywordCount >= MAX_KEYWORD_PER_PROXY) {
                    log.debug("[代理切换] 单个代理已查询{}个关键词，自动更换新代理", MAX_KEYWORD_PER_PROXY);
                    currentProxy = getAvailableProxy();
                    proxyKeywordCount = 0;
                }

                long start = System.currentTimeMillis();
                proxyKeywordCount++;

                Set<String> dropKeywordSet = queryWithRetry(
                        keyword,
                        currentProxy,
                        10,
                        (kw, p) -> doCrawlDrop(kw, site, p),
                        k -> new HashSet<>()
                );

                if (dropKeywordSet != null && !dropKeywordSet.isEmpty()) {
                    dataBuffer.add(keyword);
                    dataBuffer.addAll(dropKeywordSet);
                }

                if (dataBuffer.size() >= FLUSH_BATCH_SIZE) {
                    batchWrite(bw, dataBuffer);
                }

                long cost = System.currentTimeMillis() - start;
                siteKeywordIndex++;
                if (siteKeywordIndex % 20 == 0) {
                    log.info(site + "第{}个：{} 完成，耗时：{}ms", siteKeywordIndex, keyword, cost);
                    WsUtil.push(WsMessageType.KEYWORD_TASK, "📌 " + site + " | 第" + siteKeywordIndex + "个关键词：" + keyword + " | 结果数量：" + dropKeywordSet.size());
                }
            }

            if (!dataBuffer.isEmpty()) {
                batchWrite(bw, dataBuffer);
            }

        } catch (Exception e) {
            log.error("[{}]文件写入异常", site, e);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (tmpFile.exists()) {
                    distinctAndRenameFile(site, tmpFile, task.getTimeStamp());
                }
            } catch (Exception e) {
                log.error("[{}] finally 流关闭/文件合并异常", site, e);
            }
        }

        return true;
    }

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

    // ======================
    // 【全新：参考你给的大文件去重，完全重写】
    // ======================
    private void distinctAndRenameFile(String site, File tmpFile, String timeStamp) throws Exception {
        File finalFile = new File(keywordOutPath, site + "_" + timeStamp + ".txt");
        List<File> chunkFiles = new ArrayList<>();

        // 1. 分块 + 排序
        splitAndSort(tmpFile, chunkFiles);

        // 2. 归并去重
        mergeAndDedup(chunkFiles, finalFile);

        // 3. 清理临时块
        for (File f : chunkFiles) {
            if (f.exists()) f.delete();
        }

        // 4. 删除原始临时文件
        if (tmpFile.exists()) {
            tmpFile.delete();
        }

        log.info("[{}] 文件去重完成 → 最终文件：{}", site, finalFile.getName());
    }

    private void splitAndSort(File inputFile, List<File> chunks) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8), BUFFER_SIZE)) {
            List<String> lines = new ArrayList<>(CHUNK_LINES);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.isEmpty()) continue;

                lines.add(line);
                if (lines.size() >= CHUNK_LINES) {
                    sortAndSaveChunk(lines, chunks);
                    lines.clear();
                }
            }
            if (!lines.isEmpty()) {
                sortAndSaveChunk(lines, chunks);
            }
        }
    }

    private void sortAndSaveChunk(List<String> lines, List<File> chunks) throws Exception {
        lines.sort(String::compareTo);
        File chunk = File.createTempFile("kw_chunk", ".tmp");
        chunks.add(chunk);

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(chunk), StandardCharsets.UTF_8), BUFFER_SIZE)) {
            for (String line : lines) {
                w.write(line);
                w.newLine();
            }
        }
    }

    private void mergeAndDedup(List<File> chunks, File output) throws Exception {
        List<BufferedReader> readers = new ArrayList<>();
        PriorityQueue<LineItem> pq = new PriorityQueue<>(Comparator.comparing(i -> i.line));

        try {
            // 初始化堆
            for (int i = 0; i < chunks.size(); i++) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(chunks.get(i)), StandardCharsets.UTF_8), BUFFER_SIZE);
                readers.add(br);
                String line = br.readLine();
                if (line != null) {
                    pq.add(new LineItem(line, i));
                }
            }

            // 写入去重结果
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), BUFFER_SIZE)) {
                String lastLine = null;
                while (!pq.isEmpty()) {
                    LineItem item = pq.poll();
                    String current = item.line;

                    if (lastLine == null || !current.equals(lastLine)) {
                        w.write(current);
                        w.newLine();
                        lastLine = current;
                    }

                    String next = readers.get(item.index).readLine();
                    if (next != null) {
                        pq.add(new LineItem(next, item.index));
                    }
                }
            }
        } finally {
            for (BufferedReader br : readers) {
                try {
                    br.close();
                } catch (Exception ignored) {}
            }
        }
    }

    static class LineItem {
        String line;
        int index;

        LineItem(String line, int index) {
            this.line = line;
            this.index = index;
        }
    }
}