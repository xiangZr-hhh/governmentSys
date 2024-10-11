package com.sys.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.constant.DeptStateConstant;
import com.sys.constant.UserRoleConstant;
import com.sys.constant.UserStateConstant;
import com.sys.entity.Dept;
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
import org.springframework.web.bind.annotation.RequestParam;

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
        String username = loginRequestVo.getLoginForm().getUsername();
        String password = loginRequestVo.getLoginForm().getPassword();

//      先验证用户是否存在
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getUsername, username).eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users user = usersMapper.selectOne(usersWrapper);
        if (user == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户名错误");
        }


//        在建验密码是否正确
        boolean flag = false;
        try {
            flag = MyMD5Util.validPassword(password, user.getPassword());
        } catch (NoSuchAlgorithmException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        } catch (UnsupportedEncodingException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        }
//        不正确则抛出对应错误
        if (!flag) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL, "密码错误");
        }

//        封装返回的vo类
        LoginResponseVo userVo = new LoginResponseVo();
        if (user.getUsername() != null) {
            userVo.setRealName(user.getNickname());
        }
        userVo.setUserId(user.getId());
        userVo.setRole(user.getRoleId());
        if(!user.getRoleId().equals(UserRoleConstant.SYSTEM_MANAGER)) {
            userVo.setDeptName(deptMapper.selectById(user.getDeptId()).getDeptName());
        }else {
            userVo.setDeptName("系统管理员");
        }

//        返回数据
        return ResponseResult.okResult(userVo);
    }


    @Override
    public ResponseResult getUserFromDept(Integer deptId) {
//        查询数据
        LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(Users::getState, UserStateConstant.USER_NORMAL);
        List<Users> users = usersMapper.selectList(userWrapper.eq(Users::getDeptId, deptId).eq(Users::getState, UserRoleConstant.USER_NORMAL));

//        封装vo类
        List<UserVo> userVos = new ArrayList<>();
        for (Users user : users) {
            UserVo userVo = new UserVo(user.getId(), user.getNickname(), user.getRoleId() ,user.getUsername());
            if(user.getPhone1() != null){
                userVo.setPhone1(user.getPhone1());
            }
            if(user.getPhone2() != null){
                userVo.setPhone2(user.getPhone2());
            }
            userVo.setNickname(user.getNickname());
            if(user.getSex() != null){
                userVo.setSex(user.getSex());
            }
            userVos.add(userVo);
        }

        return ResponseResult.okResult(userVos);
    }


    @Override
    public ResponseResult addUser(UserVoRequest userVoRequest) {

//            检测用户名是否重复
        LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(Users::getUsername, userVoRequest.getName()).eq(Users::getState, UserStateConstant.USER_NORMAL);
        List<Users> checkResult = usersMapper.selectList(userWrapper);
        if (checkResult.size() > 0) {
            return  ResponseResult.errorResult(ErrorCode.USERNAME_DIPLICATE);
        }

        Users user = new Users(userVoRequest.getName(), userVoRequest.getPhone1(),userVoRequest.getPhone2(), userVoRequest.getDeptId(), userVoRequest.getRole());
        user.setNickname(userVoRequest.getNickname());


//        检测部门id
        Dept dept = deptMapper.selectById(userVoRequest.getDeptId());
        if(dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)){
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL,"添加用户对应的部门id为空");
        }


//            检测非必要参数sex是否添加
        if (!userVoRequest.getSex().equals("")) {
            user.setSex(userVoRequest.getSex());
        }


        ResponseResult result = checkUserRole(userVoRequest.getRole(),userVoRequest.getDeptId());
        if(result.getCode() != 200 ){
            return result;
        }

