package com.sys.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.UserVoRequest;
import com.sys.entity.RequestVo.LoginRequestVo;
import com.sys.entity.ResponseVo.LoginResponseVo;
import com.sys.entity.ResponseVo.UserVo;
import com.sys.excption.BusinessException;
import com.sys.mapper.DeptMapper;
import com.sys.mapper.UsersMapper;
import com.sys.service.UsersService;
import com.sys.utils.MyMD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sys.entity.Users;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * (Users)表服务实现类
 *
 * @author zrx
 * @since 2023-12-21 20:13:42
 */
@Service("usersService")
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private DeptMapper deptMapper;


    @Override
    public ResponseResult login(LoginRequestVo loginRequestVo) {

//        获取参数
        String username = loginRequestVo.getUsername();
        String password = loginRequestVo.getPassword();

//        判断参数是否为空
        if(username == null||username.equals("")){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR,"username参数为空");
        }
        if(password == null||password.equals("")){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR,"password参数为空");
        }

//      先验证用户是否存在
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getUsername,username);
        Users user = usersMapper.selectOne(usersWrapper);
        if(user == null){
            throw  new BusinessException(AppHttpCodeEnum.SEARACH_NULL,"用户名错误");
        }

//        在建验密码是否正确
        boolean flag = false;
        try {
             flag = MyMD5Util.validPassword(password, user.getPassword());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
//        如果未查询到用户，抛出异常
        if(!flag){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"密码错误");
        }

//        封装返回的vo类
        LoginResponseVo userVo = new LoginResponseVo();
        if(user.getUsername() != null){
            userVo.setRealName(user.getUsername());
        }
        userVo.setUserId(user.getId());
        userVo.setRole(user.getRoleId());

//        返回数据
        return ResponseResult.okResult(userVo);
    }


    @Override
    public ResponseResult getUserFromDept(Integer deptId) {
//        查询数据
        LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
        List<Users> users = usersMapper.selectList(userWrapper.eq(Users::getDeptId,deptId));

//        封装vo类
        List<UserVo> userVos = new ArrayList<>();
        for(Users user:users){
            UserVo userVo = new UserVo(user.getId(),user.getUsername(),user.getSex(),user.getPhone(),user.getRoleId());
            userVos.add(userVo);
        }

        return ResponseResult.okResult(userVos);
    }

    @Override
    public ResponseResult addUser(UserVoRequest userVoRequest) {

//        判断userVo参数是否为空
        judgeUserVo(userVoRequest);

        Users user = new Users(userVoRequest.getName(), userVoRequest.getPhone(), userVoRequest.getDeptId(), userVoRequest.getRole());

        if(!userVoRequest.getSex().equals("")){
            user.setSex(userVoRequest.getSex());
        }

        usersMapper.insert(user);
        return ResponseResult.okResult();

    }

    @Override
    public ResponseResult editUser(UserVoRequest userVoRequest) {

//        判断userVo参数是否为空
        judgeUserVo(userVoRequest);
    }


//    判断用户参数
    public boolean judgeUserVo(UserVoRequest userVoRequest){
        if(userVoRequest.getRole().equals("")){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"role参数为空");
        }

        if(userVoRequest.getPhone().equals("")){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"phone参数为空");
        }

        if(userVoRequest.getName().equals("")){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"name参数为空");
        }

        if(userVoRequest.getDeptId() < 0){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"deptId为空");
        }

        return true;
    }
}
