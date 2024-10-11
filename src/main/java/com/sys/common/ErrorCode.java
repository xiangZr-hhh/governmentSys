package com.sys.common;

public enum ErrorCode {
    // 成功
    SUCCESS(200,"操作成功"),
    // 登录
    DATA_NULL(401,"数据为空"),
    JSON_ERROR(403,"JSON数据格式错误"),
    SEARACH_NULL(407,"未找到数据"),
    SYSTEM_EXCEPTION(405,"系统内部异常"),
    FILE_EMPTY(409,"文件为空"),
    TASK_STATUE_NOT(410,"任务状态错误"),
    USERNAME_DIPLICATE(4401,"用户名重复"),
    STATUE_NOT_ALL(4402,"事件状态未统一"),
    STATUE_ERROR(4403,"事件状态错误"),
    REQUEST_BODY_ERROR(4404,"请求参数错误"),
    PAEEWORD_CONVERSION_ERROR(4405,"密码转换错误"),
    ROLE_NO_ALLOW(4406,"权限名额已满"),
    TASKNO_REPEATE(4407,"事项编号重复"),
    DEPT_NO_CONTAIN_USER(4408,"部门内无用户"),
    TOKEN_ERROR(4409,"Token错误"),;

    int code;
    String msg;

    ErrorCode(int code, String errorMessage){
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