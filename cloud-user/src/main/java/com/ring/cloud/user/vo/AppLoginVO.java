package com.ring.cloud.user.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("APP 登录返回对象")
public class AppLoginVO {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("账号")
    private String username;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("访问令牌")
    private String token;
}