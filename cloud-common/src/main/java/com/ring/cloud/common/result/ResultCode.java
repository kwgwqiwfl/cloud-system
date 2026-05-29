package com.ring.cloud.common.result;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "服务器异常"),
    UNAUTHORIZED(401, "未授权，请重新登录"),
    FORBIDDEN(403, "没有访问权限"),
    PARAM_ERROR(400, "参数错误"),

    // 新增 登录认证业务码
    USER_NOT_EXIST(1001, "账号不存在"),
    PASSWORD_ERROR(1002, "账号或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    SMS_CODE_ERROR(1004, "验证码错误或已过期"),
    TOKEN_INVALID(1005, "Token无效或已过期"),
    REFRESH_TOKEN_ERROR(1006, "刷新令牌无效");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}