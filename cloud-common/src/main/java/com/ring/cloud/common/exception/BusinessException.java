package com.ring.cloud.common.exception;

import com.ring.cloud.common.result.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /**
     * 原有构造：使用 ResultCode 自带提示
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 新增重载：ResultCode + 自定义错误信息（覆盖默认提示）
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}