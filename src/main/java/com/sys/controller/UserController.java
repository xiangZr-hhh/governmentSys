package com.sys.controller;

import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.UserVoRequest;
import com.sys.entity.RequestVo.LoginRequestVo;
import com.sys.entity.RequestVo.UserFromDeptRequestVo;
import com.sys.excption.BusinessException;
import com.sys.service.impl.UsersServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
        张睿相   Java
*/
@RestController
public class UserController {

    @Autowired
    private UsersServiceImpl usersService;


    @PostMapping("/login")
    public ResponseResult login(@RequestBody LoginRequestVo loginRequestVo) {

//        判断请求参数是否为空
        if (loginRequestVo == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        请求登录
        ResponseResult result = usersService.login(loginRequestVo);
        return result;
    }

    @PostMapping("/getUserList/deptId")
    public ResponseResult getUserFromDept(@RequestBody UserFromDeptRequestVo userFromDeptRequestVo) {

        int deptId = userFromDeptRequestVo.getDeptId();
//        判断参数是否为空
        if (deptId < 0) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = usersService.getUserFromDept(deptId);

        return result;
    }

    @PostMapping("/addUser")
    public ResponseResult addUser(@RequestBody UserVoRequest userVoRequest) {

//        判断参数是否为空
        if (userVoRequest == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = usersService.addUser(userVoRequest);

        return result;
    }

    @PostMapping("/editUser")
    public ResponseResult editUser(@RequestBody UserVoRequest userVoRequest) {
//        判断参数是否为空
        if (userVoRequest == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = usersService.editUser(userVoRequest);

        return result;
    }


}
