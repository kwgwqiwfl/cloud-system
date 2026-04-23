package com.ring.cloud.facade.process.metrics.impl;

import com.ring.cloud.core.pojo.MlDomainAi;
import com.ring.cloud.facade.common.QueryType;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.entity.ip.*;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.impl.MixIpExecutor;
import com.ring.cloud.facade.process.ip.StopCondition;
import com.ring.cloud.facade.process.metrics.AbstractTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MixIpImpl extends AbstractTask<MixIpInfo> implements StopCondition {

    @Override
    public TaskTypeEnum taskEnum() {
        return TaskTypeEnum.DEFAULT;
    }

    @Autowired
    private MixIpExecutor mixIpExecutor;

    /**
     * 查询ai domain
     */
    public List<MlDomainAi> queryDomainAi() {
        return queryWithRetry(
                null,
                null,
                7,
                (key, proxy) -> mixIpExecutor.queryDoaminAi(),
                (key) -> null
        );
    }
    /**
     * 统一查询所有IP/域名/ICP/子域名数据
     */
    public MixIpInfo queryMix() {
        return queryWithRetry(
                null,
                null,
                5,
                (key, proxy) -> mixIpExecutor.queryMixInfo(),
                (key) -> null
        );
    }
//查询扩展信息 ip查询 和domain查询
    /**
     * 重试查询（总次数 + 代理异常次数 由外部传入）
     * @param type          查询类型
     * @param key           IP或域名
     * @param breakpoint    断点
     * @param maxTotalTry   最大总尝试次数
     * @param maxProxyRetry 最大代理异常重试次数
     * @return 结果
     */
    public MixIpRes mixSingleWithRetry(QueryType type, String key, IpBreakpoint breakpoint,
                                        int maxTotalTry, int maxProxyRetry) {
        int totalTry = 0;
        int proxyRetry = 0;
        ProxyIp currentProxy = null;
        MixIpRes mixIpRes = new MixIpRes();

        while (totalTry < maxTotalTry) {
            totalTry++;
            try {
                if (currentProxy == null) {
                    currentProxy = globalProxyHelper.getAvailableProxy();
                }

                if (type == QueryType.IP) {
                    mixIpRes = mixIpExecutor.extByIp(key, currentProxy, breakpoint, mixIpRes);
                } else {
                    mixIpRes = mixIpExecutor.extByDomain(key, currentProxy, breakpoint, mixIpRes);
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
    // ========== StopCondition 接口实现 ==========
    @Override
    public boolean shouldStop(String currentIp, String endIp) {
        return false;
    }

}