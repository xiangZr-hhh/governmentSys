package com.sys.controller;


import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.*;
import com.sys.excption.BusinessException;
import com.sys.service.impl.TaskServiceImpl;
import com.sys.utils.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/*
        张睿相   Java
*/
@RestController
public class TaskController {

    @Autowired
    private TaskServiceImpl taskService;

//    提交新事项
    @PostMapping("/submitNewTask")
    public ResponseResult submitNewTask(@RequestBody SubmitTaskRequestVo submitTaskRequestVo){

        if(submitTaskRequestVo == null){
            throw new BusinessException(ErrorCode.DATA_NULL);
        }

        ResponseResult result = taskService.submitNewTask(submitTaskRequestVo);

        return result;
    }

//    4.2 督查跟踪：查看办理中（状态：1-11）的事项清单
//    可以点开某一条查看详情和办理过程中的流程信息
    @GetMapping("/getAllTasks")
    public ResponseResult getAllTaskByUserId(@RequestParam(value = "userId", required = false) Long userId){

//        如果userId为空，查询全部
        if(userId == null){
            return taskService.getAllTask();
        }

//        如果userId不为空，查询对应部门的信息
        if(userId != null){
            return taskService.getAllTask(userId);
        }

        return ResponseResult.okResult();
    }

//    4.2 根据事项id获取事项内容与流程
    @GetMapping("/getTaskContent/taskId")
    public ResponseResult getTaskById(@RequestParam Integer taskId){

        if(taskId <= 0){
            throw new BusinessException(ErrorCode.JSON_ERROR);
        }

        ResponseResult result = taskService.getTaskById(taskId);

        return result;

    }

//    4.1 文件上传接口
    @PostMapping("/upload")
    public ResponseResult uploadFile(@RequestPart MultipartFile file){

        if (file.isEmpty()) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }

        ResponseResult result = taskService.uploadFile(file);

        return result;

    }


//3.重点提示事项：查看领办逾期（状态3）、反馈逾期（状态7）、反馈驳回（状态9）的事项清单，可以点开某一条查看详情和办理过程中的流程信息
    @GetMapping("/getKeytipTasks")
    public ResponseResult getKeytipTasks(){
        return taskService.getKeytipTasks();
    }

//4.跟踪督查事项：查看待交办（状态1）、待领办（状态2）、推进中（状态5-6）、已反馈（状态8）的事项清单，点击查看可以点开某一条查看详情和办理过程中的流程信息
    @GetMapping("/getTrackedTasks")
    public ResponseResult getTrackedTasks(){
        return taskService.getTrackedTasks();
    }


    @GetMapping("/getArchivedTasks")
    public ResponseResult getArchivedTasks(){

        ResponseResult result = taskService.getArchivedTask();

        return result;
    }

    /**
     * @Description:  获取所有部门的得分，只包含业务部门
     * @Date: 2024/1/3
     **/
    @GetMapping("/getScoreList")
    public ResponseResult getScoreList(@RequestParam String beginDate,@RequestParam String endDate){

        ResponseResult result = taskService.getScoreList(beginDate,endDate);
        return result;
    }

    @GetMapping("/getScoreDetail")
    public ResponseResult getScoreDetail(@RequestParam String beginDate,
                                         @RequestParam String endDate,@RequestParam Integer deptId){
        return taskService.getScoreDetail(beginDate,endDate,deptId);
    }

    @GetMapping("/getTaskResult/taskId")
    public ResponseResult getTaskResultById(@RequestParam Long taskId){

        if(taskId == null){
            return ResponseResult.errorResult(ErrorCode.JSON_ERROR);
        }

        return taskService.getTaskResultById(taskId);
    }

    @PutMapping("/approveTask/taskId")
    public ResponseResult approveTaskById(){

        return taskService.approveTaskById();
    }


    @PutMapping("/approveResult/taskId")
    public ResponseResult approveResultById(@RequestParam Long taskId){
        if(taskId == null){
            return ResponseResult.errorResult(ErrorCode.JSON_ERROR);
        }

        return taskService.approveResultById(taskId);
    }


    @PutMapping("/submitRejectTask")
    public ResponseResult submitRejectTask(@RequestBody  @Validated SubmitRejectTaskVo submitRejectTaskVo, BindingResult bindingResult){

        // 判断是否有参数错误
        if (bindingResult.hasErrors()) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, TaskUtils.getValidatedErrorList(bindingResult));
        }

        return taskService.submitRejectTask(submitRejectTaskVo);
    }


    @PutMapping("/suspentTask/taskId")
    public ResponseResult suspentTaskById(@RequestParam Long taskId){
        if(taskId == null){
            return ResponseResult.errorResult(ErrorCode.JSON_ERROR);
        }

        return taskService.suspentTaskById(taskId);
    }



}
