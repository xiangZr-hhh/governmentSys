package com.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.ApproverTaskRequestVo;
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

    ResponseResult getAllTaskByUserId(Integer userId);

    ResponseResult getTaskById(Integer taskId);

    ResponseResult uploadFile(MultipartFile file);

    ResponseResult getRejctTasksById(int userId);

    //  获取以及完成度事项id
    ResponseResult getCompletedTask();

    ResponseResult getArchivedTask();

    ResponseResult getReviewAfterSubmit();

    ResponseResult getReviewTaskAfterFeedback();

    ResponseResult getTaskResult(Integer taskId);

    ResponseResult approveTask(ApproverTaskRequestVo approverTaskRequestVo);

    ResponseResult suspentTask(int taskId);

    ResponseResult getScoreList();

    ResponseResult getScoreDetailById(Integer deptId);

    ResponseResult getUnclaimedTasks(int userId);

    ResponseResult claimTask(Integer userId, Integer taskId);

    ResponseResult getUnapprovedTasks(int userId);

    ResponseResult getStaffList(int userId);

    ResponseResult getClaimedTasks(int userId);
}

