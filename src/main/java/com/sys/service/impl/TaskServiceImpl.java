package com.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.constant.*;
import com.sys.entity.*;
import com.sys.entity.RequestVo.AttachmentVo;
import com.sys.entity.RequestVo.SubmitRejectTaskVo;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.ResponseVo.*;
import com.sys.excption.BusinessException;
import com.sys.mapper.*;
import com.sys.service.TaskService;
import com.sys.utils.BeanCopyUtils;
import com.sys.utils.TaskUtils;
import io.swagger.models.auth.In;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

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
    private FlowdetailMapper flowdetailMapper;
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

        Integer createId = submitTaskRequestVo.getCreatorId();
        Users users = usersMapper.selectById(createId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "创建者用户为空");
        }

//        创建者id
        if (submitTaskRequestVo.getCreatorId() != null) {
            task.setCreateBy(submitTaskRequestVo.getCreatorId());
        } else {
            throw new BusinessException(ErrorCode.DATA_NULL);
        }

        task.setCreateName(users.getUsername());

//        事项编号
        if (submitTaskRequestVo.getForm().getTaskNo() != null) {
            String taskNo = submitTaskRequestVo.getForm().getTaskNo();
            task.setTaskNo(taskNo);
        }

//        事项类型
        if (submitTaskRequestVo.getForm().getTaskType() != null) {
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
            throw new BusinessException(ErrorCode.DATA_NULL, "taskName为空");
        }

//        事项描述
        String taskDetail = submitTaskRequestVo.getForm().getTaskDetail();
        if (taskDetail != null) {
            task.setTaskDetail(taskDetail);
        }

//        主办单位id
        Integer[] mTaskerids = submitTaskRequestVo.getForm().getMTaskerid();
        if (mTaskerids != null && mTaskerids.length > 0) {
            String innerMTaskeridsString = "";
            for (int i = 0; i < mTaskerids.length; i++) {
//                如果不是最后一次，则添加“，”
                if (i != mTaskerids.length - 1) {
                    innerMTaskeridsString += mTaskerids[i].toString() + ",";
                }
//                如果是最后一次，不添加“，”即可
                if (i == mTaskerids.length - 1) {
                    innerMTaskeridsString += mTaskerids[i].toString();
                }
            }
            task.setMtasker(innerMTaskeridsString);
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


        taskMapper.insert(task);

        return ResponseResult.okResult();
    }


    /**
     * @Description: 通过用户id获取所有任务(督办人员与督办主任)
     */
    @Override
    public ResponseResult getAllTask() {

//        根据权限不同返回不同数据，获取状态为1-11的事项
        //督办单位
        List<String> supervisingUnitStatusList = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

//        定义查询结果
        List<Task> taskData;
//        封装结果Vo类
        List<TaskResponseVo> taskVoData = new ArrayList<>();
//        进行查询
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getState, supervisingUnitStatusList);
//          获取查询结果
        taskData = taskMapper.selectList(taskWrapper);

//                封装Vo类
        for (Task task : taskData) {
            TaskResponseVo taskResponseVo = new TaskResponseVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//          获取事项下对应部门的状态
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, task.getId());
            List<Flow> flows = flowMapper.selectList(flowWrapper);
            List<JSONObject> statuesList = new ArrayList<>();
            for (Flow flow : flows) {
                JSONObject jsonObject = new JSONObject();
                Dept dept = deptMapper.selectById(flow.getDeptId());
                if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
                    return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未查找到流程表对应部门");
                }
                jsonObject.put(dept.getDeptName(), flow.getStatus());
                statuesList.add(jsonObject);
            }
//            设置状态
            taskResponseVo.setStatus(statuesList);

            taskVoData.add(taskResponseVo);
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskResponseVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }


    @Override
    public ResponseResult getAllTask(Long userId) {

        Users users = usersMapper.selectById(userId);
        if(users == null){
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL,"用户为空");
        }

//主办单位可查询的状态
        List<String> sponsorDirectorStatusList = Arrays.asList("2", "3", "4", "5", "6", "7", "8", "9");
