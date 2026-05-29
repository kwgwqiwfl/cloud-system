package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("APP注册请求")
public class AppRegisterDTO {

    @NotBlank(message = "账号不能为空")
    @ApiModelProperty("账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("昵称")
    private String nickname;
}