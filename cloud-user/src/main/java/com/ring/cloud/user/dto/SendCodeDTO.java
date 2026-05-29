package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("发送验证码")
public class SendCodeDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;
}