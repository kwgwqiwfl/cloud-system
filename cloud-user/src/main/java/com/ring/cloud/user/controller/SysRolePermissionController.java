package com.ring.cloud.user.controller;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRolePermission;
import com.ring.cloud.user.service.SysRolePermissionService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/sys/role-permission")
@Api(tags = "角色权限关联接口")
public class SysRolePermissionController {

    @Autowired
    private SysRolePermissionService sysRolePermissionService;

    @PostMapping("/page")
    @ApiOperation(value = "角色权限分页查询")
    public MResponse<?> page(@RequestBody @NotNull CommonPageQuery query) {
        try {
            PageResult<SysRolePermission> pageResult = sysRolePermissionService.pageList(query);
            return MResponse.ok(pageResult);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info/{id}")
    @ApiOperation(value = "根据ID查询详情")
    public MResponse<?> getInfo(@PathVariable("id") Long id) {
        try {
            SysRolePermission rp = sysRolePermissionService.selectByPrimaryKey(id);
            return MResponse.ok(rp);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation(value = "新增关联")
    public MResponse<?> save(@RequestBody @NotNull SysRolePermission sysRolePermission) {
        try {
            sysRolePermissionService.save(sysRolePermission);
            return MResponse.ok("新增成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "修改关联")
    public MResponse<?> update(@RequestBody @NotNull SysRolePermission sysRolePermission) {
        try {
            sysRolePermissionService.update(sysRolePermission);
            return MResponse.ok("修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    @ApiOperation(value = "删除关联")
    public MResponse<?> remove(@PathVariable("id") Long id) {
        try {
            sysRolePermissionService.deleteById(id);
            return MResponse.ok("删除成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}