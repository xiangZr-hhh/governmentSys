package com.sys.controller;

import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.*;
import com.sys.excption.BusinessException;
import com.sys.service.impl.UsersServiceImpl;
import com.sys.utils.TaskUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/*
        张睿相   Java
*/
@RestController
public class UserController {

    @Autowired
    private UsersServiceImpl usersService;


    @PostMapping("/login")
    public ResponseResult login(@RequestBody @Validated LoginRequestVo loginRequestVo, BindingResult bindingResult) {

// 判断是否有参数错误
        if (bindingResult.hasErrors()) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, TaskUtils.getValidatedErrorList(bindingResult));
        }

//        请求登录
        ResponseResult result = usersService.login(loginRequestVo);
        return result;
    }

    @GetMapping("/getUserList/deptId")
    public ResponseResult getUserFromDept(@RequestParam Integer deptId) {

        if (deptId == null || deptId <= 0) {
            throw new BusinessException(ErrorCode.JSON_ERROR);
        }

        return usersService.getUserFromDept(deptId);
    }



    @PostMapping("/addUser")
    public ResponseResult addUser(@RequestBody @Validated UserInfoRequestVo userVoRequest, BindingResult bindingResult) {
// 判断是否有参数错误
        if (bindingResult.hasErrors()) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, TaskUtils.getValidatedErrorList(bindingResult));
        }

        return usersService.addUser(userVoRequest.getUserInfo());
    }

    @PutMapping("/editUser")
    public ResponseResult editUser(@RequestBody UserEditInfoRequestVo userInfo) {
//        判断参数是否为空
        if (userInfo.getUserInfo() == null) {
            throw new BusinessException(ErrorCode.DATA_NULL);
        }

        ResponseResult result = usersService.editUser(userInfo.getUserInfo());

        return result;
    }

    //    根据id删除用户接口
    @DeleteMapping("/delUser/userId")
    public ResponseResult delUser(@RequestParam Integer userId) {

//        检测用户参数是否正确
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.JSON_ERROR);
        }

        ResponseResult result = usersService.delUserById(userId);

        return result;

    }

    //    根据id重置用户密码
    @PutMapping("/resetPwd/userId")
    public ResponseResult resetPwdById(@RequestParam Integer userId) {

//        检测用户id参数是否正确
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.REQUEST_BODY_ERROR);
        }

        return usersService.resetPwdById(userId);

    }


    /**
     * @Description: 登录人员修改密码
     * @Date: 2024/1/21
     * @Param updatePWDVo:
     **/
    @PutMapping("/updatePwd")
    public ResponseResult updatePwd(@RequestBody @Validated UpdatePWDVo updatePWDVo, BindingResult bindingResult) {

// 判断是否有参数错误
        if (bindingResult.hasErrors()) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, TaskUtils.getValidatedErrorList(bindingResult));
        }

        return usersService.updatePWD(updatePWDVo);
    }

}
