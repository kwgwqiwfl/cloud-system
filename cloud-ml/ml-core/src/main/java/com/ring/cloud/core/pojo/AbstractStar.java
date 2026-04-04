package com.ring.cloud.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ring.welkin.common.persistence.entity.gene.Idable;
import com.ring.welkin.common.persistence.entity.preprocess.PreEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public abstract class AbstractStar implements Idable<Long>, PreEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

}
