package com.ring.cloud.core.frame;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 通用分页查询请求参数
 * 所有对外列表接口都用这个
 */
@Data
@ApiModel("通用分页查询参数")
public class PageQueryDTO {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    @ApiModelProperty(value = "页码，从1开始", required = true, example = "1")
    private Integer pageNum = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页最小1条")
    @Max(value = 1000, message = "每页最多1000条")
    @ApiModelProperty(value = "每页条数", required = true, example = "10")
    private Integer pageSize = 10;

    // 可选：排序字段
    @ApiModelProperty(value = "排序字段", example = "adtime")
    private String orderBy;

    // 可选：排序方式
    @ApiModelProperty(value = "排序方式：asc/desc", example = "desc")
    private String sort;
}
