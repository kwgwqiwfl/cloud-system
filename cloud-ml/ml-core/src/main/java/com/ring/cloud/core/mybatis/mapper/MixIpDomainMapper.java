package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.*;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;

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
            "adtime = VALUES(adtime), ",
            "uptime = VALUES(uptime)",
            "</script>"
    })
    int batchUpsert(
            @Param("tableName") String tableName,
            @Param("list") List<MixIpDomain> list
    );

    //导出mix数据===============================================================================
    // 1. 最新域名
    @Select("SELECT domain,create_time,update_time,query_count FROM ml_domain")
    @Options(fetchSize = Integer.MIN_VALUE)
    List<MlDomain> streamMlDomain();

    // 2. 最新ip
    @Select("SELECT ip,create_time,update_time,query_count FROM ml_ip")
    @Options(fetchSize = Integer.MIN_VALUE)
    List<MlIp> streamMlIp();

    // 3. 最新备案
    @Select("SELECT domain,create_time,update_time,query_count FROM ml_icp")
    @Options(fetchSize = Integer.MIN_VALUE)
    List<MlIcp> streamMlIcp();

    // 4. 最新子域名
    @Select("SELECT domain,create_time,update_time,query_count FROM ml_subdomain")
    @Options(fetchSize = Integer.MIN_VALUE)
    List<MlSubdomain> streamMlSubdomain();

    // 5. 特定ip → 使用 MixIpDomain
    @Select("SELECT ip,loc,domain,adtime,uptime FROM specify_ip_domain")
    @Options(fetchSize = Integer.MIN_VALUE)
    List<MixIpDomain> streamSpecifyIpDomain();

    // 6. 最新ai
    @Select("SELECT domain,ad_time,up_time,total_count,day_count FROM ml_domain_ai")
    @Options(
            fetchSize = Integer.MIN_VALUE,
            timeout = 0,
            resultSetType = ResultSetType.FORWARD_ONLY  // 👈 必须加
    )
    List<MlDomainAi> streamMlDomainAiStream();

    // 7 单表流式查询 IP ✅ 真·流式修复
    @Select("SELECT /*+ NO_LOCK */ ip,loc,domain,adtime,uptime FROM ${tableName}")
    @Options(
            fetchSize = Integer.MIN_VALUE,
            timeout = 0,
            resultSetType = ResultSetType.FORWARD_ONLY  // 👈 必须加
    )
    List<MixIpDomain> ipDomainStream(@Param("tableName") String tableName);

    // 8 单表流式查询 Domain ✅ 真·流式修复
    @Select("SELECT /*+ NO_LOCK */ ip,domain,adtime,uptime FROM ${tableName}")
    @Options(
            fetchSize = Integer.MIN_VALUE,
            timeout = 0,
            resultSetType = ResultSetType.FORWARD_ONLY  // 👈 必须加
    )
    List<MixDomainIp> domainIpStream(@Param("tableName") String tableName);
}