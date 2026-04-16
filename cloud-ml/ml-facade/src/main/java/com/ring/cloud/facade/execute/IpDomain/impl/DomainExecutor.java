package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.core.pojo.DomainInout;
import com.ring.cloud.core.pojo.MixDomainIp;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class DomainExecutor extends IpBaseExecutor {
    //in域名-子域名
    public boolean executeSubdomain(String domain, ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        List<String> list = executeSubdomainCrawl(domain, proxy, breakpoint, 6);
        breakpoint.getSet().addAll(list);
        return true;
    }
    //子域名-ip
    public boolean executeDomain(String domain, ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        List<MixDomainIp> list = executeDomainCrawl(domain, proxy, breakpoint, 10, MixDomainIp::new);
        list.stream()
                .map(MixDomainIp::getIp)
                .forEach(breakpoint.getSet()::add);
        return true;
    }
    //ip-out域名
    public boolean execute(String ip, ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo info = executeIpCrawl(ip, proxy, null, breakpoint, 10);
        if(info==null)
            throw new IllegalArgumentException("info为空");
        return info.isSuccess();
    }
    @Override
    protected Object buildHomeItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new DomainInout(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected Object buildPageItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new DomainInout(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected void processPageData(List<?> list, BufferedWriter bw, IpBreakpoint breakpoint) {
        if (list == null || list.isEmpty()) return;
        breakpoint.getList().addAll(list);
    }
}
