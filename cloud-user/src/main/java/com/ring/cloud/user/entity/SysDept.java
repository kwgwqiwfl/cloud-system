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
@Table(name = "sys_dept")
@Comment("部门表")
public class SysDept extends AbstractAuth {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "父部门ID")
    @Comment("父部门ID")
    @Column(columnDefinition = "bigint default 0")
    private Long parentId;

    @ApiModelProperty(value = "部门名称")
    @Comment("部门名称")
    @Column(length = 50, nullable = false)
    private String deptName;

    @ApiModelProperty(value = "排序")
    @Comment("排序")
    @Column(columnDefinition = "int default 0")
    private Integer sort;

    @ApiModelProperty(value = "状态 0禁用 1正常")
    @Comment("状态 0禁用 1正常")
    @Column(columnDefinition = "tinyint default 1")
    private Integer status;
}