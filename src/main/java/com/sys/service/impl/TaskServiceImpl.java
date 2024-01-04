package com.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.AppHttpCodeEnum;
import com.sys.common.ResponseResult;
import com.sys.constant.TaskActionConstant;
import com.sys.constant.TaskConstant;
import com.sys.constant.UserRoleConstant;
import com.sys.entity.*;
import com.sys.entity.RequestVo.ApproverTaskRequestVo;
import com.sys.entity.RequestVo.AttachmentVo;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.ResponseVo.*;
import com.sys.excption.BusinessException;
import com.sys.mapper.*;
import com.sys.service.TaskService;
import com.sys.utils.BeanCopyUtils;
import com.sys.utils.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    @Autowired
    private DeptMapper deptMapper;


    /**
     * @Description: TODO 4.1 督办人员提交事项
     * @Date: 2024/1/1
     * @Param submitTaskRequestVo:
     **/
    @Override
    public ResponseResult submitNewTask(SubmitTaskRequestVo submitTaskRequestVo) {

        Task task = new Task();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 设置创建时间
        task.setCreateTime(new Date());

        // 设置前端参数

//        创建者id
        if (submitTaskRequestVo.getCreatorId() != null) {
            task.setCreateBy(submitTaskRequestVo.getCreatorId());
        } else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        事项编号
        if (!submitTaskRequestVo.getForm().getTaskNo().isEmpty()) {
            String taskNo = submitTaskRequestVo.getForm().getTaskNo();
            task.setTaskNo(taskNo);
        }

//        事项类型
        if (!submitTaskRequestVo.getForm().getTaskType().isEmpty()) {
            String taskType = submitTaskRequestVo.getForm().getTaskType();
            task.setTaskType(taskType);
        }

//        是否重大
        String isVip = submitTaskRequestVo.getForm().getIsVip();
        if (isVip != null) {
            task.setIsVip(isVip.toString());
        }

//        事项名称
        String taskName = submitTaskRequestVo.getForm().getTaskName();
        if (taskName != null && !taskName.equals("")) {
            task.setTaskName(taskName);
        } else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL, "taskName为空");
        }

//        事项描述
        String taskDetail = submitTaskRequestVo.getForm().getTaskDetail();
        if (taskDetail != null) {
            task.setTaskDetail(taskDetail);
        }

