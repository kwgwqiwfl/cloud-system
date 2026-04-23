package com.ring.cloud.facade.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.*;
import com.ring.cloud.core.service.*;
import com.ring.cloud.facade.common.QueryType;
import com.ring.cloud.facade.config.SpecifyIpSchedule;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.MixIpInfo;
import com.ring.cloud.facade.entity.ip.MixIpRes;
import com.ring.cloud.facade.process.metrics.impl.MixIpImpl;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    protected GlobalProxyHelper globalProxyHelper;
    @Autowired
    protected MixIpImpl mixIpImpl;

    @Autowired
    private MixIpDomainService mixIpDomainService;

    @Autowired
    private MixDomainIpService mixDomainIpService;

    @Autowired
    private MlDomainAiService mlDomainAiService;

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

    @Autowired(required = false)
    private SpecifyIpSchedule specifyIpSchedule;

    public void addSpecifyIp(String ip) {
        if(StringUtils.isEmpty(ip))
            throw new IllegalArgumentException("ip 不能为空");
        specifyIpSchedule.addIp(ip);
    }
    public void removeSpecifyIp(String ip) {
        if(StringUtils.isEmpty(ip))
            throw new IllegalArgumentException("ip 不能为空");
        specifyIpSchedule.removeIp(ip);
    }
    public void invokeSpecifyIp() {
        specifyIpSchedule.execute();
    }

    //ai domain定时查询任务
    public String mlDomainAi() {
        long start = System.currentTimeMillis();
        List<MlDomainAi> domainAis = mixIpImpl.queryDomainAi();
        long first = System.currentTimeMillis();
        if (domainAis == null)
            throw new IllegalArgumentException("切换7次代理查询ai失败");
        saveOrUpdateDomainAi(domainAis);
        return (first-start)+"-"+(System.currentTimeMillis()-first);

    }
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateDomainAi(List<MlDomainAi> domainAis) {
        mlDomainAiService.batchSaveOrUpdate(domainAis);
    }
    //ip最新查询定时任务
    public String mixIpStatistics() {
        long start = System.currentTimeMillis();
        MixIpInfo info = mixIpImpl.queryMix();
        long first = System.currentTimeMillis();
        if (info == null)
            throw new IllegalArgumentException("切换5次代理查询mix失败");
        queryMixExt(info);
        long second = System.currentTimeMillis();
        saveAllMixTables(info);
        return (first-start)+"-"+(second-first)+"-"+(System.currentTimeMillis()-second);

    }
    // 【内层：纯DB操作】事务只包裹数据库操作！！
    @Transactional(rollbackFor = Exception.class)
    public void saveAllMixTables(MixIpInfo info) {
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
     * 查询ext信息
     */
    private void queryMixExt(MixIpInfo info) {
        ExecutorService executor = new ThreadPoolExecutor(
                10, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            // =============================
            // 🔥 关键点1：任务返回数据，不直接添加到集合
            // =============================
            List<Callable<List<MixIpDomain>>> ipTasks = new ArrayList<>();
            List<Callable<List<MixDomainIp>>> domainTasks = new ArrayList<>();

            // IP任务
            for (MlIp mlIp : info.getIpList()) {
                ipTasks.add(() -> {
                    String ip = mlIp.getIp();
                    if (IpUtil.isInternalIp(ip)) {
                        log.debug("内网IP跳过查询: {}", ip);
                        return new ArrayList<>();
                    }
                    IpBreakpoint bp = new IpBreakpoint();
                    MixIpRes res = mixIpImpl.mixSingleWithRetry(QueryType.IP, ip, bp, 10, 5);
                    if (res.getMixIpDomainList() != null) {
                        return res.getMixIpDomainList();
                    }
                    return new ArrayList<>();
                });
            }

            // Domain任务
            for (MlDomain mlDomain : info.getDomainList()) {
                domainTasks.add(() -> {
                    String domain = mlDomain.getDomain();
                    IpBreakpoint bp = new IpBreakpoint();
                    MixIpRes res = mixIpImpl.mixSingleWithRetry(QueryType.DOMAIN, domain, bp, 10, 5);
                    if (res.getMixDomainIpList() != null) {
                        return res.getMixDomainIpList();
                    }
                    return new ArrayList<>();
                });
            }

            // 执行IP任务 + 手工获取结果
            List<MixIpDomain> ipResultList = new ArrayList<>();
            List<Future<List<MixIpDomain>>> ipFutures = executor.invokeAll(ipTasks);
            for (Future<List<MixIpDomain>> future : ipFutures) {
                if (!future.isCancelled()) {
                    List<MixIpDomain> data = future.get();
                    if (data != null && !data.isEmpty()) {
                        ipResultList.addAll(data);
                    }
                }
            }

            // 执行Domain任务 + 手工获取结果
            List<MixDomainIp> domainResultList = new ArrayList<>();
            List<Future<List<MixDomainIp>>> domainFutures = executor.invokeAll(domainTasks);
            for (Future<List<MixDomainIp>> future : domainFutures) {
                if (!future.isCancelled()) {
                    List<MixDomainIp> data = future.get();
                    if (data != null && !data.isEmpty()) {
                        domainResultList.addAll(data);
                    }
                }
            }

            // =============================
            // 🔥 关键点2：主线程单线程赋值，绝对安全
            // =============================
            info.setMixIpDomainList(ipResultList);
            info.setMixDomainIpList(domainResultList);

        } catch (Exception e) {
            log.error("并发查询IP+Domain失败", e);
        } finally {
            executor.shutdown();
        }
    }
    // 指定ip列表 定时任务 —— 高可靠版，尽量不失败
    public String specifyIpQuery(List<String> ipList) {
        long start = System.currentTimeMillis();

        ExecutorService executor = new ThreadPoolExecutor(
                3, 3, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            // =============================
            // 🔥 关键点1：任务只返回数据，不addAll
            // =============================
            List<Callable<List<MixIpDomain>>> allTasks = new ArrayList<>();
            for (String ip : ipList) {
                allTasks.add(() -> {
                    if (IpUtil.isInternalIp(ip)) {
                        log.debug("内网IP跳过查询: {}", ip);
                        return new ArrayList<>();
                    }
                    IpBreakpoint bp = new IpBreakpoint();
                    MixIpRes res = mixIpImpl.mixSingleWithRetry(QueryType.IP, ip, bp, 30, 10);
                    if (res.getMixIpDomainList() != null) {
                        return res.getMixIpDomainList();
                    }
                    return new ArrayList<>();
                });
            }

            // =============================
            // 🔥 关键点2：主线程统一合并，绝对不丢
            // =============================
            List<MixIpDomain> finalList = new ArrayList<>();
            List<Future<List<MixIpDomain>>> futures = executor.invokeAll(allTasks, 40, TimeUnit.MINUTES);

            for (Future<List<MixIpDomain>> future : futures) {
                try {
                    if (!future.isCancelled()) {
                        List<MixIpDomain> data = future.get();
                        if (data != null && !data.isEmpty()) {
                            finalList.addAll(data);
                        }
                    }
                } catch (Exception e) {
                    log.warn("IP任务执行被中断", e);
                }
            }

            // 入库
            long second = System.currentTimeMillis();
            specifyBatchUpsert(finalList);

            return (second - start) + "-" + (System.currentTimeMillis() - second);

        } catch (Exception e) {
            log.error("IP并发查询整体异常", e);
            return "error-" + (System.currentTimeMillis() - start);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
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