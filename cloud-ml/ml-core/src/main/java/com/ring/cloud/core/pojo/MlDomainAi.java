package com.ring.cloud.core.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ring.cloud.core.util.HashUtil;
import com.ring.welkin.common.core.jackson.deserializer.DateJsonDeserializer;
import com.ring.welkin.common.persistence.mybatis.type.routing.DateTypeRoutingHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.hibernate.annotations.Comment;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Data
@ApiModel
@Entity
@Table(name = "ml_domain_ai")
@Comment("域名查询记录表")
public class MlDomainAi extends AbstractStar {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "域名")
    @Comment("域名")
    @Column(length = 255, nullable = false)
    private String domain;

    @ApiModelProperty(value = "域名哈希值")
    @Comment("域名哈希值")
    @Column(length = 40, nullable = false)
    private String domainHash;

    @ApiModelProperty(value = "创建时间")
    @Comment("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date adTime;

    @ApiModelProperty(value = "更新时间")
    @Comment("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date upTime;

    @ApiModelProperty(value = "累计查询次数")
    @Comment("累计查询次数")
    @Column(nullable = false)
    private Integer totalCount;

    @ApiModelProperty(value = "按天查询次数")
    @Comment("按天查询次数")
    @Column(nullable = false)
    private Integer dayCount;

    public MlDomainAi() {
    }

    /**
     * domain初始化
     */
    public MlDomainAi(String domain) {
        this.domain = domain;
        this.domainHash = HashUtil.sha1(domain);
        this.adTime = new Date();
        this.upTime = new Date();
        this.totalCount = 1;
        this.dayCount = 1;
    }
}