//        定义封装结果Vo类
        List<TaskContentNormalVo> taskVoData = new ArrayList<>();
//        先获取该部门对应的事项id
        LambdaQueryWrapper<Flow> flowWrapperToFindTask = new LambdaQueryWrapper<>();
        flowWrapperToFindTask.eq(Flow::getDeptId,users.getDeptId());
        List<Flow> flowsToFindTask = flowMapper.selectList(flowWrapperToFindTask);
        int[] taskIds = flowsToFindTask.stream()
                .mapToInt(Flow::getTaskId)
                .toArray();
//        查询对应事项
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getId,taskIds).in(Task::getState,sponsorDirectorStatusList);

//          获取查询结果
        List<Task> taskData = taskMapper.selectList(taskWrapper);
//          封装Vo类
        for (Task task : taskData) {
            TaskContentNormalVo taskResponseVo = new TaskContentNormalVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置状态
            taskResponseVo.setStatus(task.getState());

            taskVoData.add(taskResponseVo);
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskContentNormalVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);
    }


    /**
     * @Description:  查看或编辑事项时获取事项内容进行展示---根据taskId获取事项内容
     * @Date: 2024/1/1
     * @Param taskId:
     **/
    @Override
    public ResponseResult getTaskById(Integer taskId) {

//        获取对应事项数据实体类
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.SEARACH_NULL);
        }
//        定义Vo封装类
        TaskContentVo taskContentVo = BeanCopyUtils.copyBean(task, TaskContentVo.class);

//        定义事项id
        taskContentVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>) 与 id数组
        taskContentVo.setSTaske(getTaskerName(task.getStasker()));
        taskContentVo.setSTaskerId(TaskUtils.convertToIntArray(task.getStasker()));

//        设置主办单位名称 与 id数组
        taskContentVo.setMTasker(getTaskerName(task.getMtasker()));
        taskContentVo.setMTaskerId(TaskUtils.convertToIntArray(task.getMtasker()));

//        设置附件
        List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
        taskContentVo.setAttachment(attachmentVos);

//        设置流程表
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        List<Flowdetail> flowdetails = new ArrayList<>();

        for (Flow flow : flows) {
            LambdaQueryWrapper<Flowdetail> flowdetailWrapper = new LambdaQueryWrapper<>();
            flowdetailWrapper.eq(Flowdetail::getFlowId, flow.getId());
            flowdetails.addAll(flowdetailMapper.selectList(flowdetailWrapper));
        }


