package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.DomainExecutor;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DomainSubTask extends AbstractTask<TaskEntity> {

    @Autowired
    private DomainExecutor domainExecutor;

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.DOMAIN_SUB;
    }

    @Override
    public boolean runTask(TaskEntity task) {
        List<String> domainList = task.getHandleKeyList();
        String uniqueKey = "domain_sub_import_task";

        try {
            return batchCrawl(task, uniqueKey, domainList);
        } catch (Throwable e) {
            log.error("[子域名批量-异常] 域名数量={} 异常：{}",
                    domainList.size(), e.getMessage(), e);
            return false;
        }
    }
    //主方法，域名-子域名
    private boolean batchCrawl(TaskEntity task, String uniqueKey, List<String> domainList) {
        ProxyIp currentProxy = getAvailableProxy();
        String site = task.getSite();
        List<String> dataBuffer = new ArrayList<>(FLUSH_BATCH_SIZE);
        final int MAX_SUB_PER_PROXY = 9;
        int proxySubCount = 0;
        File tmpFile = new File(task.getOutPath(), site + "_" + task.getTimeStamp() + ".tmp");
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(tmpFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    StandardCharsets.UTF_8));
            int domainSize = domainList.size();
            log.info("[子域名任务] {}开始执行，总域名数量：{}", site, domainSize);
            int siteDomainIndex = 0;
            for(String domain: domainList) {
                if (isTaskStopped(uniqueKey)) {
                    log.info("任务[" + uniqueKey + "]已终止，最后处理：" + domain);
                    return true;
                }
                if (proxySubCount >= MAX_SUB_PER_PROXY) {
                    log.debug("[代理切换] 单个代理已查询{}个域名，自动更换新代理", MAX_SUB_PER_PROXY);
                    currentProxy = getAvailableProxy();
                    proxySubCount = 0;
                }

                long start = System.currentTimeMillis();
                proxySubCount++;
                //只查第一页
                List<String> subList = queryWithRetry(
                        domain,
                        currentProxy,
                        5,
                        domainExecutor::firstSubs,
                        k -> new ArrayList<>()
                );
                if (subList.isEmpty()) {
                    log.debug("未查询到子域名数据，切换下一个域名：{}", domain);
                    continue;
                }
                int size = subList.size();
                for(String sub:subList){
                    dataBuffer.add(domain+","+sub+","+size);
                }

                if (dataBuffer.size() >= FLUSH_BATCH_SIZE) {
                    batchWrite(bw, dataBuffer);
                }

                siteDomainIndex++;
                if (siteDomainIndex % 50 == 0) {
                    log.info(site + "第{}个：{} 完成，子域名：{}", siteDomainIndex, domain, size);
                    WsUtil.push(WsMessageType.DOMAIN_SUB_TASK, "📌 " + site + " | 第" + siteDomainIndex + "个域名：" + domain + " | 结果数量：" + size);
                }
            }
            if (!dataBuffer.isEmpty()) {
                batchWrite(bw, dataBuffer);
            }
            bw.flush();
        }  catch (Exception e) {
            log.error("[{}]文件写入异常", site, e);
            return false;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
//                if (tmpFile.exists()) {
//                    String path = tmpFile.getAbsolutePath();
//                    File csvFile = new File(path.replace(".tmp", ".csv"));
//                    tmpFile.renameTo(csvFile);
//                }
            } catch (Exception e) {
                log.error("[{}] finally 流关闭/文件合并异常", site, e);
            }
        }
        return true;
    }

}