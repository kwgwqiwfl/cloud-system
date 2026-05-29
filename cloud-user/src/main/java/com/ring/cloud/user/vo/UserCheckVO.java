package com.ring.cloud.user.vo;

import lombok.Data;

@Data
public class UserCheckVO {
    private Long userId;
    private String username;
    private String nickname;
    private Integer status;
}