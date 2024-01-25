package com.sys.entity.ResponseVo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
        张睿相   Java

        UserVo封装类
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {

    private Integer userId;

    private String name;

    private String sex;

    private String phone;

    private String role;

    public UserVo(Integer userId, String name, String role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
    }
}
