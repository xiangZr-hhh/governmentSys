package com.sys.entity.RequestVo;

import lombok.Data;

/*
        张睿相   Java

        Login接口的Vo封装类
*/
@Data
public class LoginRequestVo {

    //用户名
    private String username;

    //密码，md5加密
    private String password;


}
