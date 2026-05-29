package com.ring.cloud.user.mybatis.mapper;

import com.ring.cloud.user.entity.SysUserRole;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends MyIdableMapper<SysUserRole> {

    @Select("<script>" +
            "select * from sys_user_role where 1=1 " +
            "<if test='key != null and key != \"\"'>and user_id = #{key}</if>" +
            " limit #{offset}, #{size}" +
            "</script>")
    List<SysUserRole> pageList(@Param("key") String key,
                               @Param("offset") int offset,
                               @Param("size") int size);
}