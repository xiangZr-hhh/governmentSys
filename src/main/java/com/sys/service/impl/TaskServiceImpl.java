package com.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.constant.TaskActionConstant;
import com.sys.constant.TaskConstant;
import com.sys.constant.UserRoleConstant;
import com.sys.entity.Flow;
import com.sys.entity.FlowAsist;
import com.sys.entity.RequestVo.AttachmentVo;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.Task;
import com.sys.entity.Users;
import com.sys.excption.BusinessException;
import com.sys.mapper.FlowAsistMapper;
import com.sys.mapper.FlowMapper;
import com.sys.mapper.TaskMapper;
import com.sys.mapper.UsersMapper;
import com.sys.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

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
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private FlowMapper flowMapper;
    @Autowired
    private FlowAsistMapper flowAsistMapper;


//    4.1任务人员督办立项
    @Override
    public ResponseResult submitNewTask(SubmitTaskRequestVo submitTaskRequestVo) {

        Task task = new Task();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 设置创建时间
        task.setCreateTime(new Date());

        // 设置前端参数

//        创建者id
        if(submitTaskRequestVo.getCreatorId() != null){
            task.setCreateBy(submitTaskRequestVo.getCreatorId());
        }else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        任务编号
        if (!submitTaskRequestVo.getForm().getTaskNo().isEmpty()) {
            String taskNo = submitTaskRequestVo.getForm().getTaskNo();
            task.setTaskNo(taskNo);
        }

//        任务类型
        if (!submitTaskRequestVo.getForm().getTaskType().isEmpty()) {
            String taskType = submitTaskRequestVo.getForm().getTaskType();
            task.setTaskType(taskType);
        }

//        是否重大
        String isVip = submitTaskRequestVo.getForm().getIsVip();
        if (isVip != null) {
            task.setIsVip(isVip.toString());
        }

//        任务名称
        String taskName = submitTaskRequestVo.getForm().getTaskName();
        if (taskName != null&&!taskName.equals("")) {
            task.setTaskName(taskName);
        }else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL,"taskName为空");
        }

//        任务描述
        String taskDetail = submitTaskRequestVo.getForm().getTaskDetail();
        if (taskDetail != null) {
            task.setTaskDetail(taskDetail);
        }

//        主办单位id
        Integer mTaskerid = submitTaskRequestVo.getForm().getMTaskerid();
        if (mTaskerid != null) {
            task.setMtasker(mTaskerid.toString());
        }else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        协办单位id
        Integer[] sTaskerids = submitTaskRequestVo.getForm().getSTaskerid();
        if (sTaskerids != null && sTaskerids.length > 0) {
            String innerSTaskeridsString = "";
            for(int i = 0;i < sTaskerids.length;i ++){
//                如果不是最后一次，则添加“，”
                if(i != sTaskerids.length-1){
                    innerSTaskeridsString += sTaskerids[i].toString()+",";
                }
//                如果是最后一次，不添加“，”即可
                if(i == sTaskerids.length-1){
                    innerSTaskeridsString += sTaskerids[i].toString();
                }
            }
            task.setStasker(innerSTaskeridsString);
        }

//        开始时间
        String startTime = submitTaskRequestVo.getForm().getStartTime();
        if (startTime != null&&!startTime.equals("")) {
            try {
                task.setStartTime(formatter.parse(startTime));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

//        截止时间
        String endTime = submitTaskRequestVo.getForm().getEndTime();
        if (endTime != null&&!endTime.equals("")) {
            try {
                task.setEndTime(formatter.parse(endTime));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

//        紧急程度
        String urgency = submitTaskRequestVo.getForm().getUrgency();
        if (urgency != null) {
            task.setUrgency(urgency);
        }

//        备注
        String notes = submitTaskRequestVo.getForm().getNotes();
        if (notes != null) {
            task.setNotes(notes);
        }

        List<AttachmentVo> attachmentVos = submitTaskRequestVo.getForm().getAttachment();
        if(attachmentVos.size() != 0){
            task.setAttachment(JSON.toJSONString(attachmentVos));
        }

//        设置任务状态为1，督办人员已提交
        task.setState(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);


        //添加实体类到数据库
        taskMapper.insert(task);

//        先获取对应督办主任的id
        Users users = usersMapper.selectById(task.getCreateBy());
        LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(Users::getDeptId,users.getDeptId())
                .eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT);
        Users superUser = usersMapper.selectOne(userWrapper);
//        主办单位流程表---添加记录
        Flow flow = new Flow(task.getId(),TaskActionConstant.SUPERVISING_PERSONNEL_SUBMITTED,task.getCreateBy(),
                task.getEndTime(),TaskConstant.TASK_NO_CREATE,TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED,
                superUser.getId(),task.getNotes(),0);
        flowMapper.insert(flow);
//      协办单位流程表---添加记录
        for(int i = 0;i <sTaskerids.length;i ++) {
            FlowAsist flowAsist = new FlowAsist(task.getId(),sTaskerids[i],TaskActionConstant.SUPERVISING_PERSONNEL_SUBMITTED,
                    task.getCreateBy(),task.getEndTime(),task.getNotes());
            flowAsistMapper.insert(flowAsist);
        }

        return ResponseResult.okResult();
    }
}
