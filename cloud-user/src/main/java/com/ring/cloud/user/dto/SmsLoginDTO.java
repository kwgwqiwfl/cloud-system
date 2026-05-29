package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("验证码登录请求")
public class SmsLoginDTO {

    @NotBlank(message = "手机号不能为空")
    @ApiModelProperty("手机号")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("短信验证码")
    private String code;
}