//        如果事项含有对应流程
        if (flowdetails.size() != 0) {
//        封装flow Vo类
            List<FlowVo> flowVos = BeanCopyUtils.copyBeanList(flowdetails, FlowVo.class);
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
        fileVo.setUuid(returnName.replace(file.getOriginalFilename(), ""));

        return ResponseResult.okResult(fileVo);
    }


    //3.重点提示事项：查看领办逾期（状态3）、反馈逾期（状态7）、反馈驳回（状态9）的事项清单，可以点开某一条查看详情和办理过程中的流程信息
    @Override
    public ResponseResult getKeytipTasks() {

        List<TaskContentNoFlowVo> taskVos = new ArrayList<>();

        List<String> statueList = Arrays.asList(TaskConstant.OVERDUE_APPLICATION, TaskConstant.FEEDBACK_OVERDUE, TaskConstant.FEEDBACK_REJECTION);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.in(Flow::getStatus, statueList);
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            TaskContentNoFlowVo taskVo = BeanCopyUtils.copyBean(task, TaskContentNoFlowVo.class);

//        定义事项id
            taskVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>)
            taskVo.setSTaske(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMTasker(getTaskerName(task.getMtasker()));
//          设置状态
            taskVo.setStatus(flow.getStatus());
//        设置附件
            List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
            taskVo.setAttachment(attachmentVos);
            taskVos.add(taskVo);
        }

        return ResponseResult.okResult(taskVos);
    }

    @Override
    public ResponseResult getTrackedTasks() {

        List<TaskContentNoFlowVo> taskVos = new ArrayList<>();

        List<String> statueList = Arrays.asList(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED, TaskConstant.AUDIT_MATTERS, TaskConstant.TASK_ALLOCATED, TaskConstant.TASK_EXECUTED, TaskConstant.UNIT_LEADER_SUBMITTED);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.in(Flow::getStatus, statueList);
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            TaskContentNoFlowVo taskVo = BeanCopyUtils.copyBean(task, TaskContentNoFlowVo.class);

//        定义事项id
            taskVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>)
            taskVo.setSTaske(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMTasker(getTaskerName(task.getMtasker()));
//          设置状态
            taskVo.setStatus(flow.getStatus());
//        设置附件
            List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
            taskVo.setAttachment(attachmentVos);
            taskVos.add(taskVo);
        }

        return ResponseResult.okResult(taskVos);

    }




    /**
     * @Description: 获取已归档的事项（状态：10） 督办人员
     * @Date: 2024/1/1
     **/
    @Override
    public ResponseResult getArchivedTask() {

        List<ArchiveIdTaskResponseVo> taskVos = new ArrayList<>();

        List<String> statueList = Arrays.asList(TaskConstant.PASS_EXAMINATION);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.in(Flow::getStatus, statueList);
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            ArchiveIdTaskResponseVo taskVo = BeanCopyUtils.copyBean(task, ArchiveIdTaskResponseVo.class);

//        设置协办单位名称数组(List<String>)
            taskVo.setSTaske(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMTasker(getTaskerName(task.getMtasker()));
            taskVos.add(taskVo);
        }

        return ResponseResult.okResult(taskVos);
    }


    /**
     * @Description: 将task类封装为对应的归档事项内容
     * @Date: 2024/1/1
     * @Param str:
     **/
    public List<String> getTaskerName(String str) {
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
     * @Description: TODO 获取部门积分列表
     * @Date: 2024/1/3
     **/
    @Override
    public ResponseResult getScoreList(String beginDate, String endDate) {

        List<DeptScoretVo> deptScoretVos = new ArrayList<>();

        LambdaQueryWrapper<Flowdetail> flowdetailWrapper = new LambdaQueryWrapper<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (!beginDate.equals("") && !endDate.equals("")) {
            try {
                Date beginDateTime = formatter.parse(beginDate);
                Date endDateTime = formatter.parse(endDate);
                flowdetailWrapper
                        .between(Flowdetail::getExcuteTime, beginDateTime, endDateTime);
            } catch (ParseException e) {
                throw new BusinessException(ErrorCode.JSON_ERROR, "时间转换错误");
            }
        }

        List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailWrapper);

        if(flowdetails.size() == 0){
            return ResponseResult.okResult();
        }

        Map<Integer, Double> deptScore = new HashMap<>();

        for (Flowdetail flowdetail : flowdetails) {

            Flow flow = flowMapper.selectById(flowdetail.getFlowId());

            boolean judge = false;

            if (deptScore.containsKey(flow.getDeptId())) {
                deptScore.replace(flow.getDeptId(), deptScore.get(flow.getDeptId()) + flowdetail.getScore());
                judge = true;
                break;
            }

            if (!judge) {
                deptScore.put(flow.getDeptId(), flowdetail.getScore());
            }
        }
// 遍历deptScores属性
        for (Map.Entry<Integer, Double> entry : deptScore.entrySet()) {
            int deptId = entry.getKey();
            Double score = entry.getValue();
//          封装Vo类
            Dept dept = deptMapper.selectById(deptId);
            DeptScoretVo deptScoretVo = new DeptScoretVo();
            deptScoretVo.setScore(score);
            deptScoretVo.setDeptId(dept.getId());
            deptScoretVo.setDeptName(dept.getDeptName());
            deptScoretVos.add(deptScoretVo);
        }


        return ResponseResult.okResult(deptScoretVos);
    }

    @Override
    public ResponseResult getScoreDetail(String beginDate, String endDate, Integer deptId) {

        List<DeptScoreDetailVo> deptScoretVos = new ArrayList<>();

//        判断部门是否为空
        Dept dept = deptMapper.selectById(deptId);
        if(dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)){
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL,"没找到deptId对应的部门");
        }


        LambdaQueryWrapper<Flowdetail> flowdetailWrapper = new LambdaQueryWrapper<>();
