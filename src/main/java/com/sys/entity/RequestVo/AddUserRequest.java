package com.sys.entity.RequestVo;

import lombok.Data;

/*
        张睿相   Java

        addUser的请求数据封装Vo类
*/
@Data
public class AddUserRequest {

    private String name;

    private String sex;

    private Integer deptId;

    private String phone;

    private String role;
}
