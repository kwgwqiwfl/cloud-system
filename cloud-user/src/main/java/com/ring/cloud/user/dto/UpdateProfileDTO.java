package com.ring.cloud.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("更新个人资料")
public class UpdateProfileDTO {
    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像地址")
    private String avatar;
}