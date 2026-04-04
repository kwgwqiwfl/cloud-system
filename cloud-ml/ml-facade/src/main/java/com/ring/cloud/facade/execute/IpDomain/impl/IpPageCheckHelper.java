package com.ring.cloud.facade.execute.IpDomain.impl;

import com.ring.cloud.facade.entity.ip.IpPageResponse;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class IpPageCheckHelper extends IpBaseExecutor {

    private static final int MAX_RETRY = 3;

    public boolean checkHasDataWithRetry(String ip, int page) {
        ProxyIp proxy = null;
        boolean result = false;

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                proxy = globalProxyHelper.getAvailableProxy();
                if (proxy == null) {
                    log.warn("[探测重试] 无可用代理，重试={} ip={}", retry, ip);
                    continue;
                }

                String token = getToken(ip, proxy);
                if (token == null) {
                    log.warn("[探测重试] Token获取失败，重试={} ip={}", retry, ip);
                    continue;
                }

                String url = IpUtil.buildIpUrlPage(page, ip, ipDomainUrl, ipPageInfix, token);
                IpPageResponse response = ipNextPage(proxy, url, token);
                result = !CollectionUtils.isEmpty(response.getData());

                return result;

            } catch (Throwable e) {
                log.warn("[探测异常] 重试={} ip={} page={} 异常:{}",
                        retry, ip, page, e.getMessage());

                continue;
            }
        }

        log.warn("[探测最终失败] ip={} page={}", ip, page);
        return false;
    }
    public Map<String, Object> checkHasDataAndLoc(String ip, int page) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasData", false);
        result.put("loc", null);

        ProxyIp proxy = null;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                proxy = globalProxyHelper.getAvailableProxy();
                if (proxy == null) continue;

                // -------------- 你本来就要发的 FIRST 页面 --------------
                String firstUrl = IpUtil.buildIpUrlFirst(ip, ipDomainUrl);
                String xml = ipFirstPage(proxy, firstUrl);
                String token = parseToken(xml);
                if (token == null) continue;

                // -------------- 只在这里多解析一句 loc --------------
                String loc = Jsoup.parse(xml).getElementsByClass("result result2").select("h3").text();

                String url = IpUtil.buildIpUrlPage(page, ip, ipDomainUrl, ipPageInfix, token);
                IpPageResponse response = ipNextPage(proxy, url, token);
                boolean hasData = !CollectionUtils.isEmpty(response.getData());

                result.put("hasData", hasData);
                result.put("loc", loc);
                return result;

            } catch (Throwable e) {
                log.warn("[探测异常] retry={} ip={}", retry, ip, e);
            }
        }
        return result;
    }

    public String getToken(String ip, ProxyIp proxy) {
        try {
            String firstUrl = IpUtil.buildIpUrlFirst(ip, ipDomainUrl);
            String xml = ipFirstPage(proxy, firstUrl);
            return parseToken(xml);
        } catch (Exception e) {
            log.warn("[getToken异常] ip={}", ip, e);
            return null;
        }
    }

    private String parseToken(String xml) {
        Document doc = Jsoup.parse(xml);
        return parseToken(doc.select("script[type='text/javascript']"));
    }
}
