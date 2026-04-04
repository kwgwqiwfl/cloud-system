package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IpDomainMapper extends MyIdableMapper<SourceIpDomain> {
    //根据ip查询
    @Select("SELECT * FROM ${tableName} WHERE ip = #{ip}")
    List<SourceIpDomain> selectByIpDynamic(
            @Param("tableName") String tableName,
            @Param("ip") String ip
    );

    // 分页查询
    @Select("SELECT * FROM ${tableName} WHERE ip_long = #{ipLong} LIMIT #{offset}, #{pageSize}")
    List<SourceIpDomain> selectPageByTable(
            @Param("tableName") String tableName,
            @Param("ipLong") Long ipLong,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Select("SELECT COUNT(1) FROM ${tableName} WHERE ip_long = #{ipLong}")
    long countIpByTable(
            @Param("tableName") String tableName,
            @Param("ipLong") Long ipLong
    );

    @Update({
            "LOAD DATA LOCAL INFILE #{filePath} ",
            "INTO TABLE ${tableName} ",
            "CHARACTER SET utf8 ",
            "FIELDS TERMINATED BY ',' ",
            "ENCLOSED BY '\"' ",
            "LINES TERMINATED BY '\\n' ",
            "IGNORE 1 ROWS ",
            "(id, ip, loc, domain, adtime, uptime) ",
            "SET ip_long = INET_ATON(ip)"
    })
    int loadCsvToTmpTable(
            @Param("tableName") String tableName,
            @Param("filePath") String filePath
    );

    @Insert({
            "<script>",
            "INSERT INTO ${tableName} (id, ip, loc, domain, adtime, uptime) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.id}, #{item.ip}, #{item.loc}, #{item.domain}, #{item.adtime}, #{item.uptime})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(
            @Param("tableName") String tableName,
            @Param("list") List<SourceIpDomain> list
    );
}
