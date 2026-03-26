package com.ring.cloud.core.entity.draft;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SinkNameRuler {
    @ApiModelProperty(name = "规则名称")
    private String name = "";
    @ApiModelProperty(name = "规则值")
    private String value = "";
    @ApiModelProperty(name = "描述")
    private String desc;
}
