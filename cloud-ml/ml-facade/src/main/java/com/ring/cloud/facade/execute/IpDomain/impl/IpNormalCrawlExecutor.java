package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.facade.config.IpRecordWriter;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpPageResponse;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class IpNormalCrawlExecutor extends IpBaseExecutor {

    @Autowired
    private IpRecordWriter ipRecordWriter;

    public IpReadInfo crawlIp(BufferedWriter bw, IpTaskEntity ipTaskEntity, String currentIp, ProxyIp proxy, IpBreakpoint breakpoint) throws IOException {
        IpReadInfo finalReadInfo = new IpReadInfo();
        boolean hasMore = false;

        String tempXml = ipFirstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl));
        IpReadInfo tempInfo = parseTokenAndLoc(tempXml);

        // 首页
        IpReadInfo firstInfo;
        if (breakpoint.getCurrentPage() == 1) {
            firstInfo = parseFirstPageHtml(bw, tempXml, currentIp);
            try {
                if (firstInfo.getBatchSb().length() > 0) {
                    batchWrite(bw, firstInfo.getBatchSb());
                }
                breakpoint.setCurrentCount(firstInfo.getPageSize());
                if (firstInfo.getPageSize() < 100) {
                    finalReadInfo.setSuccess(true);
                    return finalReadInfo;
                }

                breakpoint.setCurrentPage(2);
                hasMore = true;

            } finally {
                tempXml = null;
                firstInfo.setBatchSb(null);
            }
        } else {
            firstInfo = tempInfo;
            hasMore = true;
        }

        // 翻页
        while (hasMore) {
            int page = breakpoint.getCurrentPage();
            if (page % 100 == 0) {
                log.info("[{}] 页码：{}", currentIp, page);
            }
            int size = parseSinglePage(bw, currentIp, firstInfo.getLoc(), page, firstInfo.getToken(), proxy);
            breakpoint.setCurrentCount(breakpoint.getCurrentCount()+size);
            breakpoint.setCurrentPage(page + 1);
            if (size < 100) {
                hasMore = false;
            }
        }

        finalReadInfo.setSuccess(true);
        return finalReadInfo;
    }

    private boolean isLargeIp(String ip, ProxyIp proxy, String token) {
        int p1 = ThreadLocalRandom.current().nextInt(180, 221);
        String url1 = IpUtil.buildIpUrlPage(p1, ip, ipDomainUrl, ipPageInfix, token);
        IpPageResponse r1 = ipNextPage(proxy, url1, token);

        // p1无数据 → 确实是小IP
        if (CollectionUtils.isEmpty(r1.getData())) {
            return false;
        }

        // p1有数据，才探测p2
        int p2 = ThreadLocalRandom.current().nextInt(450, 551);
        String url2 = IpUtil.buildIpUrlPage(p2, ip, ipDomainUrl, ipPageInfix, token);
        IpPageResponse r2 = ipNextPage(proxy, url2, token);

        return !CollectionUtils.isEmpty(r2.getData());
    }
}