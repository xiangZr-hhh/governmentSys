package com.sys.entity.ResponseVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
        张睿相   Java

        UserVo封装类
*/
@Data
@AllArgsConstructor
public class UserVo {

    private Integer userId;

    private String name;

    private String sex;

    private String phone;

    private String role;

}
