package com.sys.controller;

import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.*;
import com.sys.excption.BusinessException;
import com.sys.service.impl.UsersServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/getUserList/deptId")
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
    public ResponseResult editUser(@RequestBody EditUserRequestVo editUserRequestVo) {
//        判断参数是否为空
        if (editUserRequestVo == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = usersService.editUser(editUserRequestVo);

        return result;
    }

//    根据id删除用户接口
    @PostMapping("/delUser/userId")
    public ResponseResult delUser(@RequestBody UserIdRequest userIdRequest){

//        检测用户参数是否正确
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = usersService.delUserById(userId);

        return result;

    }

//    根据id重置用户密码
    @PostMapping("/resetPwd/userId")
    public ResponseResult resetPwdById(@RequestBody UserIdRequest userIdRequest){

//        检测用户id参数是否正确
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = usersService.resetPwdById(userId);

        return result;
    }

    /**
     * @Description: TODO 登录人员修改密码
     * @Date: 2024/1/3
     * @Param null:
     **/
    @PostMapping("/updatePwd")
    public ResponseResult updatePwd(@RequestBody UpdatePWDVo updatePWDVo){

        if(updatePWDVo == null){
            throw  new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = usersService.updatePWD(updatePWDVo);

        return result;
    }

}
