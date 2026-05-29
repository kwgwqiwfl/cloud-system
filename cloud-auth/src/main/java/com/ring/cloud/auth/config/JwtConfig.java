package com.ring.cloud.auth.config;

import com.ring.cloud.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:ring1234567890}")
    private String secret;

    @Value("${jwt.expire:86400000}") // 默认1天 毫秒
    private long expire;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expire);
    }
}