package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MlSubdomainMapper extends MyIdableMapper<MlSubdomain> {

    @Select("<script>" +
            "select * from ml_subdomain where 1=1 " +
            "<if test='key != null and key != \"\"'>and domain like concat('%',#{key},'%')</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<MlSubdomain> pageList(@Param("key") String key,
                               @Param("offset") int offset,
                               @Param("size") int size);

    @Update("update ml_subdomain set query_count = #{queryCount} where domain = #{domain}")
    int updateByDomain(MlSubdomain subdomain);

    @Select("select * from ml_subdomain where domain = #{domain}")
    MlSubdomain selectByDomain(@Param("domain") String domain);

    // ===================== 修复版：XML特殊字符已转义 =====================
    @Insert("<script>"
            + "INSERT INTO ml_subdomain (domain, create_time, update_time) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.domain}, CURDATE(), CURDATE())"
            + "</foreach>"
            + "ON DUPLICATE KEY UPDATE "
            + "update_time = IF(update_time &lt;&gt; CURDATE(), CURDATE(), update_time), "
            + "query_count = IF(update_time &lt;&gt; CURDATE() AND PERIOD_DIFF(DATE_FORMAT(CURDATE(),'%Y%m'), DATE_FORMAT(update_time,'%Y%m'))&gt;=1, query_count+1, query_count)"
            + "</script>")
    void batchSaveOrUpdate(@Param("list") List<MlSubdomain> list);
}