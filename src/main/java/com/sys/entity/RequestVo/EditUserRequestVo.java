package com.sys.entity.RequestVo;

import lombok.Data;

/*
        张睿相   Java

        editUser接口接收请求体实体类
*/
@Data
public class EditUserRequestVo {

    private int userId;

    private String name;

    private String sex;

    private String phone;

    private String role;

}
