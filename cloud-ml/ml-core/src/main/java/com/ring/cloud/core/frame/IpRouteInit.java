package com.ring.cloud.core.frame;

import com.ring.cloud.core.pojo.IpRouteConfig;
import com.ring.cloud.core.service.IpRouteService;
import com.ring.welkin.common.core.Initializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 初始化ip路由信息（全局缓存，使用时直接从内存取）
 */
@Slf4j
@Component
public class IpRouteInit implements Initializer {

    @Autowired
    private IpRouteService ipRouteService;

    // ====================== 全局静态缓存，随便调用 ======================
    /**
     * key: ip
     * value: 表后缀 01/02/03...
     */
    public static final Map<String, String> IP_TABLE_MAP = new ConcurrentHashMap<>();

    /**
     * key: ip
     * value: 数据档位 1=10万
     */
    public static final Map<String, Integer> IP_LEVEL_MAP = new ConcurrentHashMap<>();

    @Override
    public void init() {
//        // 1. 查库
//        List<IpRouteConfig> ipRouteConfigList = ipRouteService.ipRouteList();
//        log.info("ip路由初始化完成，共 {} 条配置", ipRouteConfigList.size());
//
//        // 2. 清空旧数据
//        IP_TABLE_MAP.clear();
//        IP_LEVEL_MAP.clear();
//
//        // 3. 放入内存
//        for (IpRouteConfig config : ipRouteConfigList) {
//            String ip = config.getIp();
//            String suffix = config.getTableSuffix();
//            Integer level = config.getDataCount();
//
//            // 只存有效数据
//            if (ip != null && suffix != null) {
//                IP_TABLE_MAP.put(ip, suffix);
//                IP_LEVEL_MAP.put(ip, level);
//            }
//        }
    }
    /**
     * 统一获取IP路由表后缀（兼容：精准IP优先 + 第一段兜底）
     * 所有业务全部调用这一个方法！
     */
    public static String getTableSuffix(String ip) {
        if (ip == null || ip.isEmpty()) {
            throw new RuntimeException("IP不能为空");
        }

        // 1. 先查精准完整IP
        String suffix = IP_TABLE_MAP.get(ip);
        if (suffix != null) {
            return suffix;
        }

        // 2. 查不到 → 截取第一段再查
        try {
            String firstSegmentStr = ip.split("\\.")[0];
            suffix = IP_TABLE_MAP.get(firstSegmentStr);
        } catch (Exception e) {
            log.error("IP格式解析失败: {}", ip, e);
            throw new RuntimeException("IP格式错误: " + ip);
        }

        // 3. 都没有 → 抛出异常
        if (suffix == null) {
            throw new RuntimeException("未配置任何IP路由规则: " + ip);
        }

        return suffix;
    }
    //同步ip路由到内存
    public static void syncIpRoute(IpRouteConfig config) {
        if (config.getIp() == null || config.getTableSuffix() == null) {
            throw new RuntimeException("参数不全, 请检查");
        }
        IP_TABLE_MAP.put(config.getIp(), config.getTableSuffix());
        IP_LEVEL_MAP.put(config.getIp(), config.getDataCount() == null ? 0 : config.getDataCount());
    }

}