//        主办单位id
        Integer mTaskerid = submitTaskRequestVo.getForm().getMTaskerid();
        if (mTaskerid != null) {
            task.setMtasker(mTaskerid.toString());
        } else {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        协办单位id
        Integer[] sTaskerids = submitTaskRequestVo.getForm().getSTaskerid();
        if (sTaskerids != null && sTaskerids.length > 0) {
            String innerSTaskeridsString = "";
            for (int i = 0; i < sTaskerids.length; i++) {
//                如果不是最后一次，则添加“，”
                if (i != sTaskerids.length - 1) {
                    innerSTaskeridsString += sTaskerids[i].toString() + ",";
                }
//                如果是最后一次，不添加“，”即可
                if (i == sTaskerids.length - 1) {
                    innerSTaskeridsString += sTaskerids[i].toString();
                }
            }
            task.setStasker(innerSTaskeridsString);
        }

//        开始时间
        String startTime = submitTaskRequestVo.getForm().getStartTime();
        if (startTime != null && !startTime.equals("")) {
            try {
                task.setStartTime(formatter.parse(startTime));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

//        截止时间
        String endTime = submitTaskRequestVo.getForm().getEndTime();
        if (endTime != null && !endTime.equals("")) {
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
        if (attachmentVos.size() != 0) {
            task.setAttachment(JSON.toJSONString(attachmentVos));
        }

//        设置事项状态为1，督办人员已提交
        task.setState(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);


        //添加实体类到数据库
        taskMapper.insert(task);

//        先获取对应督办主任的id
        Users users = usersMapper.selectById(task.getCreateBy());
        LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(Users::getDeptId, users.getDeptId())
                .eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT);
        Users superUser = usersMapper.selectOne(userWrapper);
//        主办单位流程表---添加记录
        Flow flow = new Flow(task.getId(), TaskActionConstant.SUPERVISING_PERSONNEL_SUBMITTED, task.getCreateBy(),
                task.getEndTime(), TaskConstant.TASK_NO_CREATE, TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED,
                superUser.getId(), task.getNotes(), 0);
        flowMapper.insert(flow);

        return ResponseResult.okResult();
    }


    /**
     * @param userId
     * @Description: TODO 通过用户id获取所有任务(督办人员与督办主任)
     */
    @Override
    public ResponseResult getAllTaskByUserId(Integer userId) {

//        获取对应的User实体类
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        根据权限不同返回不同数据，获取状态为1，3，4，5，6，7，8，12，13,14,15的事项
        List<String> supervisoryStaffStatusList = Arrays.asList("1", "3", "4", "5", "6", "7", "8", "12", "13", "14", "15");
        List<String> superDirectorStatusList = Arrays.asList("2", "3", "4", "5", "6", "7", "9", "10", "11", "12", "13", "14", "15");
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();

//        获取查询结果
        List<Task> taskData;
//        封装结果Vo类
        List<TaskResponseVo> taskVoData = new ArrayList<>();

//        如果为督办人员,要加上状态与userId的筛选
        if (user.getRoleId().equals(UserRoleConstant.SUPERVISOR)) {
            taskWrapper.in(Task::getState, supervisoryStaffStatusList)
                    .eq(Task::getCreateBy, user.getId());

//          获取查询结果
            taskData = taskMapper.selectList(taskWrapper);

//                封装Vo类
            for (Task task : taskData) {
                TaskResponseVo taskResponseVo = new TaskResponseVo();
                taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
                taskResponseVo.setStasker(getSTaskerName(task.getStasker()));
//              修改statue名称
                taskResponseVo.setState(TaskUtils.generateStatusStringForSupervisor(task.getState()));
                taskVoData.add(taskResponseVo);
            }

        }

//        如果为督办主任，要加上状态与主办单位的筛选
        if (user.getRoleId().equals(UserRoleConstant.SUPERINTENDENT)) {
            taskWrapper.eq(Task::getState, superDirectorStatusList)
                    .eq(Task::getMtasker, user.getDeptId());

//          获取查询结果
            taskData = taskMapper.selectList(taskWrapper);

//            封装Vo类
            for (Task task : taskData) {
                TaskResponseVo taskResponseVo = new TaskResponseVo();
//                封装Vo类
                taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
                taskResponseVo.setStasker(getSTaskerName(task.getStasker()));
//              修改statue名称
                taskResponseVo.setState(TaskUtils.generateStatusStringForDirector(task.getState()));
                taskVoData.add(taskResponseVo);
            }
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskResponseVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }


    /**
     * @Description: TODO 查看或编辑事项时获取事项内容进行展示---根据taskId获取事项内容
     * @Date: 2024/1/1
     * @Param taskId:
     **/
    @Override
    public ResponseResult getTaskById(Integer taskId) {

//        获取对应事项数据实体类
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }
//        定义Vo封装类
        TaskContentVo taskContentVo = BeanCopyUtils.copyBean(task, TaskContentVo.class);

//        定义事项id
        taskContentVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>) 与 id数组
        taskContentVo.setSTaske(getSTaskerName(task.getStasker()));
        taskContentVo.setSTaskerId(TaskUtils.convertToIntArray(task.getStasker()));

//        设置主办单位名称 与 id数组
        Dept mtaskerDept = deptMapper.selectById(Integer.parseInt(task.getMtasker()));
        taskContentVo.setMTasker(mtaskerDept.getDeptName());
        taskContentVo.setMTaskerId(Integer.parseInt(task.getMtasker()));

//        设置附件
        List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
        taskContentVo.setAttachment(attachmentVos);

//        设置流程表
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, task.getId());
        List<Flow> flows = flowMapper.selectList(flowWrapper);

//        如果事项含有对应流程
        if (flows.size() != 0) {
//        封装flow Vo类
            List<FlowVo> flowVos = BeanCopyUtils.copyBeanList(flows, FlowVo.class);
            //        按执行时间排序，由近到远
            flowVos.sort(Comparator.comparing(FlowVo::getExcuteTime).reversed());
            taskContentVo.setFlow(flowVos);

//            如果不含有
        } else if (flows.size() == 0) {
            taskContentVo.setFlow(null);
        }

//        返回结果
        return ResponseResult.okResult(taskContentVo);
    }


    /**
     * @Description: TODO 上传文件接口
     * @Date: 2024/1/1
     * @Param file:
     **/
    @Override
    public ResponseResult uploadFile(MultipartFile file) {

        //新建对象：储存文件后的返回链接,文件二进制流,返回的文件名称
        String returnUrl = "";
        String blob = "";
        String returnName = "";

        //将MultipartFile类型的文件转为blob二进制流发送出去
        try {

            byte[] fileBytes = file.getBytes();
            blob = Base64.getEncoder().encodeToString(fileBytes);

            //发送请求链接
            if (blob != "") {
                //目标接口地址
                String url = "http://124.220.42.243:8188/saveFile";
                //设置请求数据(文件名称，在云服务器的相对路径，文件二进制流)
                JSONObject postData = new JSONObject();
                postData.put("fileName", file.getOriginalFilename());
                postData.put("path", "fileUpload/data/");
                postData.put("blob", blob);
                //发送请求的对象
                RestTemplate client = new RestTemplate();
                //接收请求后的返回参数
                JSONObject json = client.postForEntity(url, postData, JSONObject.class).getBody();
                //获取对应数据(文件访问/下载链接、文件名称)
                JSONObject jsonObject = json.getJSONObject("data");
                returnUrl = jsonObject.getString("url");
                returnName = jsonObject.getString("newName");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //将获取的文件链接与名称封装为json数据
        UploadFileResponseVo fileVo = new UploadFileResponseVo();
        fileVo.setUrl(returnUrl);
        fileVo.setUid(returnName.replace(file.getOriginalFilename(), ""));

        return ResponseResult.okResult(fileVo);
    }


    /**
     * @Description: TODO 根据用户id获取已驳回事项
     * @Date: 2024/1/1
     * @Param userId:
     **/
    @Override
    public ResponseResult getRejctTasksById(int userId) {

//        先获取对应User数据
        Users user = usersMapper.selectById(userId);
//        如果未找到对应数据，抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        查询结果集合
        List<Task> tasks = new ArrayList<>();

//        如果用户为督办人员，只返回其创建的事项，且状态为驳回的事项
        if (user.getRoleId().equals(UserRoleConstant.SUPERVISOR)) {
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(Task::getCreateBy, user.getId())
                    .eq(Task::getState, TaskConstant.SUPERVISING_DIRECTOR_REJECTED);
            tasks = taskMapper.selectList(taskWrapper);
        }

//        如果用户为督办主任，返回所有部门id相同且状态为驳回的事项
        if (user.getRoleId().equals(UserRoleConstant.SUPERINTENDENT)) {
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(Task::getMtasker, user.getDeptId())
                    .eq(Task::getState, TaskConstant.SUPERVISING_DIRECTOR_REJECTED);
            tasks = taskMapper.selectList(taskWrapper);
        }

//            封装Vo类
        List<TaskResponseVo> taskVoData = turnTaskToVo(tasks);

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }


    /**
     * @Description: TODO 获取已办结的事项
     * @Date: 2024/1/1
     **/
    @Override
    public ResponseResult getCompletedTask() {

//        获取已办结的事项
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getState, TaskConstant.SUPERVISING_DIRECTOR_APPROVED_2);
        List<Task> tasks = taskMapper.selectList(taskWrapper);

//            封装Vo类
        List<ArchiveIdTaskResponseVo> archiveIdTaskVos = new ArrayList<>();
        archiveIdTaskVos = turnTaskToArchivedTaskVo(tasks);

//        返回Vo封装类
        return ResponseResult.okResult(archiveIdTaskVos);
    }


    /**
     * @Description: TODO 获取已归档的事项（状态：10）
     * @Date: 2024/1/1
     **/
    @Override
    public ResponseResult getArchivedTask() {

//        获取已归档的事项
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getState, TaskConstant.TASK_ARCHIVED);
        List<Task> tasks = taskMapper.selectList(taskWrapper);

//            封装Vo类
        List<TaskResponseVo> taskVoData = turnTaskToVo(tasks);

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);
    }


    /**
     * @Description: TODO 将task结果封装为对应的Vo类
     * @Date: 2024/1/1
     * @Param tasks:
     **/
    public List<TaskResponseVo> turnTaskToVo(List<Task> tasks) {
//            Vo类结果集合
        List<TaskResponseVo> taskVoData = new ArrayList<>();
//            将每个task封装为Vo类
        for (Task task : tasks) {
            TaskResponseVo taskResponseVo = new TaskResponseVo();
//            封装Vo类的相同属性内容
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
//            设置事项id
            taskResponseVo.setTaskId(task.getId());
//            设置协办单位名称数组
            taskResponseVo.setStasker(getSTaskerName(task.getStasker()));
//            设置主办单位名称
            Dept mastakerDept = deptMapper.selectById(Integer.parseInt(taskResponseVo.getMtasker()));
            taskResponseVo.setMtasker(mastakerDept.getDeptName());
//            修改statue名称
            taskResponseVo.setState(TaskUtils.generateStatusStringForDirector(task.getState()));
            taskVoData.add(taskResponseVo);
        }

//        按时间顺序排序
        taskVoData.sort(Comparator.comparing(TaskResponseVo::getStartTime).reversed());

        return taskVoData;
    }


    /**
     * @Description: TODO 督办人员提交的待审事项(状态 1)
     * @Date: 2024/1/3
     **/
    @Override
    public ResponseResult getReviewAfterSubmit() {

        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getState, TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
        List<Task> tasks = taskMapper.selectList(taskWrapper);

//            封装Vo类
        List<TaskResponseVo> taskVoData = turnTaskToVo(tasks);

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }


    /**
     * @Description: TODO 2.办结待审：
     * 查看主办单位执行完提交反馈，督办主任还未审核的事项清单（状态：7）
     * @Date: 2024/1/3
     **/
    @Override
    public ResponseResult getReviewTaskAfterFeedback() {

        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getState, TaskConstant.UNIT_LEADER_REJECTED);
        List<Task> tasks = taskMapper.selectList(taskWrapper);

