package com.sys.controller;

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


@RestController
public class DeptController {

    @Autowired
    private DeptServiceImpl  deptService;

    @GetMapping("/getDeptList")
    public ResponseResult getDeptList(){
        ResponseResult result = deptService.getDeptList();
        return result;
    }

    @PostMapping("/addDept")
    public ResponseResult addDept(@RequestBody AddDeptRequestVo addDeptRequestVo){
        String deptName = addDeptRequestVo.getDeptName();

        if(deptName.equals("")){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = deptService.addDept(deptName);
        return result;
    }


    @PostMapping("/editDept")
    public ResponseResult editDept(@RequestBody EditDeptRequestVo editDeptRequestVo){

        if(editDeptRequestVo == null){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = deptService.editDept(editDeptRequestVo);
        return result;
    }

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
