package com.sys.controller;
/*
        张睿相   Java
*/

import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.AddDeptRequestVo;
import com.sys.entity.RequestVo.DepIdRequestVo;
import com.sys.entity.RequestVo.EditDeptRequestVo;
import com.sys.excption.BusinessException;
import com.sys.service.impl.DeptServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 总体描述:
 * <p>创建时间：2024/1/1 16:05</p>
 *
 * @author zhaoXin
 * @since v1.0
 */
@RestController
public class DeptController {

    @Autowired
    private DeptServiceImpl deptService;

    /**
     * @Description: TODO 获取部门列表
     * @Date: 2024/1/2

     **/
    @GetMapping("/getDeptList")
    public ResponseResult getDeptList(){
        ResponseResult result = deptService.getDeptList();
        return result;
    }

    /**
     * @Description: TODO 添加部门
     * @Date: 2024/1/2
     * @Param addDeptRequestVo:
     **/
    @PostMapping("/addDept")
    public ResponseResult addDept(@RequestBody AddDeptRequestVo addDeptRequestVo){
        String deptName = addDeptRequestVo.getDeptName();

        if(deptName.equals("")){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = deptService.addDept(deptName);
        return result;
    }


    /**
     * @Description: TODO 编辑部门
     * @Date: 2024/1/2
     * @Param editDeptRequestVo:
     **/
    @PostMapping("/editDept")
    public ResponseResult editDept(@RequestBody EditDeptRequestVo editDeptRequestVo){

        if(editDeptRequestVo == null){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = deptService.editDept(editDeptRequestVo);
        return result;
    }

    /**
     * @Description: TODO 删除部门
     * @Date: 2024/1/2
     * @Param depIdRequestVo:
     **/
    @PostMapping("/delDept")
    public ResponseResult delDept(@RequestBody DepIdRequestVo depIdRequestVo){
        Integer deptId = depIdRequestVo.getDeptId();

        if(deptId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = deptService.delDept(deptId);
        return result;
    }

}


