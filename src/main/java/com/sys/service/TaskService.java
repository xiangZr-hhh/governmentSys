package com.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.ApproveFeedbackVo;
import com.sys.entity.RequestVo.SubmitRejectTaskVo;
import com.sys.entity.RequestVo.SubmitResultVO;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.Task;
import org.springframework.web.multipart.MultipartFile;


/**
 * (Task)表服务接口
 *
 * @author zrx
 * @since 2023-12-21 20:13:33
 */
public interface TaskService extends IService<Task> {

    ResponseResult submitNewTask(SubmitTaskRequestVo submitTaskRequestVo);

    ResponseResult getAllTask();

    ResponseResult getTaskById(Integer taskId);

    ResponseResult uploadFile(MultipartFile file);

    ResponseResult getKeytipTasks();

    ResponseResult getTrackedTasks();
//
//    ResponseResult getRejctTasksById(int userId);
//
//    //  获取以及完成度事项id
//    ResponseResult getCompletedTask();
//
    ResponseResult getArchivedTask();
//
//    ResponseResult getReviewAfterSubmit();
//
//    ResponseResult getReviewTaskAfterFeedback();
//
//    ResponseResult getTaskResult(Integer taskId);
//
//    ResponseResult approveTask(ApproverTaskRequestVo approverTaskRequestVo);
//
//    ResponseResult suspentTask(int taskId);
//
    ResponseResult getScoreList(String beginDate, String endDate);

    ResponseResult getScoreDetail(String beginDate, String endDate, Integer deptId);

    ResponseResult getTaskResultById(Integer taskId);

    ResponseResult approveTaskById(Integer taskId);

    ResponseResult approveResultById(Long taskId);

    ResponseResult submitRejectTask(SubmitRejectTaskVo submitRejectTaskVo);

    ResponseResult suspentTaskById(Integer taskId);

    ResponseResult getAllTask(Long userId);

    ResponseResult getUnapprovedTasksByUserId(Integer userId);


    ResponseResult approveFeedback(ApproveFeedbackVo approveFeedbackVo);

    ResponseResult getApprovedTasksByUserId(Integer userId);

    ResponseResult getDeductedTasks(Integer userId);

    ResponseResult getUnclaimedTasks(Integer userId);

    ResponseResult claimTask(Integer taskId, Integer userId);

    ResponseResult getUnfeedbackTasks(Integer userId);

    ResponseResult getSubmittedTasksByUserId(Integer userId);

    ResponseResult getOverdueTasksByUserId(Integer userId);

    ResponseResult getRejectedTasksByUserId(Integer userId);

    ResponseResult getTaskFlowByUserId(Integer userId, Integer taskId);

    ResponseResult getExportScore(String beginDate, String endDate);

    ResponseResult exportTask(String status);

    ResponseResult getDeptStatus(Integer taskId);

    ResponseResult getAllDeptTasks(Integer userId);

    ResponseResult deleteTask(Integer taskId);

    ResponseResult DeptTaskResult(Integer userId, Integer taskId);

    ResponseResult submitResult(SubmitResultVO submitResultVO);
}

