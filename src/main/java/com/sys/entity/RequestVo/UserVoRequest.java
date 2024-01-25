package com.sys.entity.RequestVo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/*
        张睿相   Java

        addUser的请求数据封装Vo类
*/
@Data
public class UserVoRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    private String sex;

    @NotNull(message = "部门id不能为空")
    private Integer deptId;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "用户权限不能为空")
    private String role;
}