//            向数据库添加数据
        usersMapper.insert(user);

        return ResponseResult.okResult();
    }


    //    edituser接口业务层
    @Override
    public ResponseResult editUser(EditUserRequestVo editUserRequestVo) {

        if (judgeEditUser(editUserRequestVo)) {

//            根据userId查询数据
            Users user = usersMapper.selectById(editUserRequestVo.getUserId());
//            未找到则抛出异常
            if (user == null || user.getState().equals(UserStateConstant.USER_IS_DELETE)) {
                throw new BusinessException(ErrorCode.SEARACH_NULL);
            }
//            如果可选属性sex不为空
            if (!editUserRequestVo.getSex().equals("")) {
                user.setSex(editUserRequestVo.getSex());
            }
            user.setPhone1(editUserRequestVo.getPhone1());
            user.setPhone2(editUserRequestVo.getPhone2());

            if(!user.getUsername().equals(editUserRequestVo.getName())) {
                LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
                usersLambdaQueryWrapper.eq(Users::getUsername, editUserRequestVo.getName());
                List<Users> users = usersMapper.selectList(usersLambdaQueryWrapper);
                if(users.size() > 0){
                    return ResponseResult.errorResult(ErrorCode.USERNAME_DIPLICATE);
                }
            }

            user.setUsername(editUserRequestVo.getName());
            user.setRoleId(editUserRequestVo.getRole());
            user.setNickname(editUserRequestVo.getNickname());

            int result = usersMapper.updateById(user);
            if (result != 0) {
                return ResponseResult.okResult();
            }

            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL);
        }

        return ResponseResult.errorResult(ErrorCode.SYSTEM_EXCEPTION);
    }


    //    根据id删除用户
    @Override
    public ResponseResult delUserById(int userId) {
//        搜索该用户的实体类
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            throw new BusinessException(ErrorCode.SEARACH_NULL);
        }
//        修改其删除状态
        users.setState(UserRoleConstant.USER_DEPT);
        usersMapper.updateById(users);

        return ResponseResult.okResult();
    }



    //        判断editUserRequestVo参数是否为空
    public boolean judgeEditUser(EditUserRequestVo editUserRequestVo) {

        if (editUserRequestVo.getName().equals("")) {
            throw new BusinessException(ErrorCode.DATA_NULL, "phone参数为空");
        }
        if (editUserRequestVo.getRole().equals("")) {
            throw new BusinessException(ErrorCode.DATA_NULL, "role参数为空");
        }
        if (editUserRequestVo.getPhone1().equals("")) {
            throw new BusinessException(ErrorCode.DATA_NULL, "phone1参数为空");
        }
        if (editUserRequestVo.getPhone2() == null || editUserRequestVo.getPhone2().equals("")) {
            throw new BusinessException(ErrorCode.DATA_NULL, "phone2参数为空");
        }
        if (editUserRequestVo.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.JSON_ERROR, "userId参数错误");
        }
        if (editUserRequestVo.getNickname().equals("")){
            throw new BusinessException(ErrorCode.DATA_NULL, "nickName参数为空");
        }

        return true;
    }

    //  根据id重置密码
    @Override
    public ResponseResult resetPwdById(@RequestParam Integer userId) {

//        根据id获取对应用户实体类
        Users user = usersMapper.selectById(userId);
//        如果未找到则抛出异常
        if (user == null|| user.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            throw new BusinessException(ErrorCode.SEARACH_NULL);
        }

        try {
            user.setPassword(MyMD5Util.getEncryptedPwd("123456"));
        } catch (NoSuchAlgorithmException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        } catch (UnsupportedEncodingException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        }
//        向数据库更新数据
        usersMapper.updateById(user);

        return ResponseResult.okResult();
    }


    /**
     * @Description: 更新用户密码
     * @Date: 2024/1/3
     * @Param updatePWDVo:
     **/
    @Override
    public ResponseResult updatePWD(UpdatePWDVo updatePWDVo) {

//        获取数据
        Integer userId = updatePWDVo.getUserId();
        String oldPwd = updatePWDVo.getOldPwd();
        String newPwd = updatePWDVo.getNewPwd();

        Users user = usersMapper.selectById(userId);
        if (user == null|| user.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            throw new BusinessException(ErrorCode.SEARACH_NULL);
        }

        try {
//            如果旧密码不正确
            if (!MyMD5Util.validPassword(oldPwd, user.getPassword())) {
                throw new BusinessException(ErrorCode.JSON_ERROR, "旧密码错误");
            }
//             无误则设置新密码
            user.setPassword(MyMD5Util.getEncryptedPwd(newPwd));
        } catch (NoSuchAlgorithmException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        } catch (UnsupportedEncodingException e) {
            return ResponseResult.errorResult(ErrorCode.PAEEWORD_CONVERSION_ERROR);
        }

        usersMapper.updateById(user);

        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult getUserListByDeptId(Long deptId) {
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getDeptId, deptId).eq(Users::getState,UserStateConstant.USER_NORMAL);
        List<Users> users = usersMapper.selectList(usersWrapper);
        return ResponseResult.okResult(users);
    }


