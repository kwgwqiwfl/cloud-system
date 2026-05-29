package com.ring.cloud.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ring.welkin.common.persistence.entity.gene.Idable;
import com.ring.welkin.common.persistence.entity.preprocess.PreEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import javax.persistence.*;
import java.io.Serializable;

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
public abstract class AbstractId implements Idable<Long>, PreEntity, Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

}