//            封装Vo类
        List<TaskResponseVo> taskVoData = turnTaskToVo(tasks);

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }

    /**
     * @Description: TODO 获取事项办理结果(督办主任)
     * @Date: 2024/1/3
     * @Param taskId:
     **/
    @Override
    public ResponseResult getTaskResult(Integer taskId) {

//        根据id获取对应任务数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }
//        封装结果结果信息
        TaskResultResponseVo taskVo = new TaskResultResponseVo();
//        获取督办主任对应的任务结果文字显示
        taskVo.setResult(TaskUtils.generateStatusStringForDirector(task.getState()));
//        设置附件信息
        taskVo.setAttachment(JSON.parseArray(task.getAttachment(), AttachmentVo.class));

        return ResponseResult.okResult(taskVo);
    }

    /**
     * @Description: TODO  将task类封装为对应的归档事项内容
     * @Date: 2024/1/1
     * @Param tasks:
     **/
    public List<ArchiveIdTaskResponseVo> turnTaskToArchivedTaskVo(List<Task> tasks) {
//       定义封装类结果集
        List<ArchiveIdTaskResponseVo> archiveIdTaskVos = new ArrayList<>();
//       封装Vo类
        for (Task task : tasks) {
            ArchiveIdTaskResponseVo archiveIdTask = new ArchiveIdTaskResponseVo();
            archiveIdTask.setArchiveId(task.getId());
            archiveIdTask.setArchiveNo(task.getTaskNo());
//            在流程表查询该事项交办时的流程,以便查询归档时间
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, task.getId())
                    .eq(Flow::getAction, TaskActionConstant.ARCHIVED);
            Flow flow = flowMapper.selectOne(flowWrapper);
            archiveIdTask.setArchiveDate(flow.getExcuteTime());
            archiveIdTask.setArchivePerson(flow.getExcuter());
            archiveIdTask.setTaskNo(task.getTaskNo());
            archiveIdTask.setTaskType(task.getTaskType());
            archiveIdTask.setIsVip(task.getIsVip());
            archiveIdTask.setTaskName(task.getTaskName());
            Dept mtakserDept = deptMapper.selectById(Integer.parseInt(task.getMtasker()));
            archiveIdTask.setMTasker(mtakserDept.getDeptName());
            archiveIdTask.setCompleteDate(task.getEndTime());
            archiveIdTaskVos.add(archiveIdTask);
        }
        return archiveIdTaskVos;
    }


    /**
     * @Description: TODO  将task类封装为对应的归档事项内容
     * @Date: 2024/1/1
     * @Param str:
     **/
    public List<String> getSTaskerName(String str) {
//        先将字符串类型的协办单位id 转换为 （Integer）id数组
        Integer[] sTasks = TaskUtils.convertToIntArray(str);
//        定义协办单位名称数组
        List<String> sTasksName = new ArrayList<>();
//        根据id获取各个协办单位的名称
        for (Integer deptId : sTasks) {
            Dept dept = deptMapper.selectById(deptId);
            sTasksName.add(dept.getDeptName());
        }
//        返回协办单位名称数组
        return sTasksName;
    }


    /**
     * @Description: TODO 审批事项
     * @Date: 2024/1/3
     * @Param approverTaskRequestVo:
     **/
    @Override
    public ResponseResult approveTask(ApproverTaskRequestVo approverTaskRequestVo) {

//        设置 流程 与 协办单位流程 实体类
        Flow flow = new Flow();
        FlowAsist flowAsist = new FlowAsist();

//        获取督办主任User实体类
        LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        usersLambdaQueryWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT);
        Users directorUser = usersMapper.selectOne(usersLambdaQueryWrapper);

