package com.ring.cloud.auth.service;

public interface TokenService {

    String generateToken(Long userId);

    String generateRefreshToken(Long userId);

    boolean isBlack(String token);

    void addBlack(String token);
}