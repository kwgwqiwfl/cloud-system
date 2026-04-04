package com.ring.cloud.core.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Data
@ApiModel
@Entity
@Table(name = "ip_route_config")
@Comment("ip路由表")
public class IpRouteConfig extends AbstractStar {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "ip")
    @Comment("ip")
    @Column(length = 64)
    private String ip;

    @ApiModelProperty(value = "表后缀")
    @Comment("表后缀")
    @Column(length = 2, columnDefinition = "char(2)")
    private String tableSuffix;

    @ApiModelProperty(value = "档位（1=10万）")
    @Comment("档位")
    @Column(columnDefinition = "int default 0")
    private Integer dataCount = 0;

}