//        设置flow与Flowasisit的执行人
        flow.setExcuter(directorUser.getId());
        flowAsist.setExcuter(directorUser.getId());


//        将时间格式化
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

//        检测事项id是否正确
        Integer taskId = approverTaskRequestVo.getTaskId();
        if (taskId <= 0) {
            throw new BusinessException(AppHttpCodeEnum.JSON_ERROR);
        }

//        设置flow与flowasisit的任务id
        flow.setTaskId(taskId);
        flowAsist.setTaskId(taskId);

//        获取对应事项数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

        String approverStatue = approverTaskRequestVo.getApproveResult();
        if (approverStatue.equals("")) {
            throw new BusinessException(AppHttpCodeEnum.DATA_NULL);
        }

//        "0" 通过   事项状态改为3，督办主任审核通过
        if (approverStatue.equals("0")) {
            task.setState(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
            flow.setAction(TaskActionConstant.SUPERVISING_DIRECTOR_APPROVED);
            flowAsist.setAction(TaskActionConstant.SUPERVISING_DIRECTOR_APPROVED);
//            设置执行时间
            flow.setExcuteTime(new Date());
            flowAsist.setExcuteTime(new Date());
//        设置流程表之前与之后的状态
            flow.setStateBefore(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
            flow.setStateAfte(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
//        设置流程表下一个执行人id , 为主办单位领导id
            LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(Users::getRoleId, UserRoleConstant.OFFICE_DIRECTOR);
            Users officeDirectorUser = usersMapper.selectOne(userWrapper);
            flow.setNextExcuter(officeDirectorUser.getId());
//        设置积分
            flow.setScore(12);
        }


//        "1" 不通过 事项状态改为2，督办主任驳回
        if (approverStatue.equals("1")) {
            task.setState(TaskConstant.SUPERVISING_DIRECTOR_REJECTED);
            flow.setAction(TaskActionConstant.SUPERVISING_DIRECTOR_REJECTED);
            flowAsist.setAction(TaskActionConstant.SUPERVISING_DIRECTOR_REJECTED);
            flow.setExcuteTime(new Date());
            flowAsist.setExcuteTime(new Date());
//        设置流程表之前与之后的状态
            flow.setStateBefore(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
            flow.setStateAfte(TaskConstant.SUPERVISING_DIRECTOR_REJECTED);
//        设置流程表下一个执行人id , 为督办人员id
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, taskId)
                    .eq(Flow::getAction, TaskActionConstant.SUPERVISING_PERSONNEL_SUBMITTED);
            Flow beforeFlow = flowMapper.selectOne(flowWrapper);
            flow.setNextExcuter(beforeFlow.getExcuter());
//        设置积分
            flow.setScore(8);
        }


//        意见不为空，则设置意见
        String option = approverTaskRequestVo.getOpinion();
        if (!option.equals("")) {
            flow.setNote(option);
            flowAsist.setNote(option);
        }


//        将协办单位id转为数组,依次添加对应流程表
        Integer[] staskerIds = TaskUtils.convertToIntArray(task.getStasker());
        for (Integer staskerId : staskerIds) {
            FlowAsist staskerFlowAsist = flowAsist;
            staskerFlowAsist.setDeptId(staskerId);
            flowAsistMapper.insert(staskerFlowAsist);
        }

//        添加主办单位流程表
        flowMapper.insert(flow);
        taskMapper.updateById(task);

        return ResponseResult.okResult();
    }


    /**
     * @Description: TODO 终止任务
     * @Date: 2024/1/3
     * @Param taskId:
     **/
    @Override
    public ResponseResult suspentTask(int taskId) {

//        获取对应任务数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        设置对应流程
        Flow flow = new Flow();
        FlowAsist flowAsist = new FlowAsist();

        flow.setAction(TaskActionConstant.SUSPENDED);
        flowAsist.setAction(TaskActionConstant.SUSPENDED);

        flow.setStateBefore(task.getState());
        flow.setStateAfte(TaskConstant.TASK_TERMINATED);

        flow.setTaskId(taskId);
        flowAsist.setTaskId(taskId);
//        获取督办主任User实体类
        LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        usersLambdaQueryWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT);
        Users directorUser = usersMapper.selectOne(usersLambdaQueryWrapper);
//        设置flow与flowAsisit的执行人
        flow.setExcuter(directorUser.getId());
        flowAsist.setExcuter(directorUser.getId());
        flow.setExcuteTime(new Date());
        flowAsist.setExcuteTime(new Date());


        List<String> doingTask = Arrays.asList("3", "4", "5", "6", "7", "8", "9");
//        如果任务的状态包含在3-9里
        if (doingTask.contains(task.getState())) {
//            设置任务状态为已中止
            task.setState(TaskConstant.TASK_TERMINATED);
        } else {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL, "任务状态不在3-9");
        }

        taskMapper.updateById(task);
        return ResponseResult.okResult();
    }


    /**
     * @Description: TODO 获取部门积分列表
     * @Date: 2024/1/3
     **/
    @Override
    public ResponseResult getScoreList() {

        List<DeptScoretVo> deptScoretVos = new ArrayList<>();

        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.orderByDesc(Flow::getTaskId);
        flowWrapper.orderByDesc(Flow::getExcuteTime);
        flowWrapper.last("LIMIT 1");
//        获取所有流程表
        List<Flow> flows = flowMapper.selectList(null);
//        去重(获取相同taskId里执行时间最近的flow)
        flows = getLatestFlowsByTaskId(flows);

//        统计部门分数
        Map<Dept, Integer> deptScores = new HashMap<>();
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            Dept dept = deptMapper.selectById(Integer.parseInt(task.getMtasker()));
            int score = flow.getScore();

            // 检查部门是否已经在Map中存在，如果不存在则添加分数到Map中
            if (!deptScores.containsKey(dept)) {
                deptScores.put(dept, score);
            }

            // 将流程的分数累加到部门的分数上
            int currentScore = deptScores.get(dept);
            deptScores.put(dept, currentScore + score);
        }

        // 遍历deptScores属性
        for (Map.Entry<Dept, Integer> entry : deptScores.entrySet()) {
            Dept dept = entry.getKey();
            int score = entry.getValue();
//          封装Vo类
            DeptScoretVo deptScoretVo = new DeptScoretVo();
            deptScoretVo.setDeptId(dept.getId());
            deptScoretVo.setDeptName(dept.getDeptName());
            deptScoretVo.setScore(score);
            deptScoretVos.add(deptScoretVo);
        }

        return ResponseResult.okResult(deptScoretVos);
    }


    /**
     * @Description: TODO 根据部门id获取部门分数
     * @Date: 2024/1/3
     * @Param deptId:
     **/
    @Override
    public ResponseResult getScoreDetailById(Integer deptId) {

//        获取所有流程表
        List<Flow> flows = flowMapper.selectList(null);
//        去重(获取相同taskId里执行时间最近的flow)
        flows = getLatestFlowsByTaskId(flows);

//        统计部门分数
        Map<Dept, Integer> deptScores = new HashMap<>();
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            Dept dept = deptMapper.selectById(Integer.parseInt(task.getMtasker()));
            if (dept.getId() == deptId) {
                int score = flow.getScore();

                // 检查部门是否已经在Map中存在，如果不存在则添加分数到Map中
                if (!deptScores.containsKey(dept)) {
                    deptScores.put(dept, score);
                }

                // 将流程的分数累加到部门的分数上
                int currentScore = deptScores.get(dept);
                deptScores.put(dept, currentScore + score);
            }
        }

