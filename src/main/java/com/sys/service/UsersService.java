package com.sys.service;

import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.EditUserRequestVo;
import com.sys.entity.RequestVo.UpdatePWDVo;
import com.sys.entity.RequestVo.UserVoRequest;
import com.sys.entity.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.entity.RequestVo.LoginRequestVo;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * (Users)表服务接口
 *
 * @author zrx
 * @since 2023-12-21 20:13:42
 */
public interface UsersService extends IService<Users> {

    ResponseResult login(LoginRequestVo loginRequestVo);

    ResponseResult getUserFromDept(Integer deptId);

    ResponseResult addUser(UserVoRequest userVoRequest);

    ResponseResult editUser(EditUserRequestVo editUserRequestVo);

    ResponseResult delUserById(int userId);

    //  根据id重置密码
    ResponseResult resetPwdById(@RequestParam Integer userId);

    ResponseResult updatePWD(UpdatePWDVo updatePWDVo);

    ResponseResult getUserListByDeptId(Long deptId);
}

