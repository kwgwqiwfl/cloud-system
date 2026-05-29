package com.ring.cloud.user.service.impl;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.common.service.JwtService;
import com.ring.cloud.user.dto.*;
import com.ring.cloud.user.entity.SysUser;
import com.ring.cloud.user.mybatis.mapper.SysUserMapper;
import com.ring.cloud.user.service.SysUserService;
import com.ring.cloud.user.vo.AppLoginVO;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SysUserServiceImpl extends EntityClassServiceImpl<SysUser> implements SysUserService {

    @Autowired
    private SysUserMapper mapper;

    @Autowired
    private JwtService jwtService;
    @Value("${jwt.expire}")
    private long jwtExpire;

    @Override
    public MyIdableMapper<SysUser> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SysUser> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<SysUser> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public AppLoginVO appLogin(AppLoginDTO dto) {
        // 1. 根据用户名查询用户
        SysUser user = mapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }

        // 2. 校验密码（和已有密码加密逻辑保持一致）
        if (!dto.getPassword().equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 3. 校验账号状态（启用/禁用）
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 4. 生成 Token（补齐 3 个参数）
        String token = jwtService.createToken(
                user.getId().toString(),
                user.getUsername(),
                jwtExpire
        );

        // 5. 组装返回对象
        AppLoginVO vo = new AppLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setToken(token);

        return vo;
    }

    @Override
    public SysUser appRegister(AppRegisterDTO dto) {
        // 校验账号是否已存在
        SysUser exist = mapper.selectByUsername(dto.getUsername());
        if (exist != null) {
            throw new IllegalArgumentException("账号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        // 默认启用
        user.setStatus(1);

        // 基类自带 save 方法
        return save(user);
    }

    @Override
    public SysUser getUserInfoByToken(String token) {
        // 校验Token有效性
        if (!jwtService.validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期，请重新登录");
        }
        // 解析用户ID（使用标准方法名）
        String userIdStr = jwtService.getUserIdByToken(token);
        Long userId = Long.parseLong(userIdStr);
        // 基类方法根据主键查询用户
        return selectByPrimaryKey(userId);
    }

    @Override
    public String refreshToken(RefreshTokenDTO dto) {
        String token = dto.getToken();
        if (!jwtService.validateToken(token)) {
            throw new IllegalArgumentException("token无效，无法刷新");
        }
        // 统一使用标准方法名
        String userIdStr = jwtService.getUserIdByToken(token);
        String username = jwtService.getUsernameByToken(token);
        // 生成新token
        return jwtService.createToken(userIdStr, username, jwtExpire);
    }

    @Override
    public void updatePassword(String token, UpdatePwdDTO dto) {
        if (!jwtService.validateToken(token)) {
            throw new IllegalArgumentException("请先登录");
        }
        // 统一使用标准方法名
        Long userId = Long.parseLong(jwtService.getUserIdByToken(token));
        SysUser user = selectByPrimaryKey(userId);

        if (!user.getPassword().equals(dto.getOldPwd())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassword(dto.getNewPwd());
        updateByPrimaryKeySelective(user);
    }

    @Override
    public void resetPassword(ResetPwdDTO dto) {
        // 这里只做结构，验证码校验逻辑自行对接短信服务
        // 示例：校验验证码成功后，根据手机号更新密码
        SysUser user = mapper.selectByPhone(dto.getPhone());
        if (user == null) {
            throw new IllegalArgumentException("该手机号未注册");
        }
        user.setPassword(dto.getNewPwd());
        updateByPrimaryKeySelective(user);
    }

    @Override
    public void updateProfile(String token, UpdateProfileDTO dto) {
        if (!jwtService.validateToken(token)) {
            throw new IllegalArgumentException("请先登录");
        }
        // 统一使用标准方法名
        Long userId = Long.parseLong(jwtService.getUserIdByToken(token));
        SysUser user = selectByPrimaryKey(userId);

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        updateByPrimaryKeySelective(user);
    }

    @Override
    public boolean checkLoginStatus(String token) {
        // 直接复用Jwt工具校验有效性
        return jwtService.validateToken(token);
    }

    @Override
    public AppLoginVO smsLogin(SmsLoginDTO dto) {
        // 1. 根据手机号查询用户
        SysUser user = mapper.selectByPhone(dto.getPhone());
        if (user == null) {
            throw new IllegalArgumentException("该手机号未注册");
        }

        // 2. 校验验证码（这里只留校验位置，实际对接短信服务/Redis验证）
        // 示例：if (!redisUtil.get("sms:" + phone).equals(code))
        // throw new IllegalArgumentException("验证码错误或已过期");

        // 3. 校验账号状态
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 4. 生成 Token
        String token = jwtService.createToken(
                user.getId().toString(),
                user.getUsername(),
                jwtExpire
        );

        // 5. 组装返回
        AppLoginVO vo = new AppLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setToken(token);
        return vo;
    }

    @Override
    public SysUser getUserByUsername(String username) {
        // 调用你原有 Mapper 方法
        return mapper.selectByUsername(username);
    }
}