package com.sys.excption;


import com.sys.common.AppHttpCodeEnum;

/**
 * 自定义异常类
 *
 * @author chy
 */
public class BusinessException extends RuntimeException {

    private final int code;

    private final String description;


    public BusinessException(AppHttpCodeEnum errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = errorCode.getMsg();
    }

    public BusinessException(AppHttpCodeEnum errorCode, String description) {
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
