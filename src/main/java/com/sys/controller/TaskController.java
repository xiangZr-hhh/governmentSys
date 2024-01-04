package com.sys.controller;


import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.*;
import com.sys.excption.BusinessException;
import com.sys.service.impl.TaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = taskService.submitNewTask(submitTaskRequestVo);

        return result;
    }

//    4.2 督查跟踪：查看办理中（状态：1，3，4，5，6，7，8，12，13,14,15）的事项清单
//    可以点开某一条查看详情和办理过程中的流程信息
    @GetMapping("/getAllTasks/userId")
    public ResponseResult getAllTaskByUserId(@RequestBody UserIdRequest userIdRequest){

        Integer userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getAllTaskByUserId(userId);

        return result;
    }

//    4.2 根据事项id获取事项内容与流程
    @GetMapping("/getTaskContent/taskId")
    public ResponseResult getTaskById(@RequestBody TaskIdVo taskIdVo){

        Integer taskId = taskIdVo.getTaskId();
        if(taskId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getTaskById(taskId);

        return result;

    }

//    4.1 文件上传接口
    @PostMapping("/upload")
    public ResponseResult uploadFile(@RequestPart MultipartFile file){

        if (file.isEmpty()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = taskService.uploadFile(file);

        return result;

    }

//    4.3 获取已驳回事项（督办人员、督办主任）
    @GetMapping("/getRejectedTasks/userId")
    public ResponseResult getRejectTasksById(@RequestBody UserIdRequest userIdRequest){

        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getRejctTasksById(userId);

        return result;

    }

//    4.4 查看已办结（状态：10）的事项清单
    @GetMapping("/getCompletedTasks")
    public ResponseResult getCompletedTask(){

        ResponseResult result = taskService.getCompletedTask();

        return result;
    }

//    4.4 查看已归档（状态：11）的事项清单
    @GetMapping("/getArchivedTasks")
    public ResponseResult getArchivedTasks(){

        ResponseResult result = taskService.getArchivedTask();

        return result;
    }


    /**
     * @Description: TODO 督办人员提交的待审事项
     * @Date: 2024/1/3

     **/
    @GetMapping("/getReviewTasksAfterSubmit")
    public ResponseResult getReviewAfterSubmit(){

        ResponseResult result = taskService.getReviewAfterSubmit();

        return result;

    }


    /**
     * @Description: TODO 主/协办单位提交的待审事项
     * @Date: 2024/1/3

     **/
    @GetMapping("/getReviewTasksAfterFeedback")
    public ResponseResult getReviewTaskAfterFeedback(){

        ResponseResult result = taskService.getReviewTaskAfterFeedback();

        return result;

    }


    /**
     * @Description: TODO 获取事项办理结果（督办主任）
     * @Date: 2024/1/3
     * @Param taskIdVo:
     **/
    @GetMapping("/getTaskResult/taskId")
    public ResponseResult getTaskResult(@RequestBody TaskIdVo taskIdVo){

        Integer taskId = taskIdVo.getTaskId();
        if(taskId <= 0){
            throw  new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getTaskResult(taskId);

        return result;
    }


    /**
     * @Description: TODO 审批事项
     * @Date: 2024/1/3
     * @Param approverTaskRequestVo:
     **/
    @PostMapping("/approveTask/taskId")
    public ResponseResult approveTask(@RequestBody ApproverTaskRequestVo approverTaskRequestVo){

        if(approverTaskRequestVo== null){
            throw  new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = taskService.approveTask(approverTaskRequestVo);

        return result;
    }


    /**
     * @Description: TODO 终止任务
     * @Date: 2024/1/3
     * @Param taskIdVo: 
     **/
    @PostMapping("/suspentTask/taskId")
    public ResponseResult suspentTask(@RequestBody TaskIdVo taskIdVo){
        
        int taskId = taskIdVo.getTaskId();
        
        if(taskId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }
        
        ResponseResult result = taskService.suspentTask(taskId);
        
        return result;
    }


    /**
     * @Description: TODO 获取所有部门的得分，只包含业务部门
     * @Date: 2024/1/3
     **/
    @GetMapping("/getScoreList")
    public ResponseResult getScoreList(){

        ResponseResult result = taskService.getScoreList();
        return result;
    }



    /**
     * @Description: TODO 获取单个部门的得分
     * @Date: 2024/1/3
     * @Param depIdRequestVo:
     **/
    @GetMapping("/getScoreDetail/deptId")
    public ResponseResult getScoreDetailById(@RequestBody DepIdRequestVo depIdRequestVo){

        Integer deptId = depIdRequestVo.getDeptId();

        if(deptId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getScoreDetailById(deptId);

        return result;
    }


    /**
     * @Description: TODO 查看主办单位待执行或被驳回的事项
     * @Date: 2024/1/4
     * @Param userIdRequest: 
     **/
    @GetMapping("/getUnclaimedTasks/userId")
    public ResponseResult getUnclaimedTasks(@RequestBody UserIdRequest userIdRequest){

//        获取userId
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw  new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

        ResponseResult result = taskService.getUnclaimedTasks(userId);
        return result;
    }


    /**
     * @Description: TODO 主办单位领取事项
     * @Date: 2024/1/4
     * @Param taskIdAndUserIdRequestVo:
     **/
    @PostMapping("/claimTask")
    public ResponseResult claimTask(@RequestBody TaskIdAndUserIdRequestVo taskIdAndUserIdRequestVo){
        
//        获取数据 并 检验
        Integer userId = taskIdAndUserIdRequestVo.getUserId();
        Integer taskId = taskIdAndUserIdRequestVo.getTaskId();
        if(userId <= 0 || taskId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }
        
        ResponseResult result = taskService.claimTask(userId,taskId);
        return result;
    }



    /**
     * @Description: TODO 获取办事人员已提交但未审核的事项
     * @Date: 2024/1/4
     * @Param userIdRequest: 
     **/
    @GetMapping("/getUnapprovedTasks/userId")
    public ResponseResult getUnapprovedTasks(@RequestBody UserIdRequest userIdRequest){

//        获取userId并检验
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getUnapprovedTasks(userId);
        return  result;
    }



    /**
     * @Description: TODO 获取本部门的办事人员列表
     * @Date: 2024/1/4
     * @Param userIdRequest: 
     **/
    @GetMapping("/getStaffList/userId")
    public ResponseResult getStaffList(@RequestBody UserIdRequest userIdRequest){

//        获取userId并检验
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getStaffList(userId);
        return  result;
        
    }



    /**
     * @Description: TODO 获取已经领办待未分配办事人员的事项
     * @Date: 2024/1/4
     * @Param userIdRequest:
     **/
    @GetMapping("/getClaimedTasks/userId")
    public ResponseResult getClaimedTasks(@RequestBody UserIdRequest userIdRequest){
//        获取userId并检验
        int userId = userIdRequest.getUserId();
        if(userId <= 0){
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

        ResponseResult result = taskService.getClaimedTasks(userId);
        return  result;
    }

}
