package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("修改密码")
public class UpdatePwdDTO {
    @NotBlank(message = "原密码不能为空")
    @ApiModelProperty("原密码")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    @ApiModelProperty("新密码")
    private String newPwd;
}