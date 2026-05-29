package com.ring.cloud.user.controller;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRole;
import com.ring.cloud.user.service.SysRoleService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/sys/role")
@Api(tags = "角色管理接口")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @PostMapping("/page")
    @ApiOperation(value = "角色分页查询")
    public MResponse<?> page(@RequestBody @NotNull CommonPageQuery query) {
        try {
            PageResult<SysRole> pageResult = sysRoleService.pageList(query);
            return MResponse.ok(pageResult);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info/{id}")
    @ApiOperation(value = "根据ID查询角色详情")
    public MResponse<?> getInfo(@PathVariable("id") Long id) {
        try {
            SysRole role = sysRoleService.selectByPrimaryKey(id);
            return MResponse.ok(role);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation(value = "新增角色")
    public MResponse<?> save(@RequestBody @NotNull SysRole sysRole) {
        try {
            sysRoleService.save(sysRole);
            return MResponse.ok("新增成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "修改角色")
    public MResponse<?> update(@RequestBody @NotNull SysRole sysRole) {
        try {
            sysRoleService.update(sysRole);
            return MResponse.ok("修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    @ApiOperation(value = "删除角色")
    public MResponse<?> remove(@PathVariable("id") Long id) {
        try {
            sysRoleService.deleteById(id);
            return MResponse.ok("删除成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}