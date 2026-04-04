package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.ip.IpReadInfo;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;

@Slf4j
@Component
public class IpLargeIpExecutor extends IpBaseExecutor {

    /**
     * 大IP 分段执行
     * 无线程、无池、无分段、纯同步
     */
    public boolean crawlSegment(String loc, String currentIp, ProxyIp proxy,
                                            BufferedWriter bw, int startPage, int endPage, IpBreakpoint breakpoint) {
        String token;
        try {
            // ====================== 首页处理 ======================
            if (breakpoint.getCurrentPage() == 1) {
                IpReadInfo firstInfo = fetchAndParseFirstPage(bw, currentIp, proxy);
                token = firstInfo.getToken();
                breakpoint.setCurrentCount(firstInfo.getPageSize());
                breakpoint.setCurrentPage(2);
            } else {
                String xmlContent = ipFirstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl));
                Document doc = Jsoup.parse(xmlContent);
                Elements scriptElements = doc.select("script[type='text/javascript']");
                token = parseToken(scriptElements);
            }

            // ====================== 循环爬取 ======================
            while (breakpoint.getCurrentPage() <= endPage) {
                int currentPage = breakpoint.getCurrentPage(); // 从断点拿
                int size = parseSinglePage(bw, currentIp, loc, currentPage, token, proxy);
                breakpoint.setCurrentCount(breakpoint.getCurrentCount() + size);
                breakpoint.setCurrentPage(currentPage + 1);
            }
            return true;
        } catch (Throwable e) {
            // ====================== 异常：记录真实断点页 ======================
            log.warn("[大IP异常] {} 断点页 = {}", currentIp, breakpoint.getCurrentPage());
            return false;
        }
    }
}
