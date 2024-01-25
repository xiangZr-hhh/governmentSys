package com.sys.excption;



import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Aop思想
 *
 * 全局异常处理器
 * @author chy
 */
@RestControllerAdvice
@Slf4j
public class GlobalException extends Throwable {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResponseResult.errorResult(e.getCode(),e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseResult runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);
        return ResponseResult.errorResult(ErrorCode.SYSTEM_EXCEPTION,e.getMessage());
    }

}
