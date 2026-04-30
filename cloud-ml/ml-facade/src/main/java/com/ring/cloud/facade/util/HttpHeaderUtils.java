package com.ring.cloud.facade.util;

import okhttp3.*;
public class HttpHeaderUtils {
    // 通用头
    public static final Headers COMMON = Headers.of(
            "Accept-Encoding", "gzip, deflate, br, zstd",
            "Accept-Language", "zh-CN,zh;q=0.9",
            "Connection", "close"
    );

    // 百度
    public static final Headers BAIDU = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Upgrade-Insecure-Requests", "1",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "same-origin",
            "Cache-Control", "max-age=0",
            "Sec-Ch-Ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Referer", "https://www.baidu.com"
    );

    // 360
    public static final Headers SO_COM = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Accept", "*/*",
            "Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Sec-Fetch-Dest", "script",
            "Sec-Fetch-Mode", "no-cors",
            "Sec-Fetch-Site", "cross-site",
            "Referer", "https://www.so.com/?src=so.com"
    );

    // Bing
    public static final Headers BING = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Accept", "*/*",
            "Referer", "https://cn.bing.com/",
            "Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "same-origin"
    );

    // 搜狗 Sogou 官方原生接口
    public static final Headers SOGOU = Headers.of(
            "Accept", "application/json, text/plain, */*",
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Origin", "https://www.sogou.com",
            "Referer", "https://www.sogou.com/",  // ✅ 这个保留，正确
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "cross-site",      // ✅ 必须改成 cross-site
            "sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"Windows\""
    );

    // Yandex
    public static final Headers YANDEX = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Accept", "*/*",
            "Referer", "https://yandex.com/",
            "Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "same-origin"
    );

    // Google
    public static final Headers GOOGLE = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Accept", "*/*",
            "Referer", "https://www.google.com/",
            "Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "same-origin"
    );

    // Bing 国际版
    public static final Headers BING_INT = Headers.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Accept", "*/*",
            "Referer", "https://www.bing.com/",
            "Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "Sec-Ch-Ua-Mobile", "?0",
            "Sec-Ch-Ua-Platform", "\"Windows\"",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "same-origin"
    );
}
