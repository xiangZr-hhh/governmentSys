package com.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.Task;
import com.sys.excption.BusinessException;
import com.sys.mapper.TaskMapper;
import com.sys.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * (Task)表服务实现类
 *
 * @author zrx
 * @since 2023-12-21 20:13:34
 */
@Service("taskService")
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public ResponseResult submitNewTask(SubmitTaskRequestVo submitTaskRequestVo) {

        Task task = new Task();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        // 设置创建时间
        task.setCreateTime(new Date());
        // 设置前端参数

        if (submitTaskRequestVo.getFromVo().getTaskNo() != null) {
            String taskNo = submitTaskRequestVo.getFromVo().getTaskNo();
            task.setTaskNo(taskNo);
        }


        if (submitTaskRequestVo.getFromVo().getTaskType() != null) {
            String taskType = submitTaskRequestVo.getFromVo().getTaskType();
            task.setTaskType(taskType);
        }

        String isVip = submitTaskRequestVo.getFromVo().getIsVip();
        if (isVip != null) {
            task.setIsVip(isVip.toString());
        }

        String taskName = submitTaskRequestVo.getFromVo().getTaskName();
        if (taskName != null) {
            task.setTaskName(taskName);
        }

        String taskDetail = submitTaskRequestVo.getFromVo().getTaskDetail();
        if (taskDetail != null) {
            task.setTaskDetail(taskDetail);
        }

        Integer mTaskerid = submitTaskRequestVo.getFromVo().getMTaskerid();
        if (mTaskerid != null) {
            task.setMtasker(mTaskerid.toString());
        }

        Integer[] sTaskerid = submitTaskRequestVo.getFromVo().getSTaskerid();
        if (sTaskerid != null && sTaskerid.length > 0) {
            task.setStasker(String.join(",", sTaskerid.toString()));
        }

        String startTime = submitTaskRequestVo.getFromVo().getStartTime();
        if (startTime != null) {
            try {
                task.setStartTime(formatter.parse(startTime));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        String endTime = submitTaskRequestVo.getFromVo().getEndTime();
        if (startTime != null) {
            try {
                task.setEndTime(formatter.parse(endTime));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        String urgency = submitTaskRequestVo.getFromVo().getUrgency();
        if (urgency != null) {
            task.setUrgency(urgency);
        }

        String notes = submitTaskRequestVo.getFromVo().getNotes();
        if (notes != null) {
            task.setNotes(notes);
        }

        // 使用MyBatis Plus的insert方法添加实体类到数据库
        taskMapper.insert(task);


        return ResponseResult.okResult();
    }
}
