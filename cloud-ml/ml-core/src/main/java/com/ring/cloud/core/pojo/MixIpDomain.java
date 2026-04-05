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
@Table(name = "mix_ip")
@Comment("IP域名分表")
public class MixIpDomain extends AbstractStar {

    @ApiModelProperty(value = "ip整型")
    @Comment("ip整型")
    @Column
    @ColumnType(jdbcType = JdbcType.BIGINT)
    private Long ipLong;

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

    @ApiModelProperty("绑定过的域名")
    @Comment("绑定过的域名")
    @Column(length = 256)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String domain;

    @ApiModelProperty("域名CRC32哈希")
    @Comment("域名CRC32哈希")
    @Column
    @ColumnType(jdbcType = JdbcType.INTEGER)
    private Integer domainCrc;

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

    public void setIp(String ip) {
        this.ip = ip;
        this.ipLong = IpCoreUtils.ipToLong(ip);
    }

    public void setDomain(String domain) {
        this.domain = domain;
        this.domainCrc = IpCoreUtils.crc32(domain);
    }


}