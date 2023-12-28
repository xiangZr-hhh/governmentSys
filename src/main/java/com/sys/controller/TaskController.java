package com.sys.controller;

import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.excption.BusinessException;
import com.sys.service.TaskService;
import com.sys.service.impl.TaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
        张睿相   Java
*/
@RestController
public class TaskController {

    @Autowired
    private TaskServiceImpl taskService;


//    提交新任务
    @PostMapping("/submitNewTask")
    public ResponseResult submitNewTask(@RequestBody SubmitTaskRequestVo submitTaskRequestVo){

        if(submitTaskRequestVo == null){
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = taskService.submitNewTask(submitTaskRequestVo);

        return result;
    }
}
