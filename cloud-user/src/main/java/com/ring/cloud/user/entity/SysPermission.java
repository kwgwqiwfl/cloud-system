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
@Table(name = "sys_permission")
@Comment("权限表")
public class SysPermission extends AbstractAuth {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "父权限ID")
    @Comment("父权限ID")
    @Column(columnDefinition = "bigint default 0")
    private Long parentId;

    @ApiModelProperty(value = "权限名称")
    @Comment("权限名称")
    @Column(length = 50, nullable = false)
    private String name;

    @ApiModelProperty(value = "权限标识")
    @Comment("权限标识，如sys:user:list，唯一")
    @Column(length = 100, nullable = false, unique = true)
    private String perms;

    @ApiModelProperty(value = "权限类型 1菜单 2按钮 3接口")
    @Comment("权限类型 1菜单 2按钮 3接口")
    @Column(nullable = false)
    private Integer type;

    @ApiModelProperty(value = "菜单图标")
    @Comment("菜单图标")
    @Column(length = 255)
    private String icon;

    @ApiModelProperty(value = "前端路由地址")
    @Comment("前端路由地址")
    @Column(length = 255)
    private String path;

    @ApiModelProperty(value = "前端组件路径")
    @Comment("前端组件路径")
    @Column(length = 255)
    private String component;

    @ApiModelProperty(value = "排序")
    @Comment("排序")
    @Column(columnDefinition = "int default 0")
    private Integer sort;

    @ApiModelProperty(value = "状态 0禁用 1正常")
    @Comment("状态 0禁用 1正常")
    @Column(columnDefinition = "tinyint default 1")
    private Integer status;
}