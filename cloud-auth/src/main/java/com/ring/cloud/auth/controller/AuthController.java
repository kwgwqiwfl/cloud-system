package com.ring.cloud.auth.controller;

import com.ring.cloud.auth.dto.LoginDTO;
import com.ring.cloud.auth.dto.LogoutDTO;
import com.ring.cloud.auth.dto.RefreshTokenDTO;
import com.ring.cloud.auth.dto.SmsLoginDTO;
import com.ring.cloud.auth.dto.ValidateTokenDTO;
import com.ring.cloud.auth.service.AuthService;
import com.ring.cloud.auth.vo.LoginVO;
import com.ring.cloud.auth.vo.ValidateVO;
import com.ring.cloud.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/sms/login")
    public Result<LoginVO> smsLogin(@Validated @RequestBody SmsLoginDTO dto) {
        return Result.success(authService.smsLogin(dto));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Validated @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Validated @RequestBody LogoutDTO dto) {
        authService.logout(dto);
        return Result.success();
    }

    @PostMapping("/validate")
    public Result<ValidateVO> validate(@Validated @RequestBody ValidateTokenDTO dto) {
        return Result.success(authService.validateToken(dto.getToken()));
    }
}