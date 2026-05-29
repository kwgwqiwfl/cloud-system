package com.ring.cloud.user.mybatis.mapper;

import com.ring.cloud.user.entity.SysDept;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptMapper extends MyIdableMapper<SysDept> {

    @Select("<script>" +
            "select * from sys_dept where 1=1 " +
            "<if test='key != null and key != \"\"'>and dept_name like concat('%',#{key},'%')</if>" +
            " order by sort asc, id desc limit #{offset}, #{size}" +
            "</script>")
    List<SysDept> pageList(@Param("key") String key,
                           @Param("offset") int offset,
                           @Param("size") int size);
}