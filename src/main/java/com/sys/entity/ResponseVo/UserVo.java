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

    private String nickname;

    private String sex;

    private String phone1;

    private String phone2;

    private String role;

    public UserVo(Integer userId, String nickname, String role,String username) {
        this.userId = userId;
        this.nickname = name;
        this.role = role;
        this.name = username;
    }
}
