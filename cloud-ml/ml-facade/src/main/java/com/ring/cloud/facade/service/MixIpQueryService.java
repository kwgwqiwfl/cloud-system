package com.ring.cloud.facade.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MlDomain;
import com.ring.cloud.core.pojo.MlIcp;
import com.ring.cloud.core.pojo.MlIp;
import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.cloud.core.service.MlDomainService;
import com.ring.cloud.core.service.MlIcpService;
import com.ring.cloud.core.service.MlIpService;
import com.ring.cloud.core.service.MlSubdomainService;
import com.ring.cloud.facade.entity.ip.MixIpInfo;
import com.ring.cloud.facade.execute.IpDomain.impl.MixIpExecutor;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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


    @Transactional(rollbackFor = Exception.class)
    public void autoUpdateIpStatistics() {
        long start = System.currentTimeMillis();
        log.info("定时任务开始");

        MixIpInfo info = queryMix();

        if (info == null) {
            log.warn("定时任务已取消：5次代理查询均失败，本次不执行更新");
            return;
        }

        if (info.getIpList()!=null) {
            mlIpService.batchSaveOrUpdate(info.getIpList());
        }
        if (info.getDomainList()!=null) {
            mlDomainService.batchSaveOrUpdate(info.getDomainList());
        }
        if (info.getIcpList()!=null) {
            mlIcpService.batchSaveOrUpdate(info.getIcpList());
        }
        if (info.getSubdomainList()!=null) {
            mlSubdomainService.batchSaveOrUpdate(info.getSubdomainList());
        }

        long cost = System.currentTimeMillis() - start;
        log.info("定时任务完成，耗时: {}ms", cost);
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
                log.warn("queryMixInfo 失败，重试次数: {}，错误信息: {}", retryCount, e.getMessage());

                if (retryCount >= maxRetry) {
                    log.error("queryMixInfo 重试{}次全部失败，停止查询", maxRetry);
                    break;
                }
            }
        }

        return null;
    }

}