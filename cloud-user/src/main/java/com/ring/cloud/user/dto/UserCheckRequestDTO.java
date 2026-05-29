package com.ring.cloud.user.dto;

import lombok.Data;

@Data
public class UserCheckRequestDTO {
    private String username;
    private String password;
}