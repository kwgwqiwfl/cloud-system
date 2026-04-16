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

    /**
     * 批量域名关联查询 + 直接导出到文件（无返回值·最快）
     * 临时表：ip_domains_tmp
     * 兼容 MySQL 5.6/5.7/8.0+
     */
    @Select({
            "SELECT t.ip,t.loc,t.domain,t.adtime,t.uptime,t.ip_long ",
            "FROM ( ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_0 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_1 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_2 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_3 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_4 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_5 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_6 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_7 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_8 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_9 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_10 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_11 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_12 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_13 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_14 UNION ALL ",
            "SELECT ip,loc,domain,adtime,uptime,ip_long FROM ip_domain_15 ",
            ") t ",
            "INNER JOIN ip_domains_tmp tmp ON t.domain = tmp.domain ",
            "INTO OUTFILE #{filePath} ",
            "CHARACTER SET utf8mb4 ",
            "FIELDS TERMINATED BY ',' ",
            "LINES TERMINATED BY '\\n'"
    })
    void exportToFileByDomainTempTable(@Param("filePath") String filePath);
}
