package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlDomainAi;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MlDomainAiMapper extends MyIdableMapper<MlDomainAi> {

    @Insert("<script>"
            + "INSERT INTO ml_domain_ai (domain, domain_hash, ad_time, up_time, total_count, day_count) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.domain}, #{item.domainHash}, #{item.adTime}, #{item.upTime}, #{item.totalCount}, #{item.dayCount})"
            + "</foreach>"
            + "ON DUPLICATE KEY UPDATE "
            + "total_count = total_count + 1, "
            + "day_count = IF(up_time = CURDATE(), day_count + 1, 1), "
            + "up_time = CURDATE()"
            + "</script>")
    void batchSaveOrUpdate(@Param("list") List<MlDomainAi> list);
}