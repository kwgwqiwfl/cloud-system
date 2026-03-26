package com.ring.cloud.core.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Data
@ApiModel
@Entity
@Table(name = "ml_task_exec", indexes = {@Index(columnList = "taskId")})
@Comment("采集ip任务执行记录表")
public class IpTaskExec extends AbstractStar {
    
    @ApiModelProperty(value = "任务id")
    @Comment("任务id")
    @Column(length = 50)
    @ColumnType(jdbcType = JdbcType.BIGINT)
    private Long taskId;
    
    @ApiModelProperty(value = "采集完成状态")
    @Comment("采集完成状态")
    @Column(length = 20)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String progressStatus;

    @ApiModelProperty(value = "监控计数")
    @Comment("监控计数")
    @Column(length = 10, nullable = false)
    @ColumnType(jdbcType = JdbcType.BIGINT)
    @ColumnDefault("0")
    private Long metricCount;

    @ApiModelProperty(value = "失败原因")
    @Comment("失败原因")
    @Column(length = 1000)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    @ColumnDefault("'other'")
    private String failReason;

}
