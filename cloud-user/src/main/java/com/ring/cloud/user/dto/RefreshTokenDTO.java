package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("刷新Token请求")
public class RefreshTokenDTO {
    @NotBlank(message = "token不能为空")
    @ApiModelProperty("旧token")
    private String token;
}