//      先筛选时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (!beginDate.equals("") && !endDate.equals("")) {
            try {
                Date beginDateTime = formatter.parse(beginDate);
                Date endDateTime = formatter.parse(endDate);
                flowdetailWrapper
                        .between(Flowdetail::getExcuteTime, beginDateTime, endDateTime);
            } catch (ParseException e) {
                throw new BusinessException(ErrorCode.JSON_ERROR, "时间转换错误");
            }
        }
//        再根据部门id进行筛选
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getDeptId, deptId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);
//       获取 flows 中所有流程的 id 列表
        List<Integer> flowIds = flows.stream().map(Flow::getId).collect(Collectors.toList());
//      在 flowdetailWrapper 中使用 in 方法
        flowdetailWrapper.in(Flowdetail::getFlowId, flowIds);
//        获取筛选出来的结果
        List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailWrapper);


        for (Flowdetail flowdetail : flowdetails) {

            Flow flow = flowMapper.selectById(flowdetail.getFlowId());
            Task task = taskMapper.selectById(flow.getTaskId());

            DeptScoreDetailVo deptScoreDetailVo = new DeptScoreDetailVo();

            deptScoreDetailVo = BeanCopyUtils.copyBean(task, DeptScoreDetailVo.class);
            deptScoreDetailVo.setTaskId(task.getId());
            deptScoreDetailVo.setStatus(flowdetail.getAction());
            deptScoreDetailVo.setMTasker(getTaskerName(task.getMtasker()));
            deptScoreDetailVo.setSTasker(getTaskerName(task.getStasker()));
            deptScoreDetailVo.setTaskScore(flowdetail.getScore());

            deptScoretVos.add(deptScoreDetailVo);
        }


        return ResponseResult.okResult(deptScoretVos);
    }

    /**
     * @Description: 根据id获取事项内容
     * @Date: 2024/1/23
     * @Param taskId:
     **/
    @Override
    public ResponseResult getTaskResultById(Long taskId) {

        Task task = taskMapper.selectById(taskId);
        if(task == null){
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL,"未找到taskId对应的实体类数据");
        }

        List<DeptTaskResponseVo> deptTaskResponseVos = new ArrayList<>();

        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);

        for (Flow flow : flows) {
            DeptTaskResponseVo deptTaskResponseVo = new DeptTaskResponseVo();
            deptTaskResponseVo.setDeptId(flow.getDeptId());
            deptTaskResponseVo.setResult(TaskUtils.generateStatusStringForSupervisor(flow.getStatus()));
            deptTaskResponseVos.add(deptTaskResponseVo);
        }

        return ResponseResult.okResult(deptTaskResponseVos);
    }

    @Override
    public ResponseResult approveTaskById() {

        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getState, TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
        List<Task> tasks = taskMapper.selectList(taskWrapper);

        for (Task task : tasks) {
//        更新任务状态
            task.setState(TaskConstant.AUDIT_MATTERS);
//        获取督查单位主任
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
            usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);
//        更新数据
            task.setApproveBy(usersSuperIntendent.getId());
            task.setApproverName(usersSuperIntendent.getUsername());
            task.setApproveTime(new Date());

//        向流程表中插入数据
//        主办单位
            Integer[] mtaskersId = TaskUtils.convertToIntArray(task.getMtasker());

            for (Integer id : mtaskersId) {
//        获取对象主办单位执行人
                usersWrapper.clear();
                usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
                Users users = usersMapper.selectOne(usersWrapper);
                if (users == null) {
                    return ResponseResult.errorResult(ErrorCode.DATA_NULL,"主办单位执行人为空");
                }
                Flow flow = new Flow();
                flow.setTaskId(task.getId());
                flow.setDeptId(id);
                flow.setExcuter(users.getId());
                flow.setType(DeptType.ORGANIZER_DEPT);
                flow.setStatus(TaskConstant.AUDIT_MATTERS);
                flowMapper.insert(flow);
            }
//        协办单位
            Integer[] staskersId = TaskUtils.convertToIntArray(task.getStasker());

            for (Integer id : staskersId) {
//        获取协办单位执行人
                usersWrapper.clear();
                usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
                Users users = usersMapper.selectOne(usersWrapper);
                if (users == null) {
                    return ResponseResult.errorResult(ErrorCode.DATA_NULL,"协办单位执行人为空");
                }
                Flow flow = new Flow();
                flow.setTaskId(task.getId());
                flow.setDeptId(id);
                flow.setExcuter(users.getId());
                flow.setType(DeptType.CO_ORGANIZER_DEPT);
                flow.setStatus(TaskConstant.AUDIT_MATTERS);
                flowMapper.insert(flow);
            }
//          向数据库插入数据
            taskMapper.updateById(task);
        }
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult approveResultById(Long taskId) {

//        检测taskId下所有单位审核是否已经通过
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);

        for (Flow flow : flows) {
            if (!flow.getStatus().equals(TaskConstant.PASS_EXAMINATION)) {
                return ResponseResult.errorResult(ErrorCode.STATUE_NOT_ALL);
            }
        }
