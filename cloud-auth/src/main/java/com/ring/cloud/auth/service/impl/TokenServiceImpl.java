package com.ring.cloud.auth.service.impl;

import com.ring.cloud.auth.service.TokenService;
import com.ring.cloud.common.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 正常token过期时间 1天
    @Value("${jwt.expire:86400000}")
    private long accessExpire;

    // 刷新token过期时间 7天
    @Value("${jwt.refresh-expire:604800000}")
    private long refreshExpire;

    private static final String TOKEN_BLACK_PREFIX = "token:black:";

    @Override
    public String generateToken(Long userId) {
        // 用户名暂时用 userId 填充，后续可从用户信息取真实用户名
        return jwtService.createToken(userId.toString(), userId.toString(), accessExpire);
    }

    @Override
    public String generateRefreshToken(Long userId) {
        return jwtService.createToken(userId.toString(), userId.toString(), refreshExpire);
    }

    @Override
    public boolean isBlack(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACK_PREFIX + token));
    }

    @Override
    public void addBlack(String token) {
        // 黑名单保留7天，和刷新token周期一致
        redisTemplate.opsForValue().set(TOKEN_BLACK_PREFIX + token, "1", 7, TimeUnit.DAYS);
    }
}