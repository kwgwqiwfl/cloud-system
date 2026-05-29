package com.ring.cloud.common.exception;

import com.ring.cloud.common.result.Result;
import com.ring.cloud.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.fail(e.getResultCode());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<?> handleSysException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL);
    }
}