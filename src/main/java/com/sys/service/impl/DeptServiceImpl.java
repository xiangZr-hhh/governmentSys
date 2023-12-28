package com.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.constant.UserRoleConstant;
import com.sys.entity.Dept;
import com.sys.entity.RequestVo.EditDeptRequestVo;
import com.sys.entity.ResponseVo.DeptVo;
import com.sys.excption.BusinessException;
import com.sys.mapper.DeptMapper;
import com.sys.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * (Dept)表服务实现类
 *
 * @author zrx
 * @since 2023-12-21 20:13:15
 */
@Service("deptService")
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public ResponseResult getDeptList() {
//        获取状态正常的部门
        LambdaQueryWrapper<Dept> deptWrapper = new LambdaQueryWrapper<>();
        deptWrapper.eq(Dept::getState,UserRoleConstant.DEPT_NORMAL);
       List<Dept> depts = deptMapper.selectList(deptWrapper);
//       封装好Vo类
       List<DeptVo> data = new ArrayList<>();
       for(Dept dept:depts){
           DeptVo deptVo = new DeptVo(dept.getId(),dept.getDeptName());
           data.add(deptVo);
       }
       return ResponseResult.okResult(data);
    }


//    添加部门业务层
    @Override
    public ResponseResult addDept(String deptName) {
        Dept dept =new Dept();
        dept.setDeptName(deptName);
//        添加部门
        int check = deptMapper.insert(dept);
//        检测添加结果是否正确
        if(check != 0 ){
            return ResponseResult.okResult();
        }
        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_EXCEPTION);
    }


//    删除部门
    @Override
    public ResponseResult delDept(Integer deptId) {
//        搜索该部门实体类
        Dept dept = deptMapper.selectById(deptId);
//        如果未找到，抛异常
        if(dept == null){
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        如果部门已经被删除
        if(dept.getState().equals(UserRoleConstant.DELETE_DEPT)){
            return ResponseResult.okResult("该部门已经删除");
        }

//        如果找到，修改其为删除状态
        dept.setState(UserRoleConstant.DELETE_DEPT);
        deptMapper.updateById(dept);
        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult editDept(EditDeptRequestVo editDeptRequestVo) {

//        检验参数是否为空
        int deptId = editDeptRequestVo.getDeptId();
        if(deptId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }
        String newDeptName = editDeptRequestVo.getNewDeptName();
        if(newDeptName.equals("") || newDeptName == null){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        获取对应的dept实体类
        Dept dept = deptMapper.selectById(deptId);
//        搜索不到抛出异常
        if(dept == null){
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

        if(newDeptName.equals(dept.getDeptName())){
            return ResponseResult.okResult("新旧部门名称重复，无需更改");
        }

        dept.setDeptName(newDeptName);
        deptMapper.updateById(dept);

        return ResponseResult.okResult();
    }



}
