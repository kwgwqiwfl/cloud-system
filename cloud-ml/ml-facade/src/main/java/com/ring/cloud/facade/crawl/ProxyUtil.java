package com.ring.cloud.facade.crawl;

import com.alibaba.fastjson.JSON;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ring.cloud.facade.entity.proxy.ProxyData;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.entity.proxy.ProxyResponse;
import com.ring.cloud.facade.util.FileUtil;
import com.ring.cloud.facade.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代理获取工具类
 */
@Slf4j
public class ProxyUtil {
    public static void main(String[] args) {
        String url = "http://v2.api.juliangip.com/unlimited/getips?auto_white=1&num=10&pt=1&result_type=text&split=1&trade_no=5146320972443658&sign=6004c288e00c37af81095e47defee8d7";
        getProxy(url);
    }
    public static ProxyResponse getProxy(String url) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(5000);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 解决中文乱码问题
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        String str = restTemplate.getForObject(url, String.class);
        List<String> list = Stream.of(str.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        System.out.println("...."+list);
        return null;
    }

    //转代理ip
    public static List<ProxyIp> parseIp(ProxyResponse pr) {
        return pr.getData().getProxy_list().stream()
                .map(ProxyUtil::convertIp)
                .collect(Collectors.toList());
    }

    public static ProxyIp convertIp(String ipStr) {
        ProxyIp proxyIp = new ProxyIp();
        String[] ipStrs = ipStr.split(":");
        proxyIp.setIp(ipStrs[0]);
        proxyIp.setPort(Integer.parseInt(ipStrs[1]));
        return proxyIp;
    }

}
