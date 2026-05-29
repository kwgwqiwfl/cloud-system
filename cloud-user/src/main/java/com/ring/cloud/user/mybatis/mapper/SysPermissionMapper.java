package com.ring.cloud.user.mybatis.mapper;

import com.ring.cloud.user.entity.SysPermission;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends MyIdableMapper<SysPermission> {

    @Select("<script>" +
            "select * from sys_permission where 1=1 " +
            "<if test='key != null and key != \"\"'>and name like concat('%',#{key},'%')</if>" +
            " order by sort asc, id desc limit #{offset}, #{size}" +
            "</script>")
    List<SysPermission> pageList(@Param("key") String key,
                                 @Param("offset") int offset,
                                 @Param("size") int size);
}