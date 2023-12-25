package com.sys.entity.ResponseVo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/*
        张睿相   Java
*/
@Data
public class LoginResponseVo {

//    用户id
    private Integer userId;

//    用户真实名称
    private String realName;

//    用户身份权限
    private String role;

}
