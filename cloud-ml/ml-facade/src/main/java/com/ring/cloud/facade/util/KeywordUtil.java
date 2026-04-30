package com.ring.cloud.facade.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 关键词工具类
 */
@Slf4j
public class KeywordUtil {

    /**
     * 构建各大搜索引擎下拉词URL
     * @param keyword 关键词
     * @param site 站点：bing.com/baidu.com/so.com/google.com/yandex.com
     * @return 接口URL
     */
    public static String buildSuggestUrl(String keyword, String site) {
        //bing国际版，必须放在最前面或者bing之前
        if (site.contains("bingint.com")) {
            return "https://www.bing.com/AS/Suggestions?pt=page.home&csr=1&pths=1&cp=2&cvid=1&qry=" + keyword;
        }
        if (site.contains("baidu")) {
            // 浏览器原生：无固定cb，最防风控
            return "https://suggestion.baidu.com/su?wd=" + keyword;
        }
        // 360 首页真实接口（浏览器原生，防风控）
        if (site.contains("so.com")) {
            return "https://sug.so.360.cn/suggest?encodein=utf-8&encodeout=utf-8&format=json&src=so_home&word=" + keyword;
        }
        if (site.contains("bing")) {
            return "https://cn.bing.com/AS/Suggestions?pt=page.home&csr=1&pths=1&cp=2&cvid=1&qry=" + keyword;
        }
        if (site.contains("google")) {
            return "https://suggestqueries.google.com/complete/search?client=chrome&q=" + keyword;
        }
        if (site.contains("yandex")) {
            return "https://yandex.com/suggest/suggest-ya.cgi?srv=morda_com_desktop&wiz=TrWth&uil=en&fact=1&v=4&icon=1&lr=98538&hl=1&bemjson=0&history=1&html=1&platform=desktop&rich_nav=1&show_experiment=224&verified_nav=1&rich_phone=1&use_favicon=1&nav_favicon=1&nav_text=1&mt_wizard=1&suggest_entity_desktop=1&entity_enrichment=1&entity_alignment_mode=bottom&sn=6&maybe_ads=1&yu=8092725341777293573&entity_max_count=1&svg=1&part=" + keyword + "&pos=5";
        }
        if (site.contains("sogou")) {
            return "https://qbbusi.html5.qq.com/smartbox/GetAssociateData";
        }
        return null;
    }
}