package com.ring.cloud.gateway.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ring.cloud.auth.dto.RouteDefinitionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteManager {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final String ROUTE_DATA_ID = "gateway-route.json";
    private static final String ROUTE_GROUP = "RING_CLOUD_GROUP";

    // 新增：记录当前动态路由ID
    private final Set<String> currentRouteIds = new ConcurrentHashSet<>();

    @PostConstruct
    public void initRoute() {
        try {
            String routeJson = nacosConfigManager.getConfigService()
                    .getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (routeJson != null && !routeJson.trim().isEmpty()) {
                refreshRoute(routeJson);
            }

            nacosConfigManager.getConfigService().addListener(ROUTE_DATA_ID, ROUTE_GROUP, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("【动态路由】Nacos配置变更，开始刷新路由");
                    refreshRoute(configInfo);
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
            log.info("【动态路由】Nacos路由监听器注册成功");
        } catch (Exception e) {
            log.error("【动态路由】初始化加载失败", e);
        }
    }

    private void refreshRoute(String json) {
        try {
            List<RouteDefinitionDTO> dtoList = objectMapper.readValue(json,
                    new TypeReference<List<RouteDefinitionDTO>>() {});

            // 1. 先删除上一轮所有动态路由
            currentRouteIds.forEach(id -> routeDefinitionWriter.delete(Mono.just(id)).subscribe());
            currentRouteIds.clear();

            // 2. 注册新路由并记录ID
            for (RouteDefinitionDTO dto : dtoList) {
                RouteDefinition route = buildRoute(dto);
                routeDefinitionWriter.save(Mono.just(route)).subscribe();
                currentRouteIds.add(dto.getId());
                log.info("【动态路由】加载路由: {} -> {}", dto.getId(), dto.getUri());
            }

            eventPublisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("【动态路由】路由刷新完成，共 {} 条", dtoList.size());
        } catch (Exception e) {
            log.error("【动态路由】解析/刷新失败", e);
        }
    }

    private RouteDefinition buildRoute(RouteDefinitionDTO dto) {
        RouteDefinition route = new RouteDefinition();
        route.setId(dto.getId());
        route.setUri(java.net.URI.create(dto.getUri()));
        route.setOrder(dto.getOrder() == null ? 1 : dto.getOrder());

        // 组装断言
        if (dto.getPredicates() != null) {
            dto.getPredicates().forEach(pred -> {
                PredicateDefinition pd = new PredicateDefinition();
                pd.setName(pred.getName());
                pd.setArgs(pred.getArgs());
                route.getPredicates().add(pd);
            });
        }

        // 组装过滤器
        if (dto.getFilters() != null) {
            dto.getFilters().forEach(filter -> {
                FilterDefinition fd = new FilterDefinition();
                fd.setName(filter.getName());
                fd.setArgs(filter.getArgs());
                route.getFilters().add(fd);
            });
        }

        return route;
    }
}