package com.ring.cloud.core.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ring.cloud.core.util.DateUtil;
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
@Table(name = "ip_domain")
@Comment("ip-domain源表")
public class SourceIpDomain extends AbstractStar {

    @ApiModelProperty(value = "ip地址")
    @Comment("ip地址")
    @Column(length = 50)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String ip;

    @ApiModelProperty("归属")
    @Comment("归属")
    @Column(length = 128)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String loc;

    @ApiModelProperty("绑定过的域名")
    @Comment("绑定过的域名")
    @Column(length = 256)
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String domain;

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

    private String adtimeStr;
    private String uptimeStr;

    public SourceIpDomain() {
    }

    public SourceIpDomain(String ip, String loc, String domain, String adtimeStr, String uptimeStr) {
        this.ip = ip;
        this.loc = loc;
        this.domain = domain;
        this.adtimeStr = adtimeStr;
        this.uptimeStr = uptimeStr;
    }
}
