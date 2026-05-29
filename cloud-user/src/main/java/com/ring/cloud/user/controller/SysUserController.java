package com.ring.cloud.user.controller;

import com.ring.cloud.common.exception.BusinessException;
import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.common.result.ResultCode;
import com.ring.cloud.user.dto.UserCheckRequestDTO;
import com.ring.cloud.user.entity.SysUser;
import com.ring.cloud.user.service.SysUserService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sys/user")
@Api(tags = "系统用户管理接口")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/page")
    @ApiOperation(value = "用户分页查询")
    public MResponse<?> page(@RequestBody @NotNull CommonPageQuery query) {
        try {
            PageResult<SysUser> pageResult = sysUserService.pageList(query);
            return MResponse.ok(pageResult);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info/{id}")
    @ApiOperation(value = "根据ID查询用户详情")
    public MResponse<?> getInfo(@PathVariable("id") Long id) {
        try {
            // 基类原生方法：根据主键查询
            SysUser user = sysUserService.selectByPrimaryKey(id);
            return MResponse.ok(user);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation(value = "新增用户")
    public MResponse<?> save(@RequestBody @NotNull SysUser sysUser) {
        try {
            sysUserService.save(sysUser);
            return MResponse.ok("新增成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "修改用户信息")
    public MResponse<?> update(@RequestBody @NotNull SysUser sysUser) {
        try {
            sysUserService.update(sysUser);
            return MResponse.ok("修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    @ApiOperation(value = "删除用户")
    public MResponse<?> remove(@PathVariable("id") Long id) {
        try {
            // 基类原生方法：根据主键删除
            sysUserService.deleteById(id);
            return MResponse.ok("删除成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 内部服务接口：供 cloud-auth 校验账号密码
     * 请求地址：/sys/user/inner/check
     */
    @PostMapping("/inner/check")
    public MResponse<?> innerCheckUser(@RequestBody UserCheckRequestDTO dto) {
        // 走 Service，不直连 Mapper
        SysUser user = sysUserService.getUserByUsername(dto.getUsername());

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (!user.getPassword().equals(dto.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("status", user.getStatus());
        return MResponse.ok(data);
    }
}