package com.ring.cloud.core.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@Table(name = "mix_domain_ip")
@Comment("域名IP关系表")
public class MixDomainIp extends AbstractStar {

    @ApiModelProperty(value = "IP地址")
    @Comment("IP地址")
    @Column(length = 50, nullable = false)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String ip;

    @ApiModelProperty(value = "域名")
    @Comment("域名")
    @Column(length = 256, nullable = false)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String domain;

    @ApiModelProperty(value = "域名CRC32")
    @Comment("域名CRC32")
    @Column(nullable = false)
    @ColumnType(jdbcType = JdbcType.INTEGER)
    private Integer domainCrc;

    @ApiModelProperty(value = "归属地")
    @Comment("归属地")
    @Column(length = 128)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String loc;

    @ApiModelProperty(value = "创建时间")
    @Comment("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date adtime;

    @ApiModelProperty(value = "更新时间")
    @Comment("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ColumnType(jdbcType = JdbcType.DATE, typeHandler = DateTypeRoutingHandler.class)
    @JsonDeserialize(using = DateJsonDeserializer.class)
    private Date uptime;

    public void setDomain(String domain) {
        this.domain = domain;
        this.domainCrc = IpCoreUtils.crc32(domain);
    }
}