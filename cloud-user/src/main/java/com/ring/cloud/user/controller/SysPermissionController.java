package com.ring.cloud.user.controller;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysPermission;
import com.ring.cloud.user.service.SysPermissionService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/sys/permission")
@Api(tags = "权限菜单管理接口")
public class SysPermissionController {

    @Autowired
    private SysPermissionService sysPermissionService;

    @PostMapping("/page")
    @ApiOperation(value = "权限菜单分页查询")
    public MResponse<?> page(@RequestBody @NotNull CommonPageQuery query) {
        try {
            PageResult<SysPermission> pageResult = sysPermissionService.pageList(query);
            return MResponse.ok(pageResult);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info/{id}")
    @ApiOperation(value = "根据ID查询权限详情")
    public MResponse<?> getInfo(@PathVariable("id") Long id) {
        try {
            SysPermission permission = sysPermissionService.selectByPrimaryKey(id);
            return MResponse.ok(permission);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation(value = "新增权限菜单")
    public MResponse<?> save(@RequestBody @NotNull SysPermission sysPermission) {
        try {
            sysPermissionService.save(sysPermission);
            return MResponse.ok("新增成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "修改权限菜单")
    public MResponse<?> update(@RequestBody @NotNull SysPermission sysPermission) {
        try {
            sysPermissionService.update(sysPermission);
            return MResponse.ok("修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    @ApiOperation(value = "删除权限菜单")
    public MResponse<?> remove(@PathVariable("id") Long id) {
        try {
            sysPermissionService.deleteById(id);
            return MResponse.ok("删除成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}