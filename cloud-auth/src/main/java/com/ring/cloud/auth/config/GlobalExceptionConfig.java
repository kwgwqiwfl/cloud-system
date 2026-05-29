package com.ring.cloud.auth.config;

import com.ring.cloud.common.exception.BusinessException;
import com.ring.cloud.common.result.Result;
import com.ring.cloud.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionConfig {

    // 捕获自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.fail(e.getResultCode());
    }

    // 捕获参数校验异常 @NotBlank
    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public Result<?> handleParamError(javax.validation.ConstraintViolationException e) {
        log.error("参数校验失败:{}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR);
    }

    // 捕获全局未知系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleAllException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL);
    }
}