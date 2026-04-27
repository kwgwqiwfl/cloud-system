package com.ring.cloud.facade.execute.IpDomain.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.util.KeywordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class KeywordExecutor extends IpBaseExecutor {

    public Set<String> execute(String keyword, String site, ProxyIp proxy) {
        Set<String> resultSet = new LinkedHashSet<>();

        try {
            String url = KeywordUtil.buildSuggestUrl(keyword, site);
            String response = okProxyIp.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");

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
        int start = response.indexOf("[");
        int end = response.indexOf("]");
        String dataArray = response.substring(start, end + 1).replace("\"", "");
        String[] words = dataArray.split(",");
        for (String word : words) {
            if (word != null && !word.equals("")) {
                resultSet.add(word.trim());
            }
        }
    }

    // 360so 独立解析
    private void parseSo(String response, Set<String> resultSet) {
        List<String> list = JSON.parseArray(response, String.class);
        if (list != null && list.size() > 0) {
            resultSet.addAll(list);
        }
    }

    // Bing 独立解析
    private void parseBing(String response, Set<String> resultSet) {
        JSONArray jsonArray = JSON.parseArray(response);
        if (jsonArray != null && jsonArray.size() > 1) {
            JSONArray suggestArray = jsonArray.getJSONArray(1);
            if (suggestArray != null) {
                for (int i = 0; i < suggestArray.size(); i++) {
                    String word = suggestArray.getString(i);
                    if (word != null && !word.equals("")) {
                        resultSet.add(word.trim());
                    }
                }
            }
        }
    }

    // Google 独立解析
    private void parseGoogle(String response, Set<String> resultSet) {
        JSONArray jsonArray = JSON.parseArray(response);
        if (jsonArray != null && jsonArray.size() > 1) {
            JSONArray suggestArray = jsonArray.getJSONArray(1);
            if (suggestArray != null) {
                for (int i = 0; i < suggestArray.size(); i++) {
                    String word = suggestArray.getString(i);
                    if (word != null && !word.equals("")) {
                        resultSet.add(word.trim());
                    }
                }
            }
        }
    }

    // Yandex 独立解析
    private void parseYandex(String response, Set<String> resultSet) {
        JSONArray jsonArray = JSON.parseArray(response);
        if (jsonArray != null && jsonArray.size() > 1) {
            JSONArray suggestArray = jsonArray.getJSONArray(1);
            if (suggestArray != null) {
                for (int i = 0; i < suggestArray.size(); i++) {
                    String word = suggestArray.getString(i);
                    if (word != null && !word.equals("")) {
                        resultSet.add(word.trim());
                    }
                }
            }
        }
    }
}