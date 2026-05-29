package com.ring.cloud.user.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.*;
import java.io.Serializable;

@Data
@ApiModel
@Entity
@Table(name = "sys_role_permission")
@Comment("角色权限关联表")
public class SysRolePermission extends AbstractId {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID")
    @Comment("主键ID")
    private Long id;

    @ApiModelProperty(value = "角色ID")
    @Comment("角色ID")
    @Column(nullable = false)
    private Long roleId;

    @ApiModelProperty(value = "权限ID")
    @Comment("权限ID")
    @Column(nullable = false)
    private Long permissionId;
}