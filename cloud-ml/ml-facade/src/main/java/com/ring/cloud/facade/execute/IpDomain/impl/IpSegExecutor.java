package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.core.pojo.SourceIpDomain;
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
public class IpSegExecutor extends IpBaseExecutor {

    @Override
    protected Object buildHomeItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new SourceIpDomain(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected Object buildPageItem(String ip, String loc, String domain, String adTime, String upTime) {
        return new SourceIpDomain(ip, loc, domain, adTime, upTime);
    }

    @Override
    protected void processPageData(List<?> list, BufferedWriter bw, IpBreakpoint breakpoint) throws IOException {
        if (list == null || list.isEmpty()) return;
        writeIpDomainCsv(bw, (List<SourceIpDomain>) list);
    }

    public boolean execute(String currentIp, BufferedWriter bw, ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo info = executeIpCrawl(currentIp, proxy, bw, breakpoint, 10);
        if(info==null)
            throw new IllegalArgumentException("info为空");
        return info.isSuccess();
    }
}