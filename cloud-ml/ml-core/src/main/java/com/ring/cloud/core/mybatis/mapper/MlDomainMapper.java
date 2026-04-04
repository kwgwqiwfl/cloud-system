package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlDomain;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface MlDomainMapper extends MyIdableMapper<MlDomain> {

    @Select("<script>" +
            "select * from ml_domain where 1=1 " +
            "<if test='key != null and key != \"\"'>and domain like concat('%',#{key},'%')</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<MlDomain> pageList(@Param("key") String key,
                            @Param("offset") int offset,
                            @Param("size") int size);

    @Update("update ml_domain set query_count = #{queryCount} where domain = #{domain}")
    int updateByDomain(MlDomain domain);

    @Select("select * from ml_domain where domain = #{domain}")
    MlDomain selectByDomain(@Param("domain") String domain);
}