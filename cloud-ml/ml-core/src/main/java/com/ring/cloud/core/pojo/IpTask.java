package com.ring.cloud.core.pojo;

import com.ring.cloud.core.entity.ip.IpTaskInfo;
import com.ring.cloud.core.mybatis.type.ClobVsIpTaskInfoTypeHandler;
import com.ring.welkin.common.persistence.jpa.converter.ObjectToStringConverter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.hibernate.annotations.Comment;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.*;
import java.util.Date;

@Data
@ApiModel
@Entity
@Table(name = "ml_ip_task")
@Comment("ip任务列表")
public class IpTask  extends AbstractStar {

    private static final long serialVersionUID = -1L;

    @ApiModelProperty(value = "任务名称")
    @Comment("任务名称")
    @Column(length = 255)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String name;

    @ApiModelProperty(value = "描述")
    @Comment("描述")
    @Column(length = 255)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String description;

    @ApiModelProperty(value = "任务信息")
    @Comment("任务信息")
    @Lob
    @Convert(converter = ObjectToStringConverter.class)
    @ColumnType(jdbcType = JdbcType.CLOB, typeHandler = ClobVsIpTaskInfoTypeHandler.class)
    private IpTaskInfo ipTaskInfo;

    @ApiModelProperty("创建时间")
    @Comment("创建时间")
    @Temporal(TemporalType.TIMESTAMP)
    @ColumnType(jdbcType = JdbcType.TIMESTAMP)
    private Date createTime;

    @ApiModelProperty("修改时间")
    @Comment("修改时间")
    @Temporal(TemporalType.TIMESTAMP)
    @ColumnType(jdbcType = JdbcType.TIMESTAMP)
    private Date endTime;

}
