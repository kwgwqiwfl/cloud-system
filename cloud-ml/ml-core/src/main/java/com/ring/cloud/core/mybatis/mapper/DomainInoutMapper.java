package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.entity.ip.DomainCount;
import com.ring.cloud.core.pojo.DomainInout;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DomainInoutMapper extends MyIdableMapper<DomainInout> {

    /**
     * 根据输入域名分页查询
     */
    @Select({
            "<script>",
            "SELECT * FROM domain_inout ",
            "WHERE input_domain = #{inputDomain} ",
            "ORDER BY id DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<DomainInout> selectPageByInputDomain(
            @Param("inputDomain") String inputDomain,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );
    /**
     * 按输入域名统计每个输出域名出现的次数（你要的汇算）
     */
    @Select({
            "<script>",
            "SELECT output_domain, COUNT(*) AS cnt ",
            "FROM domain_inout ",
            "WHERE input_domain = #{inputDomain} ",
            "GROUP BY output_domain",
            "</script>"
    })
    List<Map<String, Object>> countOutputByInputDomain(
            @Param("inputDomain") String inputDomain
    );

    /**
     * 批量UPSERT（有则更新归属地、更新时间，无则插入）
     */
    @Insert({
            "<script>",
            "INSERT INTO domain_inout (input_domain, output_domain, ip, loc, adtime, uptime, input_hash, output_hash) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.inputDomain}, #{item.outputDomain}, #{item.ip}, #{item.loc}, #{item.adtime}, #{item.uptime}, #{item.inputHash}, #{item.outputHash})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE ",
            "loc = VALUES(loc), ",
            "adtime = VALUES(adtime), ",
            "uptime = VALUES(uptime)",
            "</script>"
    })
    int batchUpsert(@Param("list") List<DomainInout> list);

    @Select({
            "<script>",
            "SELECT ",
            "  input_domain     AS inputDomain, ",
            "  output_domain    AS outputDomain, ",
            "  COUNT(*)         AS count ",
            "FROM domain_inout ",

            "<if test=\"hashList != null and !hashList.isEmpty()\">",
            "WHERE input_hash IN ",
            "<foreach collection='hashList' open='(' separator=',' close=')' item='item'>",
            "#{item}",
            "</foreach>",
            "</if>",

            "GROUP BY input_hash, output_hash, input_domain, output_domain ",
            "ORDER BY input_domain, count DESC",
            "</script>"
    })
    @Options(fetchSize = Integer.MIN_VALUE)
    List<DomainCount> exportStatStream(
            @Param("hashList") List<String> hashList
    );

    @Select({
            "<script>",
            "SELECT ",
            "  input_domain     AS inputDomain, ",
            "  output_domain    AS outputDomain, ",
            "  COUNT(*)         AS count ",
            "FROM domain_inout ",
            "GROUP BY input_hash, output_hash, input_domain, output_domain ",
            "ORDER BY input_domain, count DESC",
            "</script>"
    })
    @Options(fetchSize = Integer.MIN_VALUE)
    List<DomainCount> exportAllStatStream();
}