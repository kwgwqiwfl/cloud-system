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
@Table(name = "ml_domain")
@Comment("域名查询记录表")
public class MlDomain extends AbstractMl {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "域名")
    @Comment("域名")
    @Column(length = 191, nullable = false)
    private String domain;

    public MlDomain() {
    }

    public MlDomain(String domain) {
        this.domain = domain;
        this.queryCount = 0;
    }
}