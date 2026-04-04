package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlIp;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface MlIpMapper extends MyIdableMapper<MlIp> {

    @Select("<script>" +
            "select * from ml_ip where 1=1 " +
            "<if test='key != null and key != \"\"'>and ip like concat('%',#{key},'%')</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<MlIp> pageList(@Param("key") String key,
                        @Param("offset") int offset,
                        @Param("size") int size);

    @Update("update ml_ip set query_count = #{queryCount} where ip = #{ip}")
    int updateByIp(MlIp ip);

    @Select("select * from ml_ip where ip = #{ip}")
    MlIp selectByIp(@Param("ip") String ip);
}