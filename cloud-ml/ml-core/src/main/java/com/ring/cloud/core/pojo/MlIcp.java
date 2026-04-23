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
@Table(name = "ml_icp")
@Comment("备案域名查询记录表")
public class MlIcp extends AbstractMl {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "备案域名")
    @Comment("备案域名")
    @Column(length = 255, nullable = false)
    private String domain;

    public MlIcp() {
    }
    public MlIcp(String domain) {
        this.domain = domain;
        this.queryCount = 0;
    }

}