//        部门分数
        DeptScoretVo deptScoretVo = new DeptScoretVo();
        // 遍历deptScores属性
        for (Map.Entry<Dept, Integer> entry : deptScores.entrySet()) {
            Dept dept = entry.getKey();
            int score = entry.getValue();
//          封装Vo类
            deptScoretVo.setDeptId(dept.getId());
            deptScoretVo.setDeptName(dept.getDeptName());
            deptScoretVo.setScore(score);
        }

        return ResponseResult.okResult(deptScoretVo);

    }


    public List<Flow> getLatestFlowsByTaskId(List<Flow> flows) {
        Map<Integer, Flow> latestFlowsMap = new HashMap<>();

        for (Flow flow : flows) {
            int taskId = flow.getTaskId();
            Date excuteTime = flow.getExcuteTime();

            if (!latestFlowsMap.containsKey(taskId) || excuteTime.after(latestFlowsMap.get(taskId).getExcuteTime())) {
                latestFlowsMap.put(taskId, flow);
            }
        }

        return new ArrayList<>(latestFlowsMap.values());
    }


    /**
     * @Description: TODO 查看本单位待执行或被驳回（状态：3，8）
     * @Date: 2024/1/4
     * @Param userId:
     **/
    @Override
    public ResponseResult getUnclaimedTasks(int userId) {

//        先获取对应用户的数据
        Users user = usersMapper.selectById(userId);
//        数据为空，抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        根据部门id与状态（3，8）获取对应的数据
        List<String> checkString = new ArrayList<>();
        checkString.add(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
        checkString.add(TaskConstant.UNIT_LEADER_SUBMITTED);
        LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskLambdaQueryWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
        taskLambdaQueryWrapper.in(Task::getState, checkString);
//        获取结果集
        List<Task> tasks = taskMapper.selectList(taskLambdaQueryWrapper);
        List<TaskResponseVo> taskVos = turnTaskToVo(tasks);
        return ResponseResult.okResult(taskVos);
    }


    /**
     * @Description: TODO 领取事项
     * @Date: 2024/1/4
     * @Param userId:
     * @Param taskId:
     **/
    @Override
    public ResponseResult claimTask(Integer userId, Integer taskId) {

//        获取对应事项数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        设置流程表
        Flow flow = new Flow();

//        获取任务上一条的流程数据
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
//        去重(获取相同taskId里执行时间最近的flow)
        List<Flow> flows = getLatestFlowsByTaskId(flowMapper.selectList(flowWrapper));

//      检测签收时间是否超过24小时
        Date startTime = task.getStartTime();
        Date nowTime = new Date();
        int hours = (int) ((nowTime.getTime() - startTime.getTime()) / (1000 * 60 * 60));
        if (hours > 24) {
//            如果超时，上一个流程的分数-2
            flow.setScore(flows.get(0).getScore() - 2);
//            设置任务状态已逾期与行为
            flow.setAction(TaskActionConstant.ASSIGNED_OVERDUE);
            flow.setStateBefore(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
            flow.setStateAfte(TaskConstant.TASK_ASSIGNED_OVERDUE);
            task.setState(TaskConstant.TASK_ASSIGNED_OVERDUE);
        } else if (hours <= 24) {
//            如果未超时，分数不变
            flow.setScore(flows.get(0).getScore());
//            设置任务状态与行为
            flow.setAction(TaskActionConstant.ASSIGNED);
            flow.setStateBefore(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
            flow.setStateAfte(TaskConstant.TASK_ASSIGNED);
            task.setState(TaskConstant.TASK_ASSIGNED);
        }

        flow.setExcuter(userId);
        flow.setExcuteTime(new Date());
        flow.setTaskId(taskId);

        flowMapper.insert(flow);
        taskMapper.updateById(task);

        return ResponseResult.okResult();
    }


    /**
     * @Description: TODO 获取办事人员已提交但未审核的事项
     * @Date: 2024/1/4
     * @Param userId:
     **/
    @Override
    public ResponseResult getUnapprovedTasks(int userId) {

//        先获取对应用户的数据
        Users user = usersMapper.selectById(userId);
//        数据为空，抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        查询对应事项，状态为5，部门id为用户部门
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getMtasker,String.valueOf(user.getDeptId()));
        taskWrapper.eq(Task::getState,TaskConstant.TASK_ALLOCATED);

//        获取结果集
        List<Task> tasks = taskMapper.selectList(taskWrapper);
        List<TaskResponseVo> taskVos = turnTaskToVo(tasks);
        return ResponseResult.okResult(taskVos);
    }




    /**
     * @Description: TODO 获取本部门所有办事人员
     * @Date: 2024/1/4
     * @Param userId:
     **/
    @Override
    public ResponseResult getStaffList(int userId) {

//        先获取对应用户的数据
        Users user = usersMapper.selectById(userId);
//        数据为空，抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        查询对应用户
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getDeptId,user.getDeptId());
        List<Users> users = usersMapper.selectList(usersWrapper);

//        封装Vo类
        List<UserContentVo> userVos = new ArrayList<>();
        for (Users userData :users) {
            UserContentVo userContentVo = new UserContentVo(userData.getId(),userData.getUsername());
            userVos.add(userContentVo);
        }

        return ResponseResult.okResult(userVos);

    }


    /**
     * @Description: TODO 获取已经领办待未分配办事人员的事项
     * @Date: 2024/1/4
     * @Param userId: 
     **/
    @Override
    public ResponseResult getClaimedTasks(int userId) {

//        先获取对应用户的数据
        Users user = usersMapper.selectById(userId);
//        数据为空，抛出异常
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.SEARACH_NULL);
        }

//        查询对应事项，状态为5，部门id为用户部门
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getMtasker,String.valueOf(user.getDeptId()));
        taskWrapper.eq(Task::getState,TaskConstant.TASK_ASSIGNED);

//        获取结果集
        List<Task> tasks = taskMapper.selectList(taskWrapper);
        List<TaskResponseVo> taskVos = turnTaskToVo(tasks);
        return ResponseResult.okResult(taskVos);
    }



}
