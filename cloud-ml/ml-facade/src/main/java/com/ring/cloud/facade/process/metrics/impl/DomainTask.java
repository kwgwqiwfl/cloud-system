package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.core.pojo.DomainInout;
import com.ring.cloud.core.service.DomainInoutService;
import com.ring.cloud.facade.common.ExecuteType;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.TaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.DomainExecutor;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import com.ring.cloud.facade.socket.WsMessageType;
import com.ring.cloud.facade.socket.WsUtil;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DomainTask extends AbstractTask<TaskEntity> {

    @Autowired
    private DomainExecutor domainExecutor;
    @Autowired
    private DomainInoutService inoutService;

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.DOMAIN;
    }

    @Override
    public boolean runTask(TaskEntity task) {
        List<String> domainList = task.getHandleKeyList();
        String uniqueKey = "domain_import_task";

        try {
            return batchCrawl(task, uniqueKey, domainList);
        } catch (Throwable e) {
            log.error("[域名批量-异常] 域名数量={} 异常：{}",
                    domainList.size(), e.getMessage(), e);
            // 失败：进度+0
            progressManager.onSegmentFinish(uniqueKey, 0);
            return false;
        }
    }
    //主方法，域名-子域名-ip
    private boolean batchCrawl(TaskEntity task, String uniqueKey, List<String> domainList) {
        ProxyIp currentProxy = getAvailableProxy();
        List<DomainInout> domainResultList = new ArrayList<>();
        try {
            int domainSize = domainList.size();
            for(String domain: domainList) {
                if (isTaskStopped(uniqueKey)) {
                    log.info("任务[" + uniqueKey + "]已终止，最后处理：" + domain);
                    return true;
                }
                long start = System.currentTimeMillis();
                //获取域名的子域名列表 也需要翻页查
                Set<String> subdomainSet = subdomainByDomain(domain, currentProxy, uniqueKey);
                if (subdomainSet.isEmpty()) {
                    log.debug("未查询到子域名数据，切换下一个域名：{}", domain);
                    continue;
                }
                long sub = System.currentTimeMillis();
                Set<String> ipSet = ipBySubdomains(subdomainSet, currentProxy, uniqueKey);
                long ip = System.currentTimeMillis();
                //过滤
                Set<String> ipFilterSet = IpUtil.filterIpSet(ipSet);
                domainResultList = processIpSet(domain, ipFilterSet, currentProxy, uniqueKey);
                long inout = System.currentTimeMillis();
                int resultSize = domainResultList.size();
                if (!domainResultList.isEmpty()) {
                    log.debug("【域名批量-入库】查询完成，开始入库，数量={}", resultSize);
                    domainBatchUpsert(domainResultList);
                    domainResultList.clear();
                }
                long upsert = System.currentTimeMillis();
                String cost = (sub-start)+"-"+(ip-sub)+"-"+(inout-ip)+"-"+(upsert-inout);
                log.info("{} -- {} -- {} -- cost:{}", domain, ipFilterSet.size(), resultSize, cost);
                WsUtil.push(WsMessageType.DOMAIN_TASK, "域名："+ domain+" -- "+ipFilterSet.size()+" -- "+resultSize+" -- cost:"+cost);
            }
            // 标记完成：上报本次处理的域名数量
            progressManager.onSegmentFinish(uniqueKey, domainSize);
            return true;
        } catch (Throwable e) {
            log.error("[域名批量-执行异常]", e);
            progressManager.onSegmentFinish(uniqueKey, 0);
            return false;
        }
    }
    // 域名-子域名
    private Set<String> subdomainByDomain(String domain, ProxyIp currentProxy, String uniqueKey) {
        IpBreakpoint breakpoint = new IpBreakpoint();
        breakpoint.setExecuteType(ExecuteType.SUBDOMAIN);
        retryExecute(uniqueKey, currentProxy, breakpoint, domain, null, 15);
        return breakpoint.getSet();
    }
    // 子域名-ip
    private Set<String> ipBySubdomains(Set<String> subDomainList, ProxyIp currentProxy, String uniqueKey) {
        Set<String> ips = new HashSet<>();
        IpBreakpoint breakpoint = new IpBreakpoint();
        breakpoint.setExecuteType(ExecuteType.DOMAIN);
        for (String domain : subDomainList) {
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理：" + domain);
                return ips;
            }
            breakpoint.reset();
            retryExecute(uniqueKey, currentProxy, breakpoint, domain, null, 15);
            ips.addAll(breakpoint.getSet());
        }
        return ips;
    }
    //处理单个domain的ip列表 processIpSet(ipFilterSet, currentProxy, uniqueKey);
    private List<DomainInout> processIpSet(String domain, Set<String> ipFilterSet, ProxyIp currentProxy, String uniqueKey) {
        List<DomainInout> inoutList = new ArrayList<>();
        IpBreakpoint breakpoint = new IpBreakpoint();
        for (String ip : ipFilterSet) {
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理：" + domain);
                return inoutList;
            }
            breakpoint.reset();
            retryExecute(uniqueKey, currentProxy, breakpoint, ip, null, 15);
            @SuppressWarnings("unchecked")
            List<DomainInout> resultList = (List<DomainInout>) (List<?>) breakpoint.getList();
            for(DomainInout inout: resultList){
                inout.setInputDomain(domain);
                inoutList.add(inout);
            }
        }
        return inoutList;
    }

    @Override
    protected boolean doExecute(String key, BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint) throws IOException {
        ExecuteType type = breakpoint.getExecuteType();
        if(type == ExecuteType.DEFAULT_IP)
            return domainExecutor.execute(key, currentProxy, breakpoint);
        else if (type == ExecuteType.DOMAIN)
            return domainExecutor.executeDomain(key, currentProxy, breakpoint);
        else if (type == ExecuteType.SUBDOMAIN)
            return domainExecutor.executeSubdomain(key, currentProxy, breakpoint);
        return domainExecutor.execute(key, currentProxy, breakpoint);
    }

    @Transactional(rollbackFor = Exception.class)
    public void domainBatchUpsert(List<DomainInout> domainResultList) {
        inoutService.batchUpsert(domainResultList);
    }
}