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
import java.io.IOException;

@Slf4j
@Component
public class IpSmallExecutor extends IpBaseExecutor {

    /**
     * 小ip
     */
    public boolean singleIp(String loc, String currentIp, ProxyIp proxy,
                                            BufferedWriter bw, IpBreakpoint breakpoint) throws IOException {
        boolean hasMore = true;
        String token;
        // ====================== 首页处理 ======================
        if (breakpoint.getCurrentPage() == 1) {
            IpReadInfo firstInfo = fetchAndParseFirstPage(bw, currentIp, proxy);
            token = firstInfo.getToken();
            breakpoint.setCurrentCount(firstInfo.getPageSize());
            if(firstInfo.getPageSize()<100){//第一页不足100条 任务结束
                return true;
            }
            breakpoint.setCurrentPage(2);
        } else {
            String xmlContent = ipFirstPage(proxy, IpUtil.buildIpUrlFirst(currentIp, ipDomainUrl));
            Document doc = Jsoup.parse(xmlContent);
            Elements scriptElements = doc.select("script[type='text/javascript']");
            token = parseToken(scriptElements);
        }

        // ====================== 循环爬取 ======================
        while (hasMore) {
            int currentPage = breakpoint.getCurrentPage(); // 从断点拿
            int size = parseSinglePage(bw, currentIp, loc, currentPage, token, proxy);
            breakpoint.setCurrentCount(breakpoint.getCurrentCount() + size);
            if(size<100){//当前页不足100 任务结束
                hasMore = false;
            }
            breakpoint.setCurrentPage(currentPage + 1);
        }
        return true;
    }
}
