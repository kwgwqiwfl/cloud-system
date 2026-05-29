package com.ring.cloud.auth.service;

import com.ring.cloud.auth.dto.LoginDTO;
import com.ring.cloud.auth.dto.LogoutDTO;
import com.ring.cloud.auth.dto.RefreshTokenDTO;
import com.ring.cloud.auth.dto.SmsLoginDTO;
import com.ring.cloud.auth.vo.LoginVO;
import com.ring.cloud.auth.vo.ValidateVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    LoginVO smsLogin(SmsLoginDTO dto);

    LoginVO refreshToken(RefreshTokenDTO dto);

    void logout(LogoutDTO dto);

    ValidateVO validateToken(String token);
}