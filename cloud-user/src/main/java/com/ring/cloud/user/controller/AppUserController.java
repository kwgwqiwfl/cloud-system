package com.ring.cloud.user.controller;

import com.ring.cloud.user.dto.*;
import com.ring.cloud.user.entity.SysUser;
import com.ring.cloud.user.service.SysUserService;
import com.ring.cloud.user.vo.AppLoginVO;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/app/user")
@Api(tags = "APP 用户接口")
public class AppUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * APP 账号密码登录
     */
    @PostMapping("/login")
    @ApiOperation(value = "APP 账号密码登录")
    public MResponse<?> login(@RequestBody @NotNull AppLoginDTO dto) {
        try {
            AppLoginVO vo = sysUserService.appLogin(dto);
            return MResponse.ok(vo);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/register")
    @ApiOperation(value = "APP用户注册")
    public MResponse<?> register(@RequestBody @NotNull AppRegisterDTO dto) {
        try {
            SysUser user = sysUserService.appRegister(dto);
            return MResponse.ok(user);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取当前登录用户信息")
    public MResponse<?> info(@RequestHeader("Authorization") String token) {
        try {
            SysUser user = sysUserService.getUserInfoByToken(token);
            // 可按需脱敏：清空密码等敏感字段
            user.setPassword(null);
            return MResponse.ok(user);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @ApiOperation(value = "APP 退出登录")
    public MResponse<?> logout() {
        try {
            return MResponse.ok("退出成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 刷新token
     */
    @PostMapping("/refresh-token")
    @ApiOperation(value = "刷新Token")
    public MResponse<?> refreshToken(@RequestBody @NotNull RefreshTokenDTO dto) {
        try {
            String newToken = sysUserService.refreshToken(dto);
            return MResponse.ok(newToken);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/update-pwd")
    @ApiOperation(value = "修改密码")
    public MResponse<?> updatePwd(@RequestHeader("Authorization") String token,
                                  @RequestBody @NotNull UpdatePwdDTO dto) {
        try {
            sysUserService.updatePassword(token, dto);
            return MResponse.ok("密码修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 忘记密码重置
     */
    @PostMapping("/reset-pwd")
    @ApiOperation(value = "忘记密码重置")
    public MResponse<?> resetPwd(@RequestBody @NotNull ResetPwdDTO dto) {
        try {
            sysUserService.resetPassword(dto);
            return MResponse.ok("密码重置成功，请使用新密码登录");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 更新个人资料
     */
    @PostMapping("/update-profile")
    @ApiOperation(value = "更新个人资料")
    public MResponse<?> updateProfile(@RequestHeader("Authorization") String token,
                                      @RequestBody @NotNull UpdateProfileDTO dto) {
        try {
            sysUserService.updateProfile(token, dto);
            return MResponse.ok("资料更新成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    /**
     * 验证登录状态
     */
    @GetMapping("/check-login")
    @ApiOperation(value = "验证当前是否已登录")
    public MResponse<?> checkLogin(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            boolean isLogin = false;
            if (token != null && !token.trim().isEmpty()) {
                isLogin = sysUserService.checkLoginStatus(token);
            }
            return MResponse.ok(isLogin);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/sms-login")
    @ApiOperation(value = "手机号验证码登录")
    public MResponse<?> smsLogin(@RequestBody @NotNull SmsLoginDTO dto) {
        try {
            AppLoginVO vo = sysUserService.smsLogin(dto);
            return MResponse.ok(vo);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/send-code")
    @ApiOperation(value = "发送登录验证码")
    public MResponse<?> sendCode(@RequestBody @NotNull SendCodeDTO dto) {
        try {
            // 这里对接短信SDK，生成验证码存入Redis
            return MResponse.ok("验证码已发送");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}