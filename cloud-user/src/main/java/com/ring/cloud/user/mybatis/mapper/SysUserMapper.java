package com.ring.cloud.user.mybatis.mapper;

import com.ring.cloud.user.entity.SysUser;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends MyIdableMapper<SysUser> {

    /**
     * 管理端分页模糊查询
     */
    @Select("<script>" +
            "select * from sys_user where 1=1 " +
            "<if test='key != null and key != \"\"'>and username like concat('%',#{key},'%')</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<SysUser> pageList(@Param("key") String key,
                           @Param("offset") int offset,
                           @Param("size") int size);

    @Select("select * from sys_user where username = #{username}")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("select * from sys_user where phone = #{phone}")
    SysUser selectByPhone(@Param("phone") String phone);
}