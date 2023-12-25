package com.sys.common;

public enum AppHttpCodeEnum {
    // 成功
    SUCCESS(200,"操作成功"),
    // 登录
    DATA_NULL(401,"数据为空"),
    JSON_ERROR(403,"JSON数据格式错误"),
    SEARACH_NULL(407,"未找到数据"),
    SYSTEM_EXCEPTION(405,"系统内部异常"),
    FILE_EMPTY(409,"文件为空");

    int code;
    String msg;

    AppHttpCodeEnum(int code, String errorMessage){
        this.code = code;
        this.msg = errorMessage;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}