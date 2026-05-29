package com.ring.cloud.auth.feign.dto;

import lombok.Data;

@Data
public class UserCheckRequestDTO {
    private String username;
    private String password;
}