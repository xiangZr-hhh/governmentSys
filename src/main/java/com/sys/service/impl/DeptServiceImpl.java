package com.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.constant.UserRoleConstant;
import com.sys.entity.Dept;
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
       List<Dept> depts = deptMapper.selectList(null);
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
//        如果找到，修改其为删除状态
        dept.setState(UserRoleConstant.DELETE_DEPT);
        deptMapper.updateById(dept);
        return ResponseResult.okResult();
    }
}
