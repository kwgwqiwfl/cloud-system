package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MixDomainIp;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MixDomainIpMapper extends MyIdableMapper<MixDomainIp> {

    @Select({
            "<script>",
            "SELECT * FROM mix_domain_ip ",
            "WHERE domain_crc = #{domainCrc} ",
            "ORDER BY id DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<MixDomainIp> selectPageByDomainCrc(@Param("domainCrc") Integer domainCrc,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    @Insert({
            "<script>",
            "INSERT INTO ${tableName} (ip_long, ip, domain, domain_crc, adtime, uptime) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.ipLong}, #{item.ip}, #{item.domain}, #{item.domainCrc}, #{item.adtime}, #{item.uptime})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE ",
            "adtime = VALUES(adtime), ",
            "uptime = VALUES(uptime)",
            "</script>"
    })
    int batchUpsert(
            @Param("tableName") String tableName,
            @Param("list") List<MixDomainIp> list
    );
}