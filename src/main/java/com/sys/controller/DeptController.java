package com.sys.controller;
/*
        张睿相   Java
*/

import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.AddDeptRequestVo;
import com.sys.entity.RequestVo.DepIdRequestVo;
import com.sys.entity.RequestVo.EditDeptRequestVo;
import com.sys.excption.BusinessException;
import com.sys.service.impl.DeptServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * @Description: 获取部门列表
     * @Date: 2024/1/2

     **/
    @GetMapping("/getDeptList")
    public ResponseResult getDeptList(){
        ResponseResult result = deptService.getDeptList();
        return result;
    }


    /**
     * @Description:  添加部门
     * @Date: 2024/1/2
     * @Param addDeptRequestVo:
     **/
    @PostMapping("/addDept")
    public ResponseResult addDept(@RequestParam String deptName){

        if(deptName.equals("")){
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR);
        }

        return deptService.addDept(deptName);
    }


    /**
     * @Description:  编辑部门
     * @Date: 2024/1/2
     * @Param editDeptRequestVo:
     **/
    @PutMapping("/editDept")
    public ResponseResult editDept(@RequestParam Integer deptId,
                                   @RequestParam String newDeptName){

        if(deptId <= 0 || newDeptName.equals("")){
            throw new BusinessException(ErrorCode.DATA_NULL);
        }

        ResponseResult result = deptService.editDept(deptId,newDeptName);
        return result;
    }

    /**
     * @Description:  删除部门
     * @Date: 2024/1/2
     * @Param depIdRequestVo:
     **/
    @DeleteMapping("/delDept")
    public ResponseResult delDept(@RequestParam Integer deptId){

        if(deptId <= 0){
            throw new BusinessException(ErrorCode.JSON_ERROR);
        }

        return deptService.delDept(deptId);
    }

}


