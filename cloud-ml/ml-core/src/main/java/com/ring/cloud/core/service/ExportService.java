package com.ring.cloud.core.service;

import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.core.util.FileCoreUtil;
import com.ring.cloud.core.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =======================================================================
    // 🔥 导出所有数据（真正流式，绝对不卡）
    // =======================================================================
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void exportMix(String path) {
        String timestamp = DateUtil.fileSuffixSDF.format(new Date());

        try {
            // 1. 最新域名
            exportSimpleTable(
                    path + "/" + timestamp + "_最新域名.csv",
                    "domains,adtime,uptime,queryCount",
                    "SELECT domains,create_time,update_time,query_count FROM ml_domain"
            );

            // 2. 最新IP
            exportSimpleTable(
                    path + "/" + timestamp + "_最新ip.csv",
                    "ip,adtime,uptime,queryCount",
                    "SELECT ip,create_time,update_time,query_count FROM ml_ip"
            );

            // 3. 最新备案
            exportSimpleTable(
                    path + "/" + timestamp + "_最新备案.csv",
                    "domains,adtime,uptime,queryCount",
                    "SELECT domains,create_time,update_time,query_count FROM ml_icp"
            );

            // 4. 最新子域名
            exportSimpleTable(
                    path + "/" + timestamp + "_最新子域名.csv",
                    "domains,adtime,uptime,queryCount",
                    "SELECT domains,create_time,update_time,query_count FROM ml_subdomain"
            );

            // 5. 特定IP
            exportSimpleTable(
                    path + "/" + timestamp + "_特定ip.csv",
                    "ip,loc,domains,adtime,uptime",
                    "SELECT ip,loc,domains,adtime,uptime FROM specify_ip_domain"
            );

            // 6. 最新域名AI
            exportSimpleTable(
                    path + "/" + timestamp + "_最新域名ai.csv",
                    "domains,adtime,uptime,totalCount,dayCount",
                    "SELECT domains,ad_time,up_time,total_count,day_count FROM ml_domain_ai"
            );

            // ===================== 16张 IP 分表 =====================
            String ipFile = path + "/" + timestamp + "_最新ip查询域名.csv";
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ipFile), StandardCharsets.UTF_8), 1024 * 1024)) {
                bw.write("ip,loc,domains,adtime,uptime");
                bw.newLine();
                for (int i = 0; i < 16; i++) {
                    streamIpTable(bw, "mix_ip_" + i);
                }
            }

            // ===================== 16张 Domain 分表 =====================
            String domainFile = path + "/" + timestamp + "_最新域名查询ip.csv";
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(domainFile), StandardCharsets.UTF_8), 1024 * 1024)) {
                bw.write("ip,domains,adtime,uptime");
                bw.newLine();
                for (int i = 0; i < 16; i++) {
                    streamDomainTable(bw, "mix_domain_" + i);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    // =======================================================================
    // 普通小表导出（通用）
    // =======================================================================
    private void exportSimpleTable(String filePath, String header, String sql) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8), 1024 * 1024)) {

            bw.write(header);
            bw.newLine();

            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            try (PreparedStatement pstmt = conn.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                pstmt.setFetchSize(Integer.MIN_VALUE);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        bw.write(buildCsvLine(rs));
                        bw.newLine();
                    }
                }
            } finally {
                DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            }
        }
    }

    // =======================================================================
    // 导出 IP 分表（流式）
    // =======================================================================
    private void streamIpTable(BufferedWriter bw, String table) throws Exception {
        String sql = "SELECT ip,loc,domains,adtime,uptime FROM " + table;
        Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());

        try (PreparedStatement pstmt = conn.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

            pstmt.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String line =
                            escape(rs.getString("ip")) + "," +
                                    escape(rs.getString("loc")) + "," +
                                    escape(rs.getString("domains")) + "," +
                                    rs.getDate("adtime") + "," +
                                    rs.getDate("uptime");

                    bw.write(line);
                    bw.newLine();
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        }
    }

    // =======================================================================
    // 导出 Domain 分表（流式）
    // =======================================================================
    private void streamDomainTable(BufferedWriter bw, String table) throws Exception {
        String sql = "SELECT ip,domains,adtime,uptime FROM " + table;
        Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());

        try (PreparedStatement pstmt = conn.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

            pstmt.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String line =
                            escape(rs.getString("ip")) + "," +
                                    escape(rs.getString("domains")) + "," +
                                    rs.getDate("adtime") + "," +
                                    rs.getDate("uptime");

                    bw.write(line);
                    bw.newLine();
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
        }
    }

    // =======================================================================
    // CSV 安全转义（防止逗号/换行破坏格式）
    // =======================================================================
    private String escape(String s) {
        if (s == null) return "";
        s = s.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    // =======================================================================
    // 自动构造行
    // =======================================================================
    private String buildCsvLine(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int colCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            if (i > 1) sb.append(",");
            sb.append(escape(rs.getString(i)));
        }
        return sb.toString();
    }

    // ===================== 【追加：两个域名导出方法】 =====================
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void exportDomainData(List<String> inputDomainList, String exportDirPath) {
        File exportDir = new File(exportDirPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        final int BATCH_SIZE = 100000;
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        String currentInput = null;
        BufferedWriter writer = null;
        File currentTmpFile = null;

        try {
            List<String> hashList = inputDomainList.stream().map(HashUtil::sha1).collect(Collectors.toList());
            String sql = buildExportStatSql(hashList);

            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            try (PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                pstmt.setFetchSize(Integer.MIN_VALUE);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String input = rs.getString("inputDomain");
                        String output = rs.getString("outputDomain");
                        int count = rs.getInt("count");

                        if (currentInput != null && !input.equals(currentInput)) {
                            FileCoreUtil.writeBatch(writer, batch);
                            writer.close();
                            FileCoreUtil.renameToFinal(currentTmpFile, currentInput, exportDir);
                            writer = null;
                            currentTmpFile = null;
                        }

                        if (writer == null) {
                            currentInput = input;
                            File finalFile = new File(exportDir, currentInput + ".csv");
                            currentTmpFile = new File(exportDir, currentInput + ".tmp");
                            if (currentTmpFile.exists()) currentTmpFile.delete();
                            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentTmpFile), StandardCharsets.UTF_8));
                        }

                        batch.add(output + "," + count);
                        if (batch.size() >= BATCH_SIZE) {
                            FileCoreUtil.writeBatch(writer, batch);
                        }
                    }
                }
            } finally {
                DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            }

            if (writer != null && currentTmpFile != null) {
                FileCoreUtil.writeBatch(writer, batch);
                writer.close();
                FileCoreUtil.renameToFinal(currentTmpFile, currentInput, exportDir);
            }

        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + (currentTmpFile != null ? currentTmpFile.getName() : ""), e);
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (Exception ignored) {}
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void exportAllDomainData(String fullFilePath) {
        final int BATCH_SIZE = 100000;
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        File targetFile = new File(fullFilePath);
        File tmpFile = new File(fullFilePath + ".tmp");
        if (tmpFile.exists()) tmpFile.delete();
        if (targetFile.exists()) targetFile.delete();

        BufferedWriter writer = null;
        String sql = "SELECT input_domain AS inputDomain, output_domain AS outputDomain, COUNT(*) AS count " +
                "FROM domain_inout " +
                "GROUP BY input_hash, output_hash, input_domain, output_domain " +
                "ORDER BY input_domain, count DESC";

        try {
            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            try (PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                pstmt.setFetchSize(Integer.MIN_VALUE);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String inputDomain = rs.getString("inputDomain");
                        String outputDomain = rs.getString("outputDomain");
                        int count = rs.getInt("count");
                        batch.add(inputDomain + "," + outputDomain + "," + count);

                        if (batch.size() >= BATCH_SIZE) {
                            if (writer == null) {
                                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8));
                            }
                            FileCoreUtil.writeBatch(writer, batch);
                        }
                    }
                }
            } finally {
                DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            }

            if (writer == null) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8));
            }
            FileCoreUtil.writeBatch(writer, batch);
            writer.close();

            if (!tmpFile.renameTo(targetFile)) {
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

    // ===================== 内部工具：动态拼接 IN 查询 SQL =====================
    private String buildExportStatSql(List<String> hashList) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT input_domain AS inputDomain, output_domain AS outputDomain, COUNT(*) AS count FROM domain_inout ");

        if (hashList != null && !hashList.isEmpty()) {
            sb.append("WHERE input_hash IN (");
            for (int i = 0; i < hashList.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("'").append(hashList.get(i).replace("'", "''")).append("'");
            }
            sb.append(") ");
        }

        sb.append("GROUP BY input_hash, output_hash, input_domain, output_domain ");
        sb.append("ORDER BY input_domain, count DESC");
        return sb.toString();
    }
}