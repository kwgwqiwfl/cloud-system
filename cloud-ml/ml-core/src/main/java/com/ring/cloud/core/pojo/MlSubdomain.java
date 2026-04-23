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
@Table(name = "ml_subdomain")
@Comment("子域名查询记录表")
public class MlSubdomain extends AbstractMl {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "子域名")
    @Comment("子域名")
    @Column(length = 255, nullable = false)
    private String domain;

    public MlSubdomain() {
    }

    public MlSubdomain(String domain) {
        this.domain = domain;
        this.queryCount = 0;
    }
}