package com.ring.cloud.facade.execute.IpDomain.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.execute.IpDomain.IpBaseExecutor;
import com.ring.cloud.facade.frame.OkProxyKeyword;
import com.ring.cloud.facade.frame.OkProxyPostKeyword;
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
    @Autowired
    protected OkProxyPostKeyword okProxyPostKeyword;

    public Set<String> execute(String keyword, String site, ProxyIp proxy) {
        Set<String> resultSet = new LinkedHashSet<>();

        try {
            String url = KeywordUtil.buildSuggestUrl(keyword, site);
            String response;
            if (site.contains("sogou")) {
                // 1. 构造搜狗需要的 JSON
                String jsonBody = buildSogouJson(proxy.getIp(), proxy.getPort(), keyword);
                // 2. 调用 POST 类
                response = okProxyPostKeyword.doProxyPostRequest(proxy.getIp(), proxy.getPort(), url, jsonBody, "");
            } else {
                response = okProxyKeyword.doProxyRequest(proxy.getIp(), proxy.getPort(), url, "");
            }

            if (response == null || StringUtils.isEmpty(response)) {
                log.error("[下拉查询] 返回为空 keyword={} site={}", keyword, site);
                return resultSet;
            }

            // 根据站点分发到独立解析方法
            if (site.contains("baidu")) {
                parseBaidu(response, resultSet);
            } else if (site.contains("sogou")) {
                parseSogou(response, resultSet);
            }else if (site.contains("so.com")) {
                parseSo(response, resultSet);
            } else if (site.contains("bing") || site.contains("bingint")) {
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

    // 搜狗 独立解析
    private void parseSogou(String response, Set<String> resultSet) {
        try {
            JSONObject root = JSON.parseObject(response);
            // 安全获取 data -> items
            JSONObject data = root.getJSONObject("data");
            if (data == null) return;
            JSONArray items = data.getJSONArray("items");
            if (items == null || items.isEmpty()) return;

            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item == null) continue;

                // 安全获取，防止空指针
                JSONObject card = item.getJSONObject("card");
                if (card == null) continue;
                JSONObject sugCard = card.getJSONObject("sug_card");
                if (sugCard == null) continue;

                String word = sugCard.getString("word");
                if (word == null || word.isEmpty()) continue;

                String clean = word.replaceAll("[\\p{Cntrl}\\p{Co}]", "").trim();
                if (!clean.isEmpty()) {
                    resultSet.add(clean);
                }
            }
        } catch (Exception e) {
            log.error("Sogou解析失败", e);
        }
    }
    //构造搜狗固定 JSON
    /**
     * 核心规则：
     * 同一个代理IP+端口 → 永远同一个 user_id
     * 换代理 → 自动换 user_id
     * 关键词不影响
     */
    private String buildSogouJson(String proxyHost, int proxyPort, String keyword) {
        long timestamp = System.currentTimeMillis();

        // 【唯一绑定：代理IP + 端口】
        String proxyKey = proxyHost + ":" + proxyPort;
        int proxyHash = Math.abs(proxyKey.hashCode());
        String userId = "sogou_proxy_" + proxyHash;

        return "{"
                + "\"header\":{"
                +   "\"session\":{\"time\":\"" + timestamp + "\"},"
                +   "\"user_info\":{"
                +     "\"guid\":\"\","
                +     "\"qimei36\":\"\","
                +     "\"user_id\":\"" + userId + "\","
                +     "\"qua2\":\"\","
                +     "\"user_agent_pc\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36\","
                +     "\"rn_version\":\"1.5.11\""
                +   "}"
                + "},"
                + "\"data\":{"
                +   "\"req_id\":1890571885,"
                +   "\"query\":\"" + keyword + "\","
                +   "\"source\":{\"page_name\":\"sgsearch\"}"
                + "}"
                + "}";
    }
}