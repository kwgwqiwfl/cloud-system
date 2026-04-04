package com.ring.cloud.core.pojo;

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
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public abstract class AbstractMl implements Idable<Long>, PreEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ApiModelProperty(value = "首次查询日期")
    @Comment("首次查询日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    protected Date createTime;

    @ApiModelProperty(value = "最新查询日期")
    @Comment("最新查询日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    protected Date updateTime;

    @ApiModelProperty(value = "累计查询次数")
    @Comment("累计查询次数")
    @Column(columnDefinition = "int default 1")
    protected Integer queryCount = 1;

}
