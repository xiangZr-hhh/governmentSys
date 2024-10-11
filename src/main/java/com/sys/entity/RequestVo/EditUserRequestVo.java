package com.sys.entity.RequestVo;

import lombok.Data;

import javax.validation.constraints.Pattern;

/*
        张睿相   Java

        editUser接口接收请求体实体类
*/
@Data
public class EditUserRequestVo {

    private int userId;

    private String name;

    private String nickname;

    private String sex;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话格式错误")
    private String phone1;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话格式错误")
    private String phone2;

    private String role;

}
