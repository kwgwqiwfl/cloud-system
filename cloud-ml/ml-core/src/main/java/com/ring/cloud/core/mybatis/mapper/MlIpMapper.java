package com.ring.cloud.core.mybatis.mapper;

import com.ring.cloud.core.pojo.MlIp;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.*;

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

    // ===================== 修复版 batchSaveOrUpdate =====================
    @Insert("<script>"
            + "INSERT INTO ml_ip (ip, create_time, update_time) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.ip}, CURDATE(), CURDATE())"
            + "</foreach>"
            + "ON DUPLICATE KEY UPDATE "
            + "update_time = IF(update_time &lt;&gt; CURDATE(), CURDATE(), update_time), "
            + "query_count = IF(update_time &lt;&gt; CURDATE() AND PERIOD_DIFF(DATE_FORMAT(CURDATE(),'%Y%m'), DATE_FORMAT(update_time,'%Y%m'))&gt;=1, query_count+1, query_count)"
            + "</script>")
    void batchSaveOrUpdate(@Param("list") List<MlIp> list);
}