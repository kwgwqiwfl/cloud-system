package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MixIpDomain;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MixIpDomainMapper extends MyIdableMapper<MixIpDomain> {

    // 根据 ip 查询
    @Select("SELECT * FROM ${tableName} WHERE ip = #{ip}")
    List<MixIpDomain> selectByIpDynamic(
            @Param("tableName") String tableName,
            @Param("ip") String ip
    );

    // 根据 ipLong 查询
    @Select("SELECT * FROM ${tableName} WHERE ip_long = #{ipLong}")
    List<MixIpDomain> selectByIpLong(
            @Param("tableName") String tableName,
            @Param("ipLong") Long ipLong
    );

    // 分页查询
    @Select("SELECT * FROM ${tableName} WHERE ip_long = #{ipLong} LIMIT #{offset}, #{pageSize}")
    List<MixIpDomain> selectPageByTable(
            @Param("tableName") String tableName,
            @Param("ipLong") Long ipLong,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    // 统计数量
    @Select("SELECT COUNT(1) FROM ${tableName} WHERE ip_long = #{ipLong}")
    long countIpByTable(
            @Param("tableName") String tableName,
            @Param("ipLong") Long ipLong
    );

    // 批量插入（不含主键）
    @Insert({
            "<script>",
            "INSERT INTO ${tableName} (ip_long, ip, loc, domain, domain_crc, adtime, uptime) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.ipLong}, #{item.ip}, #{item.loc}, #{item.domain}, #{item.domainCrc}, #{item.adtime}, #{item.uptime})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(
            @Param("tableName") String tableName,
            @Param("list") List<MixIpDomain> list
    );

    // 批量 UPSERT（存在则更新，不存在则插入）
    @Insert({
            "<script>",
            "INSERT INTO ${tableName} (ip_long, ip, loc, domain, domain_crc, adtime, uptime) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.ipLong}, #{item.ip}, #{item.loc}, #{item.domain}, #{item.domainCrc}, #{item.adtime}, #{item.uptime})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE ",
            "loc = VALUES(loc), ",
            "uptime = VALUES(uptime)",
            "</script>"
    })
    int batchUpsert(
            @Param("tableName") String tableName,
            @Param("list") List<MixIpDomain> list
    );

}