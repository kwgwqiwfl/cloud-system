package com.ring.cloud.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ring.welkin.common.core.jackson.deserializer.DateJsonDeserializer;
import com.ring.welkin.common.persistence.entity.gene.Idable;
import com.ring.welkin.common.persistence.entity.preprocess.PreEntity;
import com.ring.welkin.common.persistence.mybatis.type.routing.DateTypeRoutingHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;
import org.hibernate.annotations.Comment;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 权限模块基础实体父类
 * 提供通用主键、创建时间、更新时间
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "基础实体父类")
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public abstract class AbstractAuth implements Idable<Long>, PreEntity, Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ApiModelProperty(value = "创建时间", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.TIMESTAMP, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    @Column(updatable = false)
    protected Date createTime;

    @ApiModelProperty(value = "更新时间", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.TIMESTAMP, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    protected Date updateTime;

}