package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("忘记密码重置")
public class ResetPwdDTO {
    @NotBlank(message = "手机号不能为空")
    @ApiModelProperty("手机号")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @ApiModelProperty("新密码")
    private String newPwd;
}