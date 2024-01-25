package com.sys.excption;


import com.sys.common.ErrorCode;

/**
 * 自定义异常类
 *
 * @author chy
 */
public class BusinessException extends RuntimeException {

    private final int code;

    private final String description;


    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = errorCode.getMsg();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(description);
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
