package com.ring.cloud.user.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@ApiModel
@Entity
@Table(name = "sys_user")
@Comment("系统用户表")
public class SysUser extends AbstractAuth {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "所属部门ID")
    @Comment("所属部门ID")
    @Column(columnDefinition = "bigint")
    private Long deptId;

    @ApiModelProperty(value = "登录账号")
    @Comment("登录账号，唯一")
    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @ApiModelProperty(value = "BCrypt加密密码")
    @Comment("BCrypt加密密码")
    @Column(length = 100, nullable = false)
    private String password;

    @ApiModelProperty(value = "用户昵称")
    @Comment("用户昵称")
    @Column(length = 50, nullable = false)
    private String nickname;

    @ApiModelProperty(value = "手机号")
    @Comment("手机号")
    @Column(length = 11)
    private String mobile;

    @ApiModelProperty(value = "邮箱")
    @Comment("邮箱")
    @Column(length = 100)
    private String email;

    @ApiModelProperty(value = "头像地址")
    @Comment("头像地址")
    @Column(length = 255)
    private String avatar;

    @ApiModelProperty(value = "账号状态 0锁定 1正常")
    @Comment("账号状态 0锁定 1正常")
    @Column(columnDefinition = "tinyint default 1")
    private Integer status;
}