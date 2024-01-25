package com.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.common.ResponseResult;
import com.sys.entity.Dept;
import com.sys.entity.RequestVo.EditDeptRequestVo;


/**
 * (Dept)表服务接口
 *
 * @author zrx
 * @since 2023-12-21 20:13:15
 */
public interface DeptService extends IService<Dept> {

    ResponseResult getDeptList();

    ResponseResult addDept(String deptName);

    //    删除部门
    ResponseResult delDept(Integer deptId);

    ResponseResult editDept(Integer deptId, String newDeptName);
}