//        将事项归档
//        获取事项实体类数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }
        task.setState(TaskConstant.PASS_EXAMINATION);
        task.setCompleteTime(new Date());
        taskMapper.updateById(task);

        return ResponseResult.okResult();
    }


    //
    @Override
    public ResponseResult submitRejectTask(SubmitRejectTaskVo submitRejectTaskVo) {


//        获取Task内容
        Task task = taskMapper.selectById(submitRejectTaskVo.getForm().getTaskId());
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }
//        更新数据
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        task = BeanCopyUtils.copyBean(submitRejectTaskVo.getForm(), Task.class);
        try {
            if(!submitRejectTaskVo.getForm().getStartTime().equals("")) {
                task.setStartTime(formatter.parse(submitRejectTaskVo.getForm().getStartTime()));
            }
            if(!submitRejectTaskVo.getForm().getEndTime().equals("")) {
                task.setEndTime(formatter.parse(submitRejectTaskVo.getForm().getEndTime()));
            }
            if(!submitRejectTaskVo.getForm().getAttachment().isEmpty()) {
                task.setAttachment(JSON.toJSONString(submitRejectTaskVo.getForm().getAttachment()));
            }
        } catch (ParseException e) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR,"时间转换错误");
        }

//        设置task表的状态为9
        task.setState(TaskConstant.FEEDBACK_REJECTION);
//        获取反馈通过与未通过的数据单位id
        List<Integer> passId = new ArrayList<>();
        passId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getMtasker())));
        passId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getStasker())));
        passId.removeAll(Arrays.asList(submitRejectTaskVo.getForm().getMTaskerid()));
        passId.removeAll(Arrays.asList(submitRejectTaskVo.getForm().getSTaskerid()));
        List<Integer> noPassId = new ArrayList<>();
        noPassId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getMtasker())));
        noPassId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getStasker())));
        noPassId.removeAll(passId);

//        处理审核通过的事项
        for (Integer id : passId) {
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, task.getId()).eq(Flow::getDeptId, id);
            Flow flow = flowMapper.selectOne(flowWrapper);
            flow.setStatus(TaskConstant.PASS_EXAMINATION);
//            获取对应单位执行人
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
            usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users users = usersMapper.selectOne(usersWrapper);
            flow.setExcuter(users.getId());
            flowMapper.updateById(flow);
//            flowdetail表中插入一条督办主任审核通过的步骤
            Flowdetail flowdetail = new Flowdetail();
//        获取督查单位主任
            usersWrapper.clear();
            usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);
            flowdetail.setExcuter(usersSuperIntendent.getId());
            flowdetail.setExcutername(usersSuperIntendent.getUsername());
            flowdetail.setExcuteTime(new Date());
            flowdetail.setFlowId(flow.getId());
            flowdetail.setAction(TaskActionConstant.FEEDBACK_RESULT_PASS);
            flowdetailMapper.insert(flowdetail);
        }

