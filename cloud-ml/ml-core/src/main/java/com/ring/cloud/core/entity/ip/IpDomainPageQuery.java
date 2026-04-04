package com.ring.cloud.core.entity.ip;

import com.ring.cloud.core.frame.PageQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "IP域名分页查询", description = "根据IP查询域名列表")
public class IpDomainPageQuery extends PageQueryDTO {

    @NotBlank(message = "IP 不能为空")
    @ApiModelProperty(value = "目标IP", required = true, example = "1.1.1.1")
    private String ip;
}
