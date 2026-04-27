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
        if (site.contains("baidu")) {
            return "https://suggestion.baidu.com/su?wd=" + keyword + "&cb=j";
        }
        if (site.contains("so.com")) {
            return "https://sugs.so.com/suggest?q=" + keyword;
        }
        if (site.contains("bing")) {
            return "https://www.bing.com/AS/Suggestions?qry=" + keyword;
        }
        if (site.contains("google")) {
            return "https://suggestqueries.google.com/complete/search?client=chrome&q=" + keyword;
        }
        if (site.contains("yandex")) {
            return "https://suggest.yandex.ru/suggest?text=" + keyword;
        }
        return null;
    }
}