//        处理审核不通过的事项
        for (Integer id : noPassId) {
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, task.getId()).eq(Flow::getDeptId, id);
            Flow flow = flowMapper.selectOne(flowWrapper);
            flow.setStatus(TaskConstant.FEEDBACK_REJECTION);
            flow.setExcuter(null);
            flowMapper.updateById(flow);
//            flowdetail表中插入一条督办主任审核通过的步骤
            Flowdetail flowdetail = new Flowdetail();
//        获取督查单位主任
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
            usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);
            flowdetail.setExcuter(usersSuperIntendent.getId());
            flowdetail.setExcutername(usersSuperIntendent.getUsername());
            flowdetail.setExcuteTime(new Date());
            flowdetail.setFlowId(flow.getId());
            flowdetail.setAction(TaskActionConstant.REJECT_FEEDBACK_RESULT);
//            查询本季度该部门历史扣分有几次
            // 获取本季度的开始日期和结束日期
            LocalDate now = LocalDate.now();
            LocalDate startOfQuarter = LocalDate.of(now.getYear(), now.getMonth().firstMonthOfQuarter(), 1);
            LocalDate endOfQuarter = startOfQuarter.plusMonths(2).withDayOfMonth(Month.of(startOfQuarter.getMonthValue() + 2).maxLength());

            // 构建查询条件
            LambdaQueryWrapper<Flowdetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Flowdetail::getFlowId, flow.getId())
                    .ge(Flowdetail::getExcuteTime, startOfQuarter.atStartOfDay())
                    .lt(Flowdetail::getExcuteTime, endOfQuarter.plusDays(1).atStartOfDay());
            List<Flowdetail> flowdetails = flowdetailMapper.selectList(wrapper);

            if (flowdetails.size() < 3) {
                flowdetail.setScore(-0.3);
            } else {
                flowdetail.setScore((double) -1);
            }
//          设置事项驳回时间
            task.setRejectTime(new Date());
            flowdetailMapper.insert(flowdetail);
        }

//          更新事项数据
        taskMapper.updateById(task);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult suspentTaskById(Long taskId) {

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }
//      如果事项状态不在“3”-“7”之间，抛异常
        if (!(task.getState().compareTo(TaskConstant.OVERDUE_APPLICATION) >= 0
                && task.getState().compareTo(TaskConstant.FEEDBACK_OVERDUE) <= 0)) {
            return ResponseResult.errorResult(ErrorCode.STATUE_ERROR, "事项状态应在3-7之间");
        }

        task.setState(TaskConstant.TASK_ARCHIVED);
        taskMapper.updateById(task);
        return ResponseResult.okResult();
    }



