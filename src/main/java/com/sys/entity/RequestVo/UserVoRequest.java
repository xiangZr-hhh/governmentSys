package com.sys.entity.RequestVo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/*
        张睿相   Java

        addUser的请求数据封装Vo类
*/
@Data
public class UserVoRequest {

    @NotBlank(message = "名称不能为空")
    private String nickname;

    @NotBlank(message = "名称不能为空")
    private String name;

    private String sex;

    @NotNull(message = "部门id不能为空")
    private Integer deptId;

    @NotBlank(message = "电话1不能为空")
    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话格式错误")
    private String phone1;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话格式错误")
    private String phone2;

    @NotBlank(message = "用户权限不能为空")
    private String role;
}
