package com.ring.cloud.user.service;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.dto.*;
import com.ring.cloud.user.entity.SysUser;
import com.ring.cloud.user.vo.AppLoginVO;
import com.ring.cloud.user.vo.UserCheckVO;
import com.ring.welkin.common.persistence.service.BaseIdableService;

public interface SysUserService extends BaseIdableService<Long, SysUser> {

    /**
     * 管理端分页列表
     */
    PageResult<SysUser> pageList(CommonPageQuery query);

    /**
     * APP 账号密码登录
     * @param dto 登录参数
     * @return 登录信息（含token）
     */
    AppLoginVO appLogin(AppLoginDTO dto);

    /**
     * APP 用户注册
     */
    SysUser appRegister(AppRegisterDTO dto);

    /**
     * 根据Token获取当前登录用户信息
     */
    SysUser getUserInfoByToken(String token);

    /**
     * 刷新token
     */
    String refreshToken(RefreshTokenDTO dto);

    /**
     * 修改密码
     */
    void updatePassword(String token, UpdatePwdDTO dto);

    /**
     * 忘记密码重置
     */
    void resetPassword(ResetPwdDTO dto);

    /**
     * 更新个人资料
     */
    void updateProfile(String token, UpdateProfileDTO dto);

    /**
     * 验证当前登录状态
     * @param token 令牌
     * @return true=已登录 false=未登录
     */
    boolean checkLoginStatus(String token);

    /**
     * 手机号验证码登录
     */
    AppLoginVO smsLogin(SmsLoginDTO dto);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getUserByUsername(String username);
}