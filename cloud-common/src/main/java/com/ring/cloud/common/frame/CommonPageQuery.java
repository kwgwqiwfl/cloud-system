package com.ring.cloud.common.frame;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "单个条件分页查询", description = "单个条件分页查询")
public class CommonPageQuery extends PageQueryDTO {

    @NotBlank(message = "参数 不能为空")
    @ApiModelProperty(value = "key", required = true)
    private String key;
}
