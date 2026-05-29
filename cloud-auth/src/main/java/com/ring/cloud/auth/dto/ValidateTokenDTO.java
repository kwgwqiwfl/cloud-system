package com.ring.cloud.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ValidateTokenDTO {

    @NotBlank(message = "token不能为空")
    private String token;
}