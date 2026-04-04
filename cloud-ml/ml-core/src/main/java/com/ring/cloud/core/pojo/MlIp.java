package com.ring.cloud.core.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@ApiModel
@Entity
@Table(name = "ml_ip")
@Comment("IP查询记录表")
public class MlIp extends AbstractMl {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "IP地址")
    @Comment("IP地址")
    @Column(length = 50, nullable = false)
    private String ip;

    public MlIp() {
    }

    public MlIp(String ip) {
        this.ip = ip;
        this.queryCount = 0;
    }
}
