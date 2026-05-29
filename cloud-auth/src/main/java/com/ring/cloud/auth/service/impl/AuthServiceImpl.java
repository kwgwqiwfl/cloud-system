package com.ring.cloud.auth.service.impl;

import com.ring.cloud.auth.dto.LoginDTO;
import com.ring.cloud.auth.dto.LogoutDTO;
import com.ring.cloud.auth.dto.RefreshTokenDTO;
import com.ring.cloud.auth.dto.SmsLoginDTO;
import com.ring.cloud.auth.feign.UserFeignClient;
import com.ring.cloud.auth.feign.dto.UserCheckDTO;
import com.ring.cloud.auth.feign.dto.UserCheckRequestDTO;
import com.ring.cloud.auth.service.AuthService;
import com.ring.cloud.auth.service.TokenService;
import com.ring.cloud.auth.vo.LoginVO;
import com.ring.cloud.auth.vo.ValidateVO;
import com.ring.cloud.common.exception.BusinessException;
import com.ring.cloud.common.result.Result;
import com.ring.cloud.common.result.ResultCode;
import com.ring.cloud.common.service.JwtService;
import com.ring.welkin.common.core.ml.MResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtService jwtService;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 构建请求参数
        UserCheckRequestDTO requestDto = new UserCheckRequestDTO();
        requestDto.setUsername(dto.getUsername());
        requestDto.setPassword(dto.getPassword());

        // 2. 调用用户服务，用 MResponse<?> 接收
        MResponse<?> response = userFeignClient.checkUser(requestDto);

        // 校验响应状态（status != 200 表示调用失败）
        if (response.getStatus() != 200) {
            // 用通用的 FAIL 异常码，将返回的 message 传入异常
            throw new BusinessException(ResultCode.FAIL, response.getMessage());
        }

        // 3. 把返回的 data 强转为 Map，手动解析
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        if (data == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 解析字段到 UserCheckDTO
        UserCheckDTO user = new UserCheckDTO();
        user.setUserId(Long.valueOf(data.get("userId").toString()));
        user.setUsername((String) data.get("username"));
        user.setNickname((String) data.get("nickname"));
        user.setStatus((Integer) data.get("status"));

        // 账号状态校验
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成令牌（保持你原有的逻辑不变）
        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.generateToken(user.getUserId()));
        vo.setRefreshToken(tokenService.generateRefreshToken(user.getUserId()));
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    public LoginVO smsLogin(SmsLoginDTO dto) {
        // 后续自行补充：验证码校验 + 调用用户服务根据手机号查询用户
        // 示例抛异常：验证码错误
        // throw new BusinessException(ResultCode.SMS_CODE_ERROR);

        // 临时占位，后续替换真实逻辑
        return null;
    }

    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();
        // 校验刷新令牌是否在黑名单
        if (tokenService.isBlack(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_ERROR);
        }
        // 校验令牌合法性
        if (!jwtService.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_ERROR);
        }
        // 解析用户ID
        String userIdStr = jwtService.getUserIdByToken(refreshToken);
        Long userId = Long.valueOf(userIdStr);

        // 生成新令牌
        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.generateToken(userId));
        vo.setRefreshToken(tokenService.generateRefreshToken(userId));
        return vo;
    }

    @Override
    public void logout(LogoutDTO dto) {
        // 退出登录，将token加入黑名单
        tokenService.addBlack(dto.getToken());
    }

    @Override
    public ValidateVO validateToken(String token) {
        // 1. 判断是否在黑名单
        if (tokenService.isBlack(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        // 2. 校验token签名、是否过期
        if (!jwtService.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        // 3. 解析用户ID
        String userIdStr = jwtService.getUserIdByToken(token);
        ValidateVO vo = new ValidateVO();
        vo.setValid(true);
        vo.setUserId(Long.valueOf(userIdStr));
        return vo;
    }
}