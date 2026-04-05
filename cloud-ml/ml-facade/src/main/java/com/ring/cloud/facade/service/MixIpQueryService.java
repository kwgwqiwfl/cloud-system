package com.ring.cloud.facade.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.*;
import com.ring.cloud.core.service.*;
import com.ring.cloud.facade.common.QueryType;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.MixIpInfo;
import com.ring.cloud.facade.entity.ip.MixIpRes;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.MixIpExecutor;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class MixIpQueryService extends SeaCommon {

    @Resource
    private MlIpService mlIpService;
    @Resource
    private MlDomainService mlDomainService;
    @Resource
    private MlIcpService mlIcpService;
    @Resource
    private MlSubdomainService mlSubdomainService;
    @Autowired
    private MixIpExecutor mixIpExecutor;
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;

    @Autowired
    private MixIpDomainService mixIpDomainService;

    @Autowired
    private MixDomainIpService mixDomainIpService;

    public PageResult<MlIp> pageIp(CommonPageQuery query) {
        return mlIpService.pageList(query);
    }

    public PageResult<MlDomain> pageDomain(CommonPageQuery query) {
        return mlDomainService.pageList(query);
    }

    public PageResult<MlIcp> pageIcp(CommonPageQuery query) {
        return mlIcpService.pageList(query);
    }

    public PageResult<MlSubdomain> pageSubdomain(CommonPageQuery query) {
        return mlSubdomainService.pageList(query);
    }

    public PageResult<MixIpDomain> pageMixIpDomain(CommonPageQuery query) {
        return mixIpDomainService.pageList(query);
    }

    public PageResult<MixDomainIp> pageMixDomainIp(CommonPageQuery query) {
        return mixDomainIpService.pageList(query);
    }

    public PageResult<MixIpDomain> pageSpecifyIpDomain(CommonPageQuery query) {
        return mixIpDomainService.pageSpecifyList(query);
    }

    public String mixIpStatistics() {
        long start = System.currentTimeMillis();
        MixIpInfo info = queryMix();
        long first = System.currentTimeMillis();
        if (info == null)
            throw new IllegalArgumentException("切换3次代理查询mix失败");
        queryMixExt(info);
        long second = System.currentTimeMillis();
        saveAllSixTables(info);
        return (first-start)+"-"+(second-first)+"-"+(System.currentTimeMillis()-second);

    }
    // 【内层：纯DB操作】事务只包裹数据库操作！！
    @Transactional(rollbackFor = Exception.class)
    public void saveAllSixTables(MixIpInfo info) {
        if (info.getIpList() != null) {
            mlIpService.batchSaveOrUpdate(info.getIpList());
        }
        if (info.getDomainList() != null) {
            mlDomainService.batchSaveOrUpdate(info.getDomainList());
        }
        if (info.getIcpList() != null) {
            mlIcpService.batchSaveOrUpdate(info.getIcpList());
        }
        if (info.getSubdomainList() != null) {
            mlSubdomainService.batchSaveOrUpdate(info.getSubdomainList());
        }
        mixIpDomainService.batchUpsert(info.getMixIpDomainList());
        mixDomainIpService.batchUpsert(info.getMixDomainIpList());
    }

    /**
     * 统一查询所有IP/域名/ICP/子域名数据
     */
    private MixIpInfo queryMix() {
        int retryCount = 0;
        int maxRetry = 5;
        while (retryCount < maxRetry) {
            try {
                return mixIpExecutor.queryMixInfo(globalProxyHelper.getAvailableProxy());
            } catch (Throwable e) {
                retryCount++;
                log.debug("queryMixInfo 失败，重试次数: {}", retryCount);

                if (retryCount >= maxRetry) {
                    log.debug("queryMixInfo 重试{}次全部失败，停止查询", maxRetry);
                    break;
                }
            }
        }
        return null;
    }
    /**
     * 查询ext信息
     */
    private void queryMixExt(MixIpInfo info) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            // ======================
            // 所有任务放到一起：IP + Domain 全部并发
            // ======================
            List<Callable<Void>> allTasks = new ArrayList<>();

            // 1. IP 并发任务
            for (MlIp mlIp : info.getIpList()) {
                allTasks.add(() -> {
                    String ip = mlIp.getIp();
                    IpBreakpoint bp = new IpBreakpoint();
                    List<MixIpDomain> result = mixSingleWithRetry(QueryType.IP, ip, bp, 6, 4).getMixIpDomainList();
                    info.getMixIpDomainList().addAll(result);
                    return null;
                });
            }

            // 2. Domain 并发任务（和IP一起并发）
            for (MlDomain mlDomain : info.getDomainList()) {
                allTasks.add(() -> {
                    String domain = mlDomain.getDomain();
                    List<MixDomainIp> result = mixSingleWithRetry(QueryType.DOMAIN, domain, null, 6, 4).getMixDomainIpList();
                    info.getMixDomainIpList().addAll(result);
                    return null;
                });
            }
            List<Future<Void>> futures = executor.invokeAll(allTasks, 120, TimeUnit.SECONDS);
            // 等待完成（防止丢结果）
            for (Future<Void> future : futures) {
                if (!future.isCancelled()) {
                    future.get();
                }
            }

        } catch (Exception e) {
            log.error("并发查询IP+Domain失败", e);
        } finally {
            executor.shutdown();
        }
    }

    //查询扩展信息 ip查询 和domain查询
    /**
     * 重试查询（总次数 + 代理异常次数 由外部传入）
     * @param type          查询类型
     * @param key           IP或域名
     * @param breakpoint    分页对象
     * @param maxTotalTry   最大总尝试次数
     * @param maxProxyRetry 最大代理异常重试次数
     * @return 结果
     */
    private MixIpRes mixSingleWithRetry(QueryType type, String key, IpBreakpoint breakpoint,
                                        int maxTotalTry, int maxProxyRetry) {
        int totalTry = 0;
        int proxyRetry = 0;
        ProxyIp currentProxy = null;

        while (totalTry < maxTotalTry) {
            totalTry++;
            try {
                if (currentProxy == null) {
                    currentProxy = globalProxyHelper.getAvailableProxy();
                }

                MixIpRes mixIpRes;
                if (type == QueryType.IP) {
                    mixIpRes = mixIpExecutor.extByIp(key, currentProxy, breakpoint);
                } else {
                    mixIpRes = mixIpExecutor.extByDomain(key, currentProxy);
                }

                // 成功直接返回
                if (mixIpRes != null && mixIpRes.isSuccess()) {
                    return mixIpRes;
                }

                // 业务失败 → 仅换代理，不扣代理次数
                currentProxy = null;

            } catch (Throwable e) {
                // 代理异常 → 计数
                if (globalProxyHelper.needSwitchProxy(e)) {
                    proxyRetry++;
                    currentProxy = null;
                    if (proxyRetry >= maxProxyRetry) {
                        log.warn("[{}] 代理异常超限[{}次]，放弃: {}", type, proxyRetry, key);
                        return new MixIpRes();
                    }
                }
            }
        }

        log.warn("[{}] 总尝试次数超限[{}次]，放弃: {}", type, totalTry, key);
        return new MixIpRes();
    }

    // 指定ip列表 定时任务 —— 高可靠版，尽量不失败
    public String specifyIpQuery(List<String> ipList) {
        List<MixIpDomain> mixIpDomainList = Collections.synchronizedList(new ArrayList<>());
        long start = System.currentTimeMillis();

        // 固定10线程并发
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            List<Callable<Void>> allTasks = new ArrayList<>();
            for (String ip : ipList) {
                allTasks.add(() -> {
                    try {
                        IpBreakpoint bp = new IpBreakpoint();
                        MixIpRes res = mixSingleWithRetry(QueryType.IP, ip, bp, 40, 16);
                        if (res.getMixIpDomainList() != null && !res.getMixIpDomainList().isEmpty()) {
                            mixIpDomainList.addAll(res.getMixIpDomainList());
                        }
                    } catch (Throwable e) {
                        log.error("单个IP查询异常，IP:{}", ip, e);
                    }
                    return null;
                });
            }

            // 全局超时 40 分钟，足够跑完所有重试
            List<Future<Void>> futures = executor.invokeAll(allTasks, 40, TimeUnit.MINUTES);

            // 等待所有任务结束
            for (Future<Void> future : futures) {
                try {
                    if (!future.isCancelled()) {
                        future.get();
                    }
                } catch (Exception e) {
                    // 单个任务失败不影响整体
                    log.warn("IP任务执行被中断", e);
                }
            }

        } catch (Exception e) {
            log.error("IP并发查询整体异常", e);
        } finally {
            // 安全关闭线程池，绝对不泄露
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        // 入库
        long second = System.currentTimeMillis();
        specifyBatchUpsert(mixIpDomainList);

        // 返回耗时：查询耗时-入库耗时
        return (second - start) + "-" + (System.currentTimeMillis() - second);
    }

    @Transactional(rollbackFor = Exception.class)
    public void specifyBatchUpsert(List<MixIpDomain> sIpDomainList) {
        if (sIpDomainList == null || sIpDomainList.isEmpty()) {
            log.info("specify_ip_domain 无数据需要入库");
            return;
        }
        mixIpDomainService.specifyBatchUpsert(sIpDomainList);
    }

}