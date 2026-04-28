package com.ring.cloud.facade.execute.IpDomain.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.frame.OkProxyKeyword;
import com.ring.cloud.facade.util.KeywordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class KeywordExecutor extends IpBaseExecutor {

    @Autowired
    protected OkProxyKeyword okProxyKeyword;

    public Set<String> execute(String keyword, String site, ProxyIp proxy) {
        Set<String> resultSet = new LinkedHashSet<>();

        try {
            String url = KeywordUtil.buildSuggestUrl(keyword, site);
            String response = okProxyKeyword.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");

            if (response == null || StringUtils.isEmpty(response)) {
                log.error("[下拉查询] 返回为空 keyword={} site={}", keyword, site);
                return resultSet;
            }

            // 根据站点分发到独立解析方法
            if (site.contains("baidu")) {
                parseBaidu(response, resultSet);
            } else if (site.contains("so.com")) {
                parseSo(response, resultSet);
            } else if (site.contains("bing")) {
                parseBing(response, resultSet);
            } else if (site.contains("google")) {
                parseGoogle(response, resultSet);
            } else if (site.contains("yandex")) {
                parseYandex(response, resultSet);
            }

            log.info("[下拉查询] site={} keyword={} 结果数={}", site, keyword, resultSet.size());
        } catch (Throwable e) {
            throw new IllegalArgumentException("查询失败，"+e.getMessage());
        }
        return resultSet;
    }

    // 百度独立解析
    private void parseBaidu(String response, Set<String> resultSet) {
        // 定位 s: 后面的数组
        int sIndex = response.indexOf("s:");
        if (sIndex == -1) {
            return;
        }

        int start = response.indexOf("[", sIndex);
        int end = response.indexOf("]", start);
        if (start == -1 || end == -1 || start >= end) {
            return;
        }

        // 截取数组内容
        String dataArray = response.substring(start + 1, end);
        String[] words = dataArray.split("\",\"");

        for (String word : words) {
            if (word != null) {
                String clean = word.replace("\"", "").trim();
                resultSet.add(clean);
            }
        }
    }

    // 360 最终正确解析
    private void parseSo(String response, Set<String> resultSet) {
        try {
            JSONObject root = JSON.parseObject(response);
            JSONArray array = root.getJSONArray("result");

            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                String word = item.getString("word");
                resultSet.add(word);
            }
        } catch (Exception e) {
            log.error("360解析失败", e);
        }
    }

    // Bing 独立解析
    private void parseBing(String response, Set<String> resultSet) {
        try {
            JSONObject root = JSON.parseObject(response);
            JSONArray array = root.getJSONArray("s");

            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                String raw = item.getString("q");
                if (raw == null || raw.isEmpty()) {
                    continue;
                }
                // 清理所有非可见的控制字符和私有区乱码
                String clean = raw.replaceAll("[\\p{Cntrl}\\p{Co}]", "").trim();
                if (!clean.isEmpty()) {
                    resultSet.add(clean);
                }
            }
        } catch (Exception e) {
            log.error("Bing解析失败", e);
        }
    }

    // Google 独立解析
    private void parseGoogle(String response, Set<String> resultSet) {
        try {
            JSONArray root = JSON.parseArray(response);
            JSONArray words = root.getJSONArray(1);
            for (int i = 0; i < words.size(); i++) {
                String word = words.getString(i);
                resultSet.add(word.trim());
            }
        } catch (Exception e) {
            log.error("Google解析失败", e);
        }
    }

    // Yandex 独立解析
    private void parseYandex(String response, Set<String> resultSet) {
        try {
            JSONArray root = JSON.parseArray(response);
            JSONArray suggestList = root.getJSONArray(1);

            for (int i = 0; i < suggestList.size(); i++) {
                JSONArray item = suggestList.getJSONArray(i);
                String word = item.getString(1);
                if (word != null && !word.isEmpty()) {
                    resultSet.add(word.trim());
                }
            }
        } catch (Exception e) {
            log.error("Yandex解析失败", e);
        }
    }
}