//
//
//    /**
//     * @Description: TODO 根据部门id获取部门分数 督办主任
//     * @Date: 2024/1/3
//     * @Param deptId:
//     **/
//    @Override
//    public ResponseResult getScoreDetailById(Integer deptId) {
//
////        获取所有流程表
//        List<Flow> flows = flowMapper.selectList(null);
////        去重(获取相同taskId里执行时间最近的flow)
//        flows = getLatestFlowsByTaskId(flows);
//
////        统计部门分数
//        Map<Dept, Integer> deptScores = new HashMap<>();
//        for (Flow flow : flows) {
//            Task task = taskMapper.selectById(flow.getTaskId());
//            Dept dept = deptMapper.selectById(Integer.parseInt(task.getMtasker()));
//            if (dept.getId() == deptId) {
//                int score = flow.getScore();
//
//                // 检查部门是否已经在Map中存在，如果不存在则添加分数到Map中
//                if (!deptScores.containsKey(dept)) {
//                    deptScores.put(dept, score);
//                }
//
//                // 将流程的分数累加到部门的分数上
//                int currentScore = deptScores.get(dept);
//                deptScores.put(dept, currentScore + score);
//            }
//        }
//
////        部门分数
//        DeptScoretVo deptScoretVo = new DeptScoretVo();
//        // 遍历deptScores属性
//        for (Map.Entry<Dept, Integer> entry : deptScores.entrySet()) {
//            Dept dept = entry.getKey();
//            int score = entry.getValue();
////          封装Vo类
//            deptScoretVo.setDeptId(dept.getId());
//            deptScoretVo.setDeptName(dept.getDeptName());
//            deptScoretVo.setScore(score);
//        }
//
//        return ResponseResult.okResult(deptScoretVo);
//
//    }
//
//
//    /**
//     * @Description: TODO 获取该流程表数据里每个事项最近的流程
//     * @Date: 2024/1/5
//     * @Param flows:
//     **/
//    public List<Flow> getLatestFlowsByTaskId(List<Flow> flows) {
//        Map<Integer, Flow> latestFlowsMap = new HashMap<>();
//
//        for (Flow flow : flows) {
//            int taskId = flow.getTaskId();
//            Date excuteTime = flow.getExcuteTime();
//
//            if (!latestFlowsMap.containsKey(taskId) || excuteTime.after(latestFlowsMap.get(taskId).getExcuteTime())) {
//                latestFlowsMap.put(taskId, flow);
//            }
//        }
//
//        return new ArrayList<>(latestFlowsMap.values());
//    }
//
//
//    /**
//     * @Description: TODO 获取该事项里最近的流程
//     * @Date: 2024/1/5
//     * @Param taskId:
//     **/
//    public Flow getLastFlowFromTask(int taskId) {
//        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
//        flowWrapper.eq(Flow::getTaskId, taskId);
//        flowWrapper.orderByDesc(Flow::getExcuteTime);
//        flowWrapper.last("LIMIT 1");
//
//        Flow latestFlow = flowMapper.selectOne(flowWrapper);
//        return latestFlow;
//    }
//
//
//    /**
//     * @Description: TODO 查看本单位待执行或被驳回（状态：3，14） 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getUnclaimedTasks(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        根据部门id与状态（3，14）获取对应的数据
//        List<String> checkString = new ArrayList<>();
//        checkString.add(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
//        checkString.add(TaskConstant.TASK_ASSIGNED_OVERDUE);
//        LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        taskLambdaQueryWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
//        taskLambdaQueryWrapper.in(Task::getState, checkString);
////        获取结果集
//        List<Task> tasks = taskMapper.selectList(taskLambdaQueryWrapper);
//        List<TaskResponseVo> taskVos = turnTaskToVo(tasks, UserRoleConstant.OFFICE_DIRECTOR);
//        return ResponseResult.okResult(taskVos);
//    }
//
//
//    /**
//     * @Description: TODO 领取事项 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     * @Param taskId:
//     **/
//    @Override
//    public ResponseResult claimTask(Integer userId, Integer taskId) {
//
////        获取对应事项数据
//        Task task = taskMapper.selectById(taskId);
//        if (task == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        设置流程表
//        Flow flow = new Flow();
//
////        获取任务上一条的流程数据
//        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
//        flowWrapper.eq(Flow::getTaskId, taskId);
////        去重(获取相同taskId里执行时间最近的flow)
//        List<Flow> flows = getLatestFlowsByTaskId(flowMapper.selectList(flowWrapper));
//
//////      检测签收时间是否超过24小时
////        Date startTime = task.getStartTime();
////        Date nowTime = new Date();
////        int hours = (int) ((nowTime.getTime() - startTime.getTime()) / (1000 * 60 * 60));
////        if (hours > 24) {
//////            如果超时，上一个流程的分数-2
////            flow.setScore(flows.get(0).getScore() - 2);
//////            设置任务状态已逾期与行为
////            flow.setAction(TaskActionConstant.ASSIGNED_OVERDUE);
////            flow.setStateBefore(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
////            flow.setStateAfte(TaskConstant.TASK_ASSIGNED_OVERDUE);
////            task.setState(TaskConstant.TASK_ASSIGNED_OVERDUE);
////        } else if (hours <= 24) {
////            如果未超时，分数不变
//
//
//        flow.setScore(flows.get(0).getScore());
////            设置任务状态与行为
//        flow.setAction(TaskActionConstant.ASSIGNED);
//        flow.setStateBefore(TaskConstant.SUPERVISING_DIRECTOR_APPROVED);
//        flow.setStateAfte(TaskConstant.TASK_ASSIGNED);
//        task.setState(TaskConstant.TASK_ASSIGNED);
//
//
//        flow.setExcuter(userId);
//        flow.setExcuteTime(new Date());
//        flow.setTaskId(taskId);
//        flow.setNextExcuter(userId);
//
//        flowMapper.insert(flow);
//        taskMapper.updateById(task);
//
//        return ResponseResult.okResult();
//    }


