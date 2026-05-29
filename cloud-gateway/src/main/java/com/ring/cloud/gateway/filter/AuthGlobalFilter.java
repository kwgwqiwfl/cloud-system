package com.ring.cloud.gateway.filter;

import com.ring.cloud.common.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@Order(-100)
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter {

    // 这里必须用 final，配合 @RequiredArgsConstructor 实现注入，不能再写 @Autowired
    private final JwtService jwtService;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 放行路径（和你的业务路径对应）
    private final List<String> ignorePaths = Arrays.asList(
            "/api/auth/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/swagger-ui/**",
            "/actuator/**",
            "/error/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 白名单路径直接放行
        for (String pattern : ignorePaths) {
            if (antPathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 获取 Token（支持三种常见头）
        String token = getToken(request);
        if (token == null) {
            return buildUnauthorizedResponse(exchange.getResponse());
        }

        // 3. 校验 Token 有效性（用你固定的 JwtService 方法）
        if (!jwtService.validateToken(token)) {
            return buildUnauthorizedResponse(exchange.getResponse());
        }

        // 4. 校验通过，继续路由
        return chain.filter(exchange);
    }

    /**
     * 构建 401 未授权响应
     */
    private Mono<Void> buildUnauthorizedResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    /**
     * 从请求头中提取 Token
     */
    private String getToken(ServerHttpRequest request) {
        // 优先从标准 Authorization 头获取（Bearer 格式）
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 兼容 X-AUTH-TOKEN 自定义头
        String xAuthToken = request.getHeaders().getFirst("X-AUTH-TOKEN");
        if (xAuthToken != null && !xAuthToken.trim().isEmpty()) {
            return xAuthToken;
        }

        // 兼容 access_token 头
        String accessToken = request.getHeaders().getFirst("access_token");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return accessToken;
        }

        return null;
    }
}