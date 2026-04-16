package com.ring.cloud.core.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.core.util.IpCoreUtils;
import com.ring.welkin.common.core.jackson.deserializer.DateJsonDeserializer;
import com.ring.welkin.common.persistence.mybatis.type.routing.DateTypeRoutingHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;
import org.hibernate.annotations.Comment;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel
@Entity
@Table(name = "domain_inout")
@Comment("域名输入输出表")
public class DomainInout extends AbstractStar {

    @ApiModelProperty(value = "ip地址")
    @Comment("ip地址")
    @Column(length = 50)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String ip;

    @ApiModelProperty("归属地")
    @Comment("归属地")
    @Column(length = 128)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String loc;

    @ApiModelProperty("输入域名")
    @Comment("输入域名")
    @Column(length = 191)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String inputDomain;

    @ApiModelProperty("输出域名")
    @Comment("输出域名")
    @Column(length = 191)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String outputDomain;

    @ApiModelProperty(value = "创建时间", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date adtime;

    @ApiModelProperty(value = "更新时间", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Comment("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date uptime;

    public DomainInout() {
    }

    public DomainInout(String ip, String loc, String outputDomain, String adtime, String uptime) {
        this.ip = ip;
        this.loc = loc;
        this.outputDomain = outputDomain;
        this.adtime = DateUtil.parseDate(adtime);
        this.uptime = DateUtil.parseDate(uptime);
    }
}