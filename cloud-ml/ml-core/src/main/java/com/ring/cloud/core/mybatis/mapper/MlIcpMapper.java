package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlIcp;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface MlIcpMapper extends MyIdableMapper<MlIcp> {

    @Select("<script>" +
            "select * from ml_icp where 1=1 " +
            "<if test='key != null and key != \"\"'>and domain like concat('%',#{key},'%')</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<MlIcp> pageList(@Param("key") String key,
                         @Param("offset") int offset,
                         @Param("size") int size);

    @Update("update ml_icp set query_count = #{queryCount} where domain = #{domain}")
    int updateByDomain(MlIcp icp);

    @Select("select * from ml_icp where domain = #{domain}")
    MlIcp selectByDomain(@Param("domain") String domain);
}