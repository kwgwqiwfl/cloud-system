package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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
}