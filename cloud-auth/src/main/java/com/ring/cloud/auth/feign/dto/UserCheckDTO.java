package com.ring.cloud.auth.feign.dto;

import lombok.Data;

@Data
public class UserCheckDTO {

    private Long userId;

    private String username;

    private String nickname;

    private Integer status;
}