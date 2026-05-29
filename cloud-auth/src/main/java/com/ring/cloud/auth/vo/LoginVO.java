package com.ring.cloud.auth.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private String refreshToken;

    private Long expire;

    private Long userId;

    private String username;

    private String nickname;
}