//    /**
//     * @Description: TODO 获取办事人员已提交但未审核的事项 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getUnapprovedTasks(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        查询对应事项，状态为6，部门id为用户部门
//        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
//        taskWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
//        taskWrapper.eq(Task::getState, TaskConstant.TASK_EXECUTED);
//
////        获取结果集
//        List<Task> tasks = taskMapper.selectList(taskWrapper);
//        List<TaskResponseVo> taskVos = turnTaskToVo(tasks, UserRoleConstant.OFFICE_DIRECTOR);
//        return ResponseResult.okResult(taskVos);
//    }


//    /**
//     * @Description: TODO 获取本部门所有办事人员 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getStaffList(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        查询对应用户
//        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
//        usersWrapper.eq(Users::getDeptId, user.getDeptId());
//        List<Users> users = usersMapper.selectList(usersWrapper);
//
////        封装Vo类
//        List<UserContentVo> userVos = new ArrayList<>();
//        for (Users userData : users) {
//            UserContentVo userContentVo = new UserContentVo(userData.getId(), userData.getUsername());
//            userVos.add(userContentVo);
//        }
//
//        return ResponseResult.okResult(userVos);
//
//    }
//
//
//    /**
//     * @Description: TODO 获取已经领办待未分配办事人员的事项 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getClaimedTasks(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        查询对应事项，状态为4，部门id为用户部门
//        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
//        taskWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
//        taskWrapper.eq(Task::getState, TaskConstant.TASK_ASSIGNED);
//
////        获取结果集
//        List<Task> tasks = taskMapper.selectList(taskWrapper);
//        List<TaskResponseVo> taskVos = turnTaskToVo(tasks, UserRoleConstant.OFFICE_DIRECTOR);
//        return ResponseResult.okResult(taskVos);
//    }
//
//
//    /**
//     * @Description: TODO 获取已分配给办事人员但未反馈的事项 主办单位主任
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getAssignedTasks(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        查询对应事项，状态为5，部门id为用户部门
//        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
//        taskWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
//        taskWrapper.eq(Task::getState, TaskConstant.TASK_ALLOCATED);
//
////        获取结果集
//        List<Task> tasks = taskMapper.selectList(taskWrapper);
//        List<TaskResponseVo> taskVos = turnTaskToVo(tasks, UserRoleConstant.OFFICE_DIRECTOR);
//        return ResponseResult.okResult(taskVos);
//    }
//
//
//    /**
//     * @Description: TODO 获取已审批通过提交给督办主任的事项 状态8 主办单位
//     * @Date: 2024/1/4
//     * @Param userId:
//     **/
//    @Override
//    public ResponseResult getSubmittedTasks(int userId) {
//
////        先获取对应用户的数据
//        Users user = usersMapper.selectById(userId);
////        数据为空，抛出异常
//        if (user == null) {
//            throw new BusinessException(ErrorCode.SEARACH_NULL);
//        }
//
////        查询对应事项，状态为5，部门id为用户部门
//        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
//        taskWrapper.eq(Task::getMtasker, String.valueOf(user.getDeptId()));
//        taskWrapper.eq(Task::getState, TaskConstant.UNIT_LEADER_SUBMITTED);
//
////        获取结果集
//        List<Task> tasks = taskMapper.selectList(taskWrapper);
//        List<TaskResponseVo> taskVos = turnTaskToVo(tasks, UserRoleConstant.OFFICE_DIRECTOR);
//        return ResponseResult.okResult(taskVos);
//
//    }
//

}
