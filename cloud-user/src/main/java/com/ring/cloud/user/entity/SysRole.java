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
@Table(name = "sys_role")
@Comment("角色表")
public class SysRole extends AbstractAuth {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "角色名称")
    @Comment("角色名称")
    @Column(length = 50, nullable = false)
    private String roleName;

    @ApiModelProperty(value = "角色标识")
    @Comment("角色标识，如admin、user，唯一")
    @Column(length = 50, nullable = false, unique = true)
    private String roleCode;

    @ApiModelProperty(value = "角色描述")
    @Comment("角色描述")
    @Column(length = 255)
    private String description;

    @ApiModelProperty(value = "排序")
    @Comment("排序")
    @Column(columnDefinition = "int default 0")
    private Integer sort;

    @ApiModelProperty(value = "状态 0禁用 1正常")
    @Comment("状态 0禁用 1正常")
    @Column(columnDefinition = "tinyint default 1")
    private Integer status;
}