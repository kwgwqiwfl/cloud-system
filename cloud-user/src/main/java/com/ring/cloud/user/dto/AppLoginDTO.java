package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("APP 登录请求参数")
public class AppLoginDTO {

    @NotBlank(message = "账号不能为空")
    @ApiModelProperty("登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @ApiModelProperty("登录密码")
    private String password;
}