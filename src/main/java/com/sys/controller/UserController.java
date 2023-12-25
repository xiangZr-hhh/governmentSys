package com.sys.controller;

import com.sys.entity.RequestVo.AddUserRequest;
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
    public BaseResponse login(@RequestBody LoginRequestVo loginRequestVo){

//        判断请求参数是否为空
        if(loginRequestVo == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

//        请求登录
        BaseResponse result = usersService.login(loginRequestVo);
        return result;
    }

    @PostMapping("/getUserList/deptId")
    public BaseResponse getUserFromDept (@RequestBody UserFromDeptRequestVo userFromDeptRequestVo){

        int deptId = userFromDeptRequestVo.getDeptId();
//        判断参数是否为空
        if(deptId < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        BaseResponse result = usersService.getUserFromDept(deptId);

        return result;
    }

    @PostMapping("/addUser")
    public BaseResponse addUser (@RequestBody AddUserRequest addUserRequest){

//        判断参数是否为空
        if(addUserRequest == null){
            throw  new BusinessException(ErrorCode.NULL_ERROR);
        }

        BaseResponse result = usersService.addUser(addUserRequest);

        return result;
    }



}