//    检查用户权限
    public ResponseResult checkUserRole(String roleId,Integer deptId){

        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();


        LambdaQueryWrapper<Dept> deptLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deptLambdaQueryWrapper.eq(Dept::getDeptName, "督查室").eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
        Dept deptDuCha = deptMapper.selectOne(deptLambdaQueryWrapper);



        if(roleId.equals(UserRoleConstant.SYSTEM_MANAGER)){
            usersWrapper.clear();
            usersWrapper.eq(Users::getRoleId,UserRoleConstant.SYSTEM_MANAGER)
                    .eq(Users::getState,UserStateConstant.USER_NORMAL);

            List<Users> users = usersMapper.selectList(usersWrapper);
            if(users.size() == 0){
                return ResponseResult.okResult();
            }else {
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"管理员名额已满");
            }
        }

        if(roleId.equals(UserRoleConstant.SUPERINTENDENT)){
            usersWrapper.clear();
            usersWrapper.eq(Users::getRoleId,UserRoleConstant.SUPERINTENDENT)
                    .eq(Users::getState,UserStateConstant.USER_NORMAL);

            List<Users> users = usersMapper.selectList(usersWrapper);

//            如果不是添加部门督查部门,抛出异常
            if(deptDuCha != null && deptId != deptDuCha.getId()){
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"该单位不是督查部门");
            }

            if(users.size() == 0){
                return ResponseResult.okResult();
            }else {
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"督查室单位领导名额已满");
            }

        }


        if(roleId.equals(UserRoleConstant.SUPERVISOR)){
            //            如果不是添加部门督查部门,抛出异常
            if(deptDuCha != null && deptId != deptDuCha.getId()){
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"该单位不是督查部门");
            }
        }


        if(roleId.equals(UserRoleConstant.OFFICE_DIRECTOR)){
            usersWrapper.clear();
            usersWrapper.eq(Users::getRoleId,UserRoleConstant.OFFICE_DIRECTOR)
                    .eq(Users::getDeptId,deptId)
                    .eq(Users::getState,UserStateConstant.USER_NORMAL);

            List<Users> users = usersMapper.selectList(usersWrapper);
            if(users.size() == 0){
                return ResponseResult.okResult();
            }else {
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"办事单位领导名额已满");
            }
        }

        if(roleId.equals(UserRoleConstant.OFFICE_STAFF)){
            usersWrapper.clear();
            usersWrapper.eq(Users::getRoleId,UserRoleConstant.OFFICE_STAFF)
                    .eq(Users::getDeptId,deptId)
                    .eq(Users::getState,UserStateConstant.USER_NORMAL);

            List<Users> users = usersMapper.selectList(usersWrapper);
            if(users.size() == 0){
                return ResponseResult.okResult();
            }else {
                return ResponseResult.errorResult(ErrorCode.ROLE_NO_ALLOW,"办事单位执行人名额已满");
            }
        }

        return ResponseResult.okResult();
    }
}
