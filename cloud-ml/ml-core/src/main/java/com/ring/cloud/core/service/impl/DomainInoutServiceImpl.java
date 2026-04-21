package com.ring.cloud.core.service.impl;

import com.google.common.collect.Lists;
import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.entity.ip.DomainCount;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.DomainInoutMapper;
import com.ring.cloud.core.pojo.DomainInout;
import com.ring.cloud.core.service.DomainInoutService;
import com.ring.cloud.core.util.FileCoreUtil;
import com.ring.cloud.core.util.HashUtil;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DomainInoutServiceImpl extends EntityClassServiceImpl<DomainInout> implements DomainInoutService {

    @Autowired
    private DomainInoutMapper mapper;

    @Override
    public MyIdableMapper<DomainInout> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<DomainInout> pageList(CommonPageQuery query) {
        String inputDomain = query.getKey().trim();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 分页偏移量
        int offset = (pageNum - 1) * pageSize;

        // 分页查询
        List<DomainInout> list = mapper.selectPageByInputDomain(inputDomain, offset, pageSize);

        return PageResult.of(0, pageNum, pageSize, list);
    }

    @Override
    public PageResult<DomainInout> pageByInputDomainNoCount(CommonPageQuery query) {
        return null;
    }

    @Override
    public void batchUpsert(List<DomainInout> list) {
        // 每200条一批插入，防止SQL过长
        Lists.partition(list, 200).forEach(batch -> {
            mapper.batchUpsert(batch);
        });
    }

    @Override
    public void exportDomainData(List<String> inputDomainList, String exportDirPath) {
        File exportDir = new File(exportDirPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        final int BATCH_SIZE = 100000;
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        String currentInput = null;
        BufferedWriter writer = null;
        File currentTmpFile = null; // 临时文件

        try {
            List<String> hashList = inputDomainList.stream().map(HashUtil::sha1).collect(Collectors.toList());
            for (DomainCount row : mapper.exportStatStream(hashList)) {
                String input = row.getInputDomain();
                String output = row.getOutputDomain();
                int count = row.getCount();

                // 切换域名 → 关闭上一个并改名
                if (currentInput != null && !input.equals(currentInput)) {
                    // 写入最后一批
                    writeBatch(writer, batch);
                    writer.close();

                    // 临时文件 → 正式文件
                    FileCoreUtil.renameToFinal(currentTmpFile, currentInput, exportDir);

                    writer = null;
                    currentTmpFile = null;
                }

                // 新文件：使用临时文件名
                if (writer == null) {
                    currentInput = input;

                    // 正式文件
                    File finalFile = new File(exportDir, currentInput + ".csv");
                    // 临时文件
                    currentTmpFile = new File(exportDir, currentInput + ".tmp");

                    // 如果有旧临时文件，先删除
                    if (currentTmpFile.exists()) {
                        currentTmpFile.delete();
                    }

                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentTmpFile), StandardCharsets.UTF_8));
                }

                batch.add(output + "," + count);

                // 批量写入
                if (batch.size() >= BATCH_SIZE) {
                    writeBatch(writer, batch);
                }
            }

            // 最后一个文件
            if (writer != null && currentTmpFile != null) {
                writeBatch(writer, batch);
                writer.close();
                FileCoreUtil.renameToFinal(currentTmpFile, currentInput, exportDir);
            }

        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + (currentTmpFile != null ? currentTmpFile.getName() : ""), e);

        } finally {
            // 安全关闭流
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {}
            }
        }
    }

    // 批量写入
    private void writeBatch(BufferedWriter writer, List<String> batch) throws Exception {
        if (writer == null || batch.isEmpty()) return;
        for (String line : batch) {
            writer.write(line);
            writer.newLine();
        }
        batch.clear();
    }

    /**
     * 导出所有域名统计到单个文件（三列：输入域名,输出域名,次数）
     * @param fullFilePath 文件全路径
     */
    @Override
    public void exportAllDomainData(String fullFilePath) {
        final int BATCH_SIZE = 100000;
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        File targetFile = new File(fullFilePath);
        File tmpFile = new File(fullFilePath + ".tmp");

        // 已存在则删除
        if (tmpFile.exists()) tmpFile.delete();
        if (targetFile.exists()) targetFile.delete();

        BufferedWriter writer = null;

        try {
            // 流式全量查询
            for (DomainCount row : mapper.exportAllStatStream()) {
                String inputDomain = row.getInputDomain();
                String outputDomain = row.getOutputDomain();
                int count = row.getCount();

                // 三列拼接：输入域名,输出域名,次数
                batch.add(inputDomain + "," + outputDomain + "," + count);

                // 批量写入防OOM
                if (batch.size() >= BATCH_SIZE) {
                    if (writer == null) {
                        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8));
                    }
                    writeBatch(writer, batch);
                }
            }

            // 最后一批
            if (writer == null) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8));
            }
            writeBatch(writer, batch);
            writer.close();

            // 重命名为最终文件
            boolean ok = tmpFile.renameTo(targetFile);
            if (!ok) {
                throw new RuntimeException("导出失败：临时文件重命名失败");
            }

        } catch (Exception e) {
            if (tmpFile.exists()) tmpFile.delete();
            throw new RuntimeException("全量导出失败", e);

        } finally {
            if (writer != null) {
                try { writer.close(); } catch (Exception ignored) {}
            }
        }
    }

}