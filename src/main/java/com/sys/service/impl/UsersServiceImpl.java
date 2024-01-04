package com.sys.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.constant.UserRoleConstant;
import com.sys.entity.RequestVo.EditUserRequestVo;
import com.sys.entity.RequestVo.UpdatePWDVo;
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
        if (username == null || username.equals("")) {
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR, "username参数为空");
        }
        if (password == null || password.equals("")) {
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR, "password参数为空");
        }

//      先验证用户是否存在
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getUsername, username);
        Users user = usersMapper.selectOne(usersWrapper);
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL, "用户名错误");
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
        if (!flag) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "密码错误");
        }

//        封装返回的vo类
        LoginResponseVo userVo = new LoginResponseVo();
        if (user.getUsername() != null) {
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
        List<Users> users = usersMapper.selectList(userWrapper.eq(Users::getDeptId, deptId).eq(Users::getState, UserRoleConstant.USER_NORMAL));

//        封装vo类
        List<UserVo> userVos = new ArrayList<>();
        for (Users user : users) {
            UserVo userVo = new UserVo(user.getId(), user.getUsername(), user.getSex(), user.getPhone(), user.getRoleId());
            userVos.add(userVo);
        }

        return ResponseResult.okResult(userVos);
    }

    @Override
    public ResponseResult addUser(UserVoRequest userVoRequest) {

//        判断userVo参数是否为空
        if (judgeUserVo(userVoRequest)) {

//            检测用户名是否重复
            LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(Users::getUsername, userVoRequest.getName());
            List<Users> checkResult = usersMapper.selectList(userWrapper);
            if (checkResult.size() > 0) {
                throw new BusinessException(AppHttpCodeEnum.USERNAME_DIPLICATE);
            }

//            创建对应实体类
            Users user = new Users(userVoRequest.getName(), userVoRequest.getPhone(), userVoRequest.getDeptId(), userVoRequest.getRole());

//            检测非必要参数sex是否添加
            if (!userVoRequest.getSex().equals("")) {
                user.setSex(userVoRequest.getSex());
            }

//            添加接口
            usersMapper.insert(user);
            return ResponseResult.okResult();
        }

        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_EXCEPTION);
    }


    //    edituser接口业务层
    @Override
    public ResponseResult editUser(EditUserRequestVo editUserRequestVo) {

        if (judgeEditUser(editUserRequestVo)) {

//            根据userId查询数据
            Users user = usersMapper.selectById(editUserRequestVo.getUserId());
//            未找到则抛出异常
            if (user == null) {
                throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
            }
//            如果可选属性sex不为空
            if (!editUserRequestVo.getSex().equals("")) {
                user.setSex(editUserRequestVo.getSex());
            }
            user.setPhone(editUserRequestVo.getPhone());
            user.setUsername(editUserRequestVo.getName());
            user.setRoleId(editUserRequestVo.getRole());

            int result = usersMapper.updateById(user);
            if (result != 0) {
                return ResponseResult.okResult();
            }

            return ResponseResult.errorResult(AppHttpCodeEnum.SEARACH_NULL);
        }

        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_EXCEPTION);
    }


    //    根据id删除用户
    @Override
    public ResponseResult delUserById(int userId) {
//        搜索该用户的实体类
        Users users = usersMapper.selectById(userId);
        if (users == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }
//        修改其删除状态
        users.setState(UserRoleConstant.USER_DEPT);
        usersMapper.updateById(users);

        return ResponseResult.okResult();
    }


    //    判断添加用户接口参数
    public boolean judgeUserVo(UserVoRequest userVoRequest) {
        if (userVoRequest.getRole().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "role参数为空");
        }

        if (userVoRequest.getPhone().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "phone参数为空");
        }

        if (userVoRequest.getName().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "name参数为空");
        }

        if (userVoRequest.getDeptId() <= 0) {
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR, "deptId参数错误");
        }

        return true;
    }


    //        判断editUserRequestVo参数是否为空
    public boolean judgeEditUser(EditUserRequestVo editUserRequestVo) {

        if (editUserRequestVo.getName().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "phone参数为空");
        }
        if (editUserRequestVo.getRole().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "role参数为空");
        }
        if (editUserRequestVo.getPhone().equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "phone参数为空");
        }
        if (editUserRequestVo.getUserId() <= 0) {
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR, "userId参数错误");
        }

        return true;
    }

    //  根据id重置密码
    @Override
    public ResponseResult resetPwdById(int userId) {

//        根据id获取对应用户实体类
        Users user = usersMapper.selectById(userId);
//        如果未找到则抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

        user.setPassword("EC95256710DC067BB0C111B3CD619DBA14FADAC617A56EDE0883230B");
        usersMapper.updateById(user);

        return ResponseResult.okResult();
    }



    /**
     * @Description: TODO 更新用户密码
     * @Date: 2024/1/3
     * @Param updatePWDVo:
     **/
    @Override
    public ResponseResult updatePWD(UpdatePWDVo updatePWDVo) {

        String userId = updatePWDVo.getUerId();
        if (userId.equals("") || userId == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "userId为空");
        }

        String oldPwd = updatePWDVo.getOldPwd();
        if (oldPwd.equals("") || oldPwd == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "oldPwd为空");
        }

        String newPwd = updatePWDVo.getNewPwd();
        if (newPwd.equals("") || newPwd == null) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "newPwd为空");
        }

        Users user = usersMapper.selectById(Integer.parseInt(userId));
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

        try {
//            如果旧密码不正确
            if (MyMD5Util.validPassword(oldPwd, user.getPassword())) {
                throw new BusinessException(AppHttpCodeEnum.JSON_ERROR,"旧密码错误");
            }
//             无误则设置新密码
            user.setPassword(MyMD5Util.getEncryptedPwd(newPwd));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        usersMapper.updateById(user);

        return ResponseResult.okResult();
    }


}
