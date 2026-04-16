package com.ring.cloud.facade.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpRecordWriter {

//    // 从配置读取
//    @Value("${ml.client.ip.large.mark:false}")
//    private boolean enableRecord;
//
//    @Value("${ml.client.ip.large.file.path:./largeIp.txt}")
//    private String filePath;
//
//    private final ReentrantLock lock = new ReentrantLock();
//
//    /**
//     * 记录大IP —— 不去重、多线程安全、可配置开关
//     */
//    public void recordLargeIp(String ip) {
//        // 配置关闭 → 直接跳过
//        if (!enableRecord) {
//            return;
//        }
//
//        lock.lock();
//        try (FileOutputStream fos = new FileOutputStream(filePath, true);
//             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
//             PrintWriter writer = new PrintWriter(osw)) {
//
//            writer.println(ip);
//
//        } catch (Exception e) {
//            log.error("写入大IP记录文件失败: {}", e.getMessage(), e);
//        } finally {
//            lock.unlock();
//        }
//    }
}
