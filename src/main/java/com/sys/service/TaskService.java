package com.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.SubmitRejectTaskVo;
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

    ResponseResult getTaskResultById(Long taskId);

    ResponseResult approveTaskById();

    ResponseResult approveResultById(Long taskId);

    ResponseResult submitRejectTask(SubmitRejectTaskVo submitRejectTaskVo);

    ResponseResult suspentTaskById(Long taskId);

    ResponseResult getAllTask(Long userId);
//
//    ResponseResult getScoreDetailById(Integer deptId);
//
//    ResponseResult getUnclaimedTasks(int userId);
//
//    ResponseResult claimTask(Integer userId, Integer taskId);
//
//    ResponseResult getUnapprovedTasks(int userId);
//
//    ResponseResult getStaffList(int userId);
//
//    ResponseResult getClaimedTasks(int userId);
//
//    ResponseResult getAssignedTasks(int userId);
//
//    ResponseResult getSubmittedTasks(int userId);
}

