package com.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.constant.*;
import com.sys.entity.*;
import com.sys.entity.RequestVo.*;
import com.sys.entity.ResponseVo.*;
import com.sys.excption.BusinessException;
import com.sys.mapper.*;
import com.sys.service.TaskService;
import com.sys.utils.BeanCopyUtils;
import com.sys.utils.TaskUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.function.Function;
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

//        检查创建者是否为空
        Integer createId = submitTaskRequestVo.getCreatorId();
        Users users = usersMapper.selectById(createId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "创建者用户为空");
        }

//      检查督查室主任是否为空
        LambdaQueryWrapper<Dept> deptLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deptLambdaQueryWrapper.eq(Dept::getDeptName, "督查室").eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
        Dept deptDuCha = deptMapper.selectOne(deptLambdaQueryWrapper);
        if (deptDuCha == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "督查室部门未创建，请创建一个");
        }

//        检查创建者是否为督查室人员
        if (users.getDeptId() != deptDuCha.getId()) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "该员工不是督查室部门");
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

            if (users.getRoleId().equals(UserRoleConstant.SUPERVISOR)) {
                LambdaQueryWrapper<Task> findTaskNoWrapper = new LambdaQueryWrapper<>();

                findTaskNoWrapper.eq(Task::getTaskNo, submitTaskRequestVo.getForm().getTaskNo());
                List<Task> tasks = taskMapper.selectList(findTaskNoWrapper);
                if (tasks.size() > 0) {
                    return ResponseResult.errorResult(ErrorCode.TASKNO_REPEATE);
                }
            }

            if (users.getRoleId().equals(UserRoleConstant.SUPERINTENDENT)) {
                LambdaQueryWrapper<Task> findTaskNoWrapper = new LambdaQueryWrapper<>();

                findTaskNoWrapper.eq(Task::getTaskNo, submitTaskRequestVo.getForm().getTaskNo());
                List<Task> tasks = taskMapper.selectList(findTaskNoWrapper);
                for (Task task1 : tasks) {
                    if (task1.getApproveBy() != null) {
                        return ResponseResult.errorResult(ErrorCode.TASKNO_REPEATE);
                    }
                }
            }

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
        Integer[] mtaskerids = submitTaskRequestVo.getForm().getMtaskerid();
        for (Integer deptId : mtaskerids) {

            Dept dept = deptMapper.selectById(deptId);
            if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
                return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到根据id：" + deptId + "对应主办单位");
            }
//        检查部门下属人员是否存在
            LambdaQueryWrapper<Users> deptUserWrapper = new LambdaQueryWrapper<>();
            deptUserWrapper.eq(Users::getDeptId, deptId);
            if (usersMapper.selectList(deptUserWrapper).size() == 0) {
                return ResponseResult.errorResult(ErrorCode.DEPT_NO_CONTAIN_USER);
            }
        }
        if (mtaskerids != null && mtaskerids.length > 0) {
            String innermtaskeridsString = "";
            for (int i = 0; i < mtaskerids.length; i++) {
//                如果不是最后一次，则添加“，”
                if (i != mtaskerids.length - 1) {
                    innermtaskeridsString += mtaskerids[i].toString() + ",";
                }
//                如果是最后一次，不添加“，”即可
                if (i == mtaskerids.length - 1) {
                    innermtaskeridsString += mtaskerids[i].toString();
                }
            }
            task.setMtasker(innermtaskeridsString);
        } else {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, "主办单位id不能为空");
        }


//        协办单位id
        Integer[] staskerids = submitTaskRequestVo.getForm().getStaskerid();
        for (Integer deptId : staskerids) {

            Dept dept = deptMapper.selectById(deptId);
//            检查对应部门是否存在
            if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
                return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到根据id：" + deptId + "对应协办单位");
            }
//        检查部门下属人员是否存在
            LambdaQueryWrapper<Users> deptUserWrapper = new LambdaQueryWrapper<>();
            deptUserWrapper.eq(Users::getDeptId, deptId);
            if (usersMapper.selectList(deptUserWrapper).size() == 0) {
                return ResponseResult.errorResult(ErrorCode.DEPT_NO_CONTAIN_USER);
            }
        }
        if (staskerids != null && staskerids.length > 0) {
            String innerstaskeridsString = "";
            for (int i = 0; i < staskerids.length; i++) {
//                如果不是最后一次，则添加“，”
                if (i != staskerids.length - 1) {
                    innerstaskeridsString += staskerids[i].toString() + ",";
                }
//                如果是最后一次，不添加“，”即可
                if (i == staskerids.length - 1) {
                    innerstaskeridsString += staskerids[i].toString();
                }
            }
            task.setStasker(innerstaskeridsString);
        } else {
            task.setStasker("");
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

//        附件上传
        if (submitTaskRequestVo.getForm().getAttachment() != null) {
            List<AttachmentVo> attachmentVos = submitTaskRequestVo.getForm().getAttachment();
            task.setAttachment(JSON.toJSONString(attachmentVos));
        }


//        设置事项状态为1，督办人员已提交
        if (users.getRoleId().equals(UserRoleConstant.SUPERVISOR)) {
            task.setState(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
        } else if (users.getRoleId().equals(UserRoleConstant.SUPERINTENDENT)) {
            System.out.println("       \n\n\n" + "提交任务---督办主任审核");
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.eq(Task::getTaskNo, submitTaskRequestVo.getForm().getTaskNo());

            List<Task> tasks = taskMapper.selectList(taskLambdaQueryWrapper);
            task.setState((TaskConstant.AUDIT_MATTERS));
            if (tasks.size() >= 1) {
                taskMapper.deleteById(tasks.get(0).getId());
            }
            //        更新数据
            task.setApproveBy(users.getId());
            task.setApproverName(users.getUsername());
            task.setApproveTime(new Date());

        }

        taskMapper.insert(task);

        if (users.getRoleId().equals(UserRoleConstant.SUPERINTENDENT)) {
            //        获取督查单位主任
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();

//        向流程表中插入数据
//        主办单位
            Integer[] mtaskersId = TaskUtils.convertToIntArray(task.getMtasker());

            for (Integer id : mtaskersId) {
//        获取对象主办单位执行人
                usersWrapper.clear();
                usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
                Users users1 = usersMapper.selectOne(usersWrapper);
                if (users1 == null) {
                    return ResponseResult.errorResult(ErrorCode.DATA_NULL, "主办单位执行人为空");
                }
                Flow flow = new Flow();
                flow.setTaskId(task.getId());
                flow.setDeptId(id);
                flow.setExcuter(users1.getId());
                flow.setType(DeptType.ORGANIZER_DEPT);
                flow.setStatus(TaskConstant.AUDIT_MATTERS);
                flowMapper.insert(flow);
            }
//        协办单位
            Integer[] staskersId;
            if (!task.getStasker().equals("")) {
                staskersId = TaskUtils.convertToIntArray(task.getStasker());
            } else {
                staskersId = new Integer[0];
            }


            for (Integer id : staskersId) {
//        获取协办单位执行人
                usersWrapper.clear();
                usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
                Users users1 = usersMapper.selectOne(usersWrapper);
                if (users1 == null) {
                    return ResponseResult.errorResult(ErrorCode.DATA_NULL, "协办单位执行人为空");
                }
                Flow flow = new Flow();
                flow.setTaskId(task.getId());
                flow.setDeptId(id);
                flow.setExcuter(users1.getId());
                flow.setType(DeptType.CO_ORGANIZER_DEPT);
                flow.setStatus(TaskConstant.AUDIT_MATTERS);
                flowMapper.insert(flow);
            }
        }

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
            JSONObject jsonObject = new JSONObject();

            if (task.getState().equals(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED)) {
                jsonObject.put("督查室", "1");
            }

            for (Flow flow : flows) {

                Dept dept = deptMapper.selectById(flow.getDeptId());
                if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
                    continue;
                }
                jsonObject.put(dept.getDeptName(), flow.getStatus());

            }


//            设置状态
            taskResponseVo.setStatus(jsonObject);

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
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "创建者用户为空");
        }

//主办单位可查询的状态
        List<String> sponsorDirectorStatusList = Arrays.asList("2", "3", "4", "5", "6", "7", "8", "9");
//        定义封装结果Vo类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();
//        先获取该部门对应的事项id
        LambdaQueryWrapper<Flow> flowWrapperToFindTask = new LambdaQueryWrapper<>();
        flowWrapperToFindTask.eq(Flow::getDeptId, users.getDeptId())
                .ne(Flow::getStatus, TaskConstant.TASK_EXECUTED);
        List<Flow> flowsToFindTask = flowMapper.selectList(flowWrapperToFindTask);
        Integer[] taskIds = flowsToFindTask.stream()
                .map(Flow::getTaskId)
                .toArray(Integer[]::new);


        if (taskIds.length == 0) {
            return ResponseResult.okResult();
        }

//        查询对应事项
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getId, taskIds)
                .and(wrapper -> wrapper.in(Task::getState, sponsorDirectorStatusList));
//          获取查询结果
        List<Task> taskData = taskMapper.selectList(taskWrapper);
//          封装Vo类
        for (Task task : taskData) {
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.eq(Flow::getDeptId, users.getDeptId())
                    .eq(Flow::getTaskId, task.getId());
//            设置所有状态
            taskResponseVo.setStatus(flowMapper.selectOne(flowLambdaQueryWrapper).getStatus());

            taskVoData.add(taskResponseVo);
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskContentNormalOneStatusVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);
    }


    /**
     * @Description: 查看或编辑事项时获取事项内容进行展示---根据taskId获取事项内容
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

        taskContentVo.setStatus(getAllTaskStatue(taskId));

//        设置协办单位名称数组(List<String>) 与 id数组
        taskContentVo.setStasker(getTaskerName(task.getStasker()));
        if (!task.getStasker().equals("")) {
            taskContentVo.setStaskerid(TaskUtils.convertToIntArray(task.getStasker()));
        } else {
            taskContentVo.setStaskerid(new Integer[0]);
        }

//        设置主办单位名称 与 id数组
        taskContentVo.setMtasker(getTaskerName(task.getMtasker()));
        if (!task.getMtasker().equals("")) {
            taskContentVo.setMtaskerid(TaskUtils.convertToIntArray(task.getMtasker()));
        } else {
            taskContentVo.setMtaskerid(new Integer[0]);
        }

//        设置附件
        List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
        taskContentVo.setAttachment(attachmentVos);


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
        fileVo.setName(file.getOriginalFilename());
        fileVo.setUid(returnName.replace(file.getOriginalFilename(), ""));

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
            taskVo.setStasker(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMtasker(getTaskerName(task.getMtasker()));
//          设置合并状态
            taskVo.setTaskStatus(getFirstStatus(taskVo.getTaskId()));
//            设置状态
            taskVo.setStatus(getAllTaskStatue(taskVo.getTaskId()));
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

        List<String> statueList = Arrays.asList(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED, TaskConstant.AUDIT_MATTERS, TaskConstant.TASK_ALLOCATED, TaskConstant.TASK_EXECUTED, TaskConstant.UNIT_LEADER_SUBMITTED, TaskConstant.TASK_ASSIGNED);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.in(Flow::getStatus, statueList);
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);


        LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskLambdaQueryWrapper.eq(Task::getState, TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED);
        for (Task task : taskMapper.selectList(taskLambdaQueryWrapper)) {
            Flow flow = new Flow();
            flow.setTaskId(task.getId());
            flows.add(flow);
        }

        for (Flow flow : flows) {

//            判断是否已经重复
            boolean judgeIsRepeat = false;
            for (TaskContentNoFlowVo taskContent : taskVos) {
                if (taskContent.getTaskId() == flow.getTaskId()) {
                    judgeIsRepeat = true;
                }
            }
            if (judgeIsRepeat) {
                continue;
            }

            Task task = taskMapper.selectById(flow.getTaskId());
            TaskContentNoFlowVo taskVo = BeanCopyUtils.copyBean(task, TaskContentNoFlowVo.class);

//        定义事项id
            taskVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>)
            taskVo.setStasker(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMtasker(getTaskerName(task.getMtasker()));
//          设置合并状态
            taskVo.setTaskStatus(getFirstStatus(taskVo.getTaskId()));


            JSONObject taskStatus = getAllTaskStatue(taskVo.getTaskId());

            if (task.getState().equals(TaskConstant.SUPERVISING_PERSONNEL_SUBMITTED)) {
                taskStatus.put("督查室", "1");
            }
            for (Object value : taskStatus.values()) {
                if (!statueList.contains(value)) {
                    // 不满足条件的逻辑处理
                    judgeIsRepeat = true;
                }
            }

            if (judgeIsRepeat) {
                continue;
            }

//            设置状态
            taskVo.setStatus(taskStatus);

//        设置附件
            List<AttachmentVo> attachmentVos = JSON.parseArray(task.getAttachment(), AttachmentVo.class);
            taskVo.setAttachment(attachmentVos);
            taskVos.add(taskVo);
        }

        taskVos.sort(Comparator.comparing(TaskContentNoFlowVo::getStartTime).reversed());

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

        List<Flow> distinctFlows = flows.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Flow::getTaskId))),
                        ArrayList::new
                ));


        for (Flow flow : distinctFlows) {

            Task task = taskMapper.selectById(flow.getTaskId());
            ArchiveIdTaskResponseVo taskVo = BeanCopyUtils.copyBean(task, ArchiveIdTaskResponseVo.class);

//        设置协办单位名称数组(List<String>)
            taskVo.setStasker(getTaskerName(task.getStasker()));

//        设置主办单位名称
            taskVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置完结日期
            taskVo.setCompleteDate(task.getCompleteTime());
//            设置id
            taskVo.setTaskId(task.getId());

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
        if (str == null || str.equals("")) {
            List<String> test = new ArrayList<>();
            test.add("");
            return test;
        }

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

//        如果日期不为空，则带上日期参加进行查询。
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

        Map<Integer, List<BigDecimal>> deptScore = new HashMap<>();

//        往积分结果集合中添加所有未删除部门
        LambdaQueryWrapper<Dept> deptWrapper = new LambdaQueryWrapper<>();
        deptWrapper.eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
        List<Dept> depts = deptMapper.selectList(deptWrapper);
        for (Dept dept : depts) {
            List<BigDecimal> monthScores = new ArrayList<>(Collections.nCopies(12, BigDecimal.valueOf(100)));//            每个部门初始分数为100
            deptScore.put(dept.getId(), monthScores);
        }

//        按照流程进行扣分，在对应月份的分数里进行
        //  时间转换，返回时间对应月份
        Calendar calendar = Calendar.getInstance();
        for (Flowdetail flowdetail : flowdetails) {

//            获取对应流程信息
            Flow flow = flowMapper.selectById(flowdetail.getFlowId());

//            如果为中止事项则跳出
            if (flow.getStatus().equals(TaskConstant.TASK_ARCHIVED)) {
                continue;
            }

            if (deptScore.containsKey(flow.getDeptId())) {
//                获取每个流程的扣分
                BigDecimal deductionScore = BigDecimal.valueOf(flowdetail.getScore());
//                获取对应执行时间月份(0~11)，比正常月份少1，方便数组加减
                calendar.setTime(flowdetail.getExcuteTime());
                int month = calendar.get(Calendar.MONTH);
//                修改数据
                List<BigDecimal> monthScores = deptScore.get(flow.getDeptId());
                BigDecimal currentScore = monthScores.get(month);
                BigDecimal updatedScore = currentScore.add(deductionScore);
                monthScores.set(month, updatedScore);

//                更新部门分数
                deptScore.put(flow.getDeptId(), monthScores);
            }

        }

//        移除督查室积分
        LambdaQueryWrapper<Dept> deptLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deptLambdaQueryWrapper.eq(Dept::getDeptName, "督查室").eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
        Dept deptDuCha = deptMapper.selectOne(deptLambdaQueryWrapper);


// 遍历deptScores属性
        for (Map.Entry<Integer, List<BigDecimal>> entry : deptScore.entrySet()) {

            int deptId = entry.getKey();

            if (deptDuCha != null) {
                if (deptDuCha.getId() == deptId) {
                    continue;
                }
            }

//            计算平均值
            BigDecimal average = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()));

            Double score = average.doubleValue();

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
        if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "没找到deptId对应的部门");
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
//        如果为空返回空数据
        if (flows.isEmpty()) {
            return ResponseResult.okResult();
        }
//       获取 flows 中所有流程的 id 列表
        List<Integer> flowIds = flows.stream().map(Flow::getId).collect(Collectors.toList());
//      在 flowdetailWrapper 中使用 in 方法
        flowdetailWrapper.in(Flowdetail::getFlowId, flowIds);
//        获取筛选出来的结果
        List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailWrapper);

        for (Flowdetail flowdetail : flowdetails) {
//            如果是扣分事项才会返回对应数据
            if (flowdetail.getScore() != 0.0) {
                Flow flow = flowMapper.selectById(flowdetail.getFlowId());

//                判断不是已中止事项
                if (flow.getStatus().equals(TaskConstant.TASK_ARCHIVED)) {
                    continue;
                }

                Task task = taskMapper.selectById(flow.getTaskId());

                DeptScoreDetailVo deptScoreDetailVo = new DeptScoreDetailVo();

                deptScoreDetailVo = BeanCopyUtils.copyBean(task, DeptScoreDetailVo.class);
                deptScoreDetailVo.setTaskId(task.getId());
                deptScoreDetailVo.setStatus(flow.getStatus());
                deptScoreDetailVo.setMtasker(getTaskerName(task.getMtasker()));
                deptScoreDetailVo.setStasker(getTaskerName(task.getStasker()));
                deptScoreDetailVo.setTaskScore(flowdetail.getScore());

                deptScoretVos.add(deptScoreDetailVo);
            }
        }


//        合并积分
        Map<Integer, DeptScoreDetailVo> uniqueTasks = new HashMap<>();

        for (DeptScoreDetailVo vo : deptScoretVos) {
            Integer taskId = vo.getTaskId();
            Double taskScore = vo.getTaskScore();

            if (uniqueTasks.containsKey(taskId)) {
                // 重复的 taskId，进行累加操作
                DeptScoreDetailVo existingVo = uniqueTasks.get(taskId);
                Double existingTaskScore = existingVo.getTaskScore();
                existingVo.setTaskScore(existingTaskScore + taskScore);
            } else {
                // 不重复的 taskId，将其添加到 uniqueTasks 中
                uniqueTasks.put(taskId, vo);
            }
        }

// 从 uniqueTasks 中获取处理后的结果列表
        List<DeptScoreDetailVo> result = new ArrayList<>(uniqueTasks.values());


        return ResponseResult.okResult(result);
    }

    /**
     * @Description: 根据id获取事项内容
     * @Date: 2024/1/23
     * @Param taskId:
     **/
    @Override
    public ResponseResult getTaskResultById(Integer taskId) {

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到taskId对应的实体类数据");
        }

        Integer[] strakeIds = TaskUtils.convertToIntArray(task.getStasker());
        List<Integer> taskIdList = Arrays.asList(strakeIds);

        if (taskIdList.size() == 0) {
            return ResponseResult.okResult(new ArrayList<>());
        }


        List<DeptTaskResponseVo> deptTaskResponseVos = new ArrayList<>();

        List<Flow> flows = new ArrayList<>();
        if (taskIdList.size() == 1) {
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getDeptId, taskIdList.get(0)).eq(Flow::getTaskId, taskId);
            flows.addAll(flowMapper.selectList(flowWrapper));
        } else {
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.in(Flow::getDeptId, taskIdList).eq(Flow::getTaskId, taskId);
            flows.addAll(flowMapper.selectList(flowWrapper));
        }

        for (Flow flow : flows) {
            Dept dept = deptMapper.selectById(flow.getDeptId());
            DeptTaskResponseVo deptTaskResponseVo = new DeptTaskResponseVo();
            deptTaskResponseVo.setDeptName(dept.getDeptName());
            deptTaskResponseVo.setResult(TaskUtils.generateStatusStringForSupervisor(flow.getStatus()));
            deptTaskResponseVos.add(deptTaskResponseVo);
        }


        return ResponseResult.okResult(deptTaskResponseVos);
    }


    @Override
    public ResponseResult approveTaskById(Integer taskId) {

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL, "未找到该id对应的事项");
        }

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
                return ResponseResult.errorResult(ErrorCode.DATA_NULL, "主办单位执行人为空");
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
        Integer[] staskersId;
        if (!task.getStasker().equals("")) {
            staskersId = TaskUtils.convertToIntArray(task.getStasker());
        } else {
            staskersId = new Integer[0];
        }


        for (Integer id : staskersId) {
//        获取协办单位执行人
            usersWrapper.clear();
            usersWrapper.eq(Users::getDeptId, id).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users users = usersMapper.selectOne(usersWrapper);
            if (users == null) {
                return ResponseResult.errorResult(ErrorCode.DATA_NULL, "协办单位执行人为空");
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

        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult approveResultById(Long taskId) {

//        检测taskId下所有单位审核是否已经通过
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);

//        获取督办主任用户
        LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        usersLambdaQueryWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT)
                .eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users managerUser = usersMapper.selectOne(usersLambdaQueryWrapper);


        for (Flow flow : flows) {
            if (!flow.getStatus().equals(TaskConstant.UNIT_LEADER_SUBMITTED) && !flow.getStatus().equals(TaskConstant.PASS_EXAMINATION)) {
                return ResponseResult.errorResult(ErrorCode.STATUE_NOT_ALL);
            }
        }


//        将流程信息归档
        for (Flow flow : flows) {
//            如果此时事件状态为8，先改为状态10进行审核确认
            if (flow.getStatus().equals(TaskConstant.UNIT_LEADER_SUBMITTED)) {
                flow.setStatus(TaskConstant.PASS_EXAMINATION);
                flow.setExcuter(null);
                flowMapper.updateById(flow);
                Flowdetail flowDetail = new Flowdetail();
                flowDetail.setExcuter(managerUser.getId());
                flowDetail.setFlowId(flow.getId());
                flowDetail.setAction(TaskActionConstant.FEEDBACK_RESULT_PASS);
                flowDetail.setExcuteTime(new Date());
                flowDetail.setExcutername(managerUser.getNickname());
                flowDetail.setScore(0.0);
                // 插入flowDetail记录
                flowdetailMapper.insert(flowDetail);
            }
        }


//        如果所有事项状态都为10，将事项归档
        if (flows.stream()
                .allMatch(flow -> flow.getStatus().equals(TaskConstant.PASS_EXAMINATION))) {
            for (Flow flow : flows) {
                if (flow.getExcuter() != null) {
                    flow.setExcuter(null);
                    flowMapper.updateById(flow);
                }
                Flowdetail flowDetail = new Flowdetail();
                flowDetail.setExcuter(null);
                flowDetail.setFlowId(flow.getId());
                flowDetail.setAction(TaskActionConstant.ARCHIVED);
                flowDetail.setExcuteTime(new Date());
                flowDetail.setExcutername(null);
                flowDetail.setScore(0.0);
                // 插入flowDetail记录
                flowdetailMapper.insert(flowDetail);
            }

            //        获取并更新事项实体类数据
            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                return ResponseResult.errorResult(ErrorCode.DATA_NULL);
            }
            task.setState(TaskConstant.PASS_EXAMINATION);
            task.setCompleteTime(new Date());
            taskMapper.updateById(task);
        }


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

        try {
            if (!submitRejectTaskVo.getForm().getStartTime().equals("")) {
                task.setStartTime(formatter.parse(submitRejectTaskVo.getForm().getStartTime()));
            }
            if (!submitRejectTaskVo.getForm().getEndTime().equals("")) {
                task.setEndTime(formatter.parse(submitRejectTaskVo.getForm().getEndTime()));
            }
            if (!submitRejectTaskVo.getForm().getAttachment().isEmpty()) {
                task.setAttachment(JSON.toJSONString(submitRejectTaskVo.getForm().getAttachment()));
            }
        } catch (ParseException e) {
            return ResponseResult.errorResult(ErrorCode.REQUEST_BODY_ERROR, "时间转换错误");
        }

//        设置task表的状态为9
        task.setState(TaskConstant.FEEDBACK_REJECTION);
//        获取反馈通过与未通过的数据单位id
        List<Integer> passId = new ArrayList<>();
        passId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getMtasker())));
        passId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getStasker())));
        passId.removeAll(Arrays.asList(submitRejectTaskVo.getForm().getMtaskerid()));
        passId.removeAll(Arrays.asList(submitRejectTaskVo.getForm().getStaskerid()));
        List<Integer> noPassId = new ArrayList<>();
        noPassId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getMtasker())));
        noPassId.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getStasker())));
        noPassId.removeAll(passId);

//        如果没有不通过的单位，默认为归档事项
        if (noPassId.size() == 0) {
            return approveResultById(Long.valueOf(task.getId()));
        }


//        处理审核通过的事项
        for (Integer id : passId) {
            LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
            flowWrapper.eq(Flow::getTaskId, task.getId()).eq(Flow::getDeptId, id);
            Flow flow = flowMapper.selectOne(flowWrapper);
            flow.setStatus(TaskConstant.PASS_EXAMINATION);
//          执行人置空
            flow.setExcuter(null);
            flowMapper.updateById(flow);
//            flowdetail表中插入一条督办主任审核通过的步骤
            Flowdetail flowdetail = new Flowdetail();
//        获取督查单位主任
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
            usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);
            flowdetail.setExcuter(usersSuperIntendent.getId());
            flowdetail.setExcutername(usersSuperIntendent.getNickname());
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

            LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
            usersLambdaQueryWrapper.eq(Users::getState, UserStateConstant.USER_NORMAL)
                    .eq(Users::getDeptId, flow.getDeptId())
                    .eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF);
            Users userWorker = usersMapper.selectOne(usersLambdaQueryWrapper);
            flow.setExcuter(userWorker.getId());
            flowMapper.updateById(flow);
//            flowdetail表中插入一条督办主任审核不通过的步骤
            Flowdetail flowdetail = new Flowdetail();
//        获取督查单位主任
            LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
            usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);
            flowdetail.setExcuter(usersSuperIntendent.getId());
            flowdetail.setExcutername(usersSuperIntendent.getNickname());
            flowdetail.setExcuteTime(new Date());
            flowdetail.setFlowId(flow.getId());
            flowdetail.setAction(TaskActionConstant.HOSTING_UNIT_LEADER_REJECTED);

//            查询本年该部门反馈驳回有几次
            LocalDate now = LocalDate.now();
            LocalDate startOfYear = LocalDate.of(now.getYear(), Month.JANUARY, 1);
            LocalDate endOfYear = LocalDate.of(now.getYear(), Month.DECEMBER, Month.DECEMBER.maxLength());

            //        获取该部门所有的流程id
            List<Integer> allFlowIds = new ArrayList<>();
            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.eq(Flow::getDeptId, id);
            List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);
            for (Flow f : flows) {
                allFlowIds.add(f.getId());
            }

            // 构建查询条件
            LambdaQueryWrapper<Flowdetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Flowdetail::getFlowId, allFlowIds)
                    .eq(Flowdetail::getAction, TaskActionConstant.HOSTING_UNIT_LEADER_REJECTED)
                    .ge(Flowdetail::getExcuteTime, startOfYear.atStartOfDay())
                    .lt(Flowdetail::getExcuteTime, endOfYear.plusDays(1).atStartOfDay());
            List<Flowdetail> flowdetails = flowdetailMapper.selectList(wrapper);

//            进行扣分
            int occurrences = flowdetails.size();
            double score = 0;

            if (occurrences == 0) {
                score = 0; // 没有逾期，得分为0
            } else if (occurrences == 1) {
                score = -0.5; // 第1次扣0.5分
            } else if (occurrences == 2) {
                score = -1; // 第2次扣1分
            } else if (occurrences == 3) {
                score = -2; // 第3次扣2分
            } else {
                score = -5; // 从第4次开始每次扣5分
            }

// 如果累计发生10次以上，第11次开始每次额外扣5分
            if (occurrences > 10) {
                score -= 10;
            }


//          设置事项驳回时间
            task.setRejectTime(new Date());
            flowdetailMapper.insert(flowdetail);
        }

//          更新事项数据
        Task newtask = BeanCopyUtils.copyBean(submitRejectTaskVo.getForm(), Task.class);
        newtask.setId(task.getId());
        taskMapper.updateById(newtask);
        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult suspentTaskById(Integer taskId) {

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }
//      如果事项状态不在“2”-“7”之间，抛异常
        if (!(task.getState().compareTo(TaskConstant.AUDIT_MATTERS) >= 0
                && task.getState().compareTo(TaskConstant.FEEDBACK_OVERDUE) <= 0)) {
            return ResponseResult.errorResult(ErrorCode.STATUE_ERROR, "事项状态应在2-7之间");
        }

        LambdaQueryWrapper<Users> usersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        usersLambdaQueryWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT)
                .eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users duchaUser = usersMapper.selectOne(usersLambdaQueryWrapper);

        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getTaskId, task.getId());
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);

        for (Flow flow : flows) {
            flow.setStatus(TaskConstant.TASK_ARCHIVED);
            flowMapper.updateById(flow);
            Flowdetail flowDetail = new Flowdetail();
            flowDetail.setExcuter(duchaUser.getId());
            flowDetail.setFlowId(flow.getId());
            flowDetail.setAction(TaskActionConstant.SUSPENDED);
            flowDetail.setExcuteTime(new Date());
            flowDetail.setExcutername(duchaUser.getNickname());
            flowDetail.setScore(0.0);
            flowdetailMapper.insert(flowDetail);
        }

        task.setState(TaskConstant.TASK_ARCHIVED);
        taskMapper.updateById(task);
        return ResponseResult.okResult();
    }


    //    获取状态为5，部门为办事单位该部门的实现
    @Override
    public ResponseResult getUnapprovedTasksByUserId(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        定义封装结果Vo类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();
//        先获取该部门对应的事项id
        LambdaQueryWrapper<Flow> flowWrapperToFindTask = new LambdaQueryWrapper<>();
        flowWrapperToFindTask.eq(Flow::getDeptId, users.getDeptId())
                .eq(Flow::getStatus, TaskConstant.TASK_ALLOCATED);
        List<Flow> flowsToFindTask = flowMapper.selectList(flowWrapperToFindTask);

        for (Flow flow : flowsToFindTask) {

//        查询对应事项
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(Task::getId, flow.getTaskId());
//          获取查询结果
            Task task = taskMapper.selectOne(taskWrapper);
//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskContentNormalOneStatusVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult approveFeedback(ApproveFeedbackVo approveFeedbackVo) {

//        获取事项数据
        Integer taskId = approveFeedbackVo.getTaskId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
        }

//        获取用户数据
        Integer userId = approveFeedbackVo.getUserId();
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        角色判断
        if (!users.getRoleId().equals(UserRoleConstant.OFFICE_DIRECTOR)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户角色不是办事单位主任");
        }


        String option = approveFeedbackVo.getOption();


//      先获取事项对应的流程
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId).eq(Flow::getDeptId, users.getDeptId());
        Flow flow = flowMapper.selectOne(flowWrapper);

//        获取督办主任
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);

//        获取审批结果
        String result = approveFeedbackVo.getResult();
//        "0"为通过
        if (result.equals("0")) {
//            更新审批成功的数据
            flow.setExcuter(usersSuperIntendent.getId());
            flow.setStatus(TaskConstant.UNIT_LEADER_SUBMITTED);
            flowMapper.updateById(flow);
//            插入flowdetail数据
            Flowdetail flowdetail = new Flowdetail();
            flowdetail.setExcuter(users.getId());
            flowdetail.setExcutername(users.getNickname());
            flowdetail.setFlowId(flow.getId());
            flowdetail.setAction(TaskActionConstant.HOSTING_UNIT_LEADER_SUBMITTED);
            flowdetail.setExcuteTime(new Date());
            flowdetail.setNote(option);
            flowdetailMapper.insert(flowdetail);
//            判断当前事项是否所有主协办单位状态都是8
            List<Integer> allDeptIdsFromTask = new ArrayList<>();
            allDeptIdsFromTask.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getMtasker())));
            allDeptIdsFromTask.addAll(Arrays.asList(TaskUtils.convertToIntArray(task.getStasker())));
//            查询这些单位该任务状态不为8的数量
            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.in(Flow::getDeptId, allDeptIdsFromTask)
                    .and(wrapper -> wrapper.eq(Flow::getTaskId, taskId)
                            .ne(Flow::getStatus, TaskConstant.UNIT_LEADER_SUBMITTED));
//            如果是，修改事项状态为8
            if (flowMapper.selectList(flowLambdaQueryWrapper).isEmpty()) {
                task.setState(TaskConstant.UNIT_LEADER_SUBMITTED);
                taskMapper.updateById(task);
            }
        }

//        如果为"1"，为不通过
        if (result.equals("1")) {
            flow.setStatus(TaskConstant.TASK_EXECUTED);
            flowMapper.updateById(flow);

//            获取对应办事单位执行人
            usersWrapper.clear();
            usersWrapper.eq(Users::getDeptId, users.getDeptId()).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
            Users executorUser = usersMapper.selectOne(usersWrapper);
            if (executorUser == null) {
                return ResponseResult.errorResult(ErrorCode.DATA_NULL, "主办单位执行人为空");
            }
//            插入flowdetail数据
            Flowdetail flowdetail = new Flowdetail();
            flowdetail.setExcuter(users.getId());
            flowdetail.setExcutername(users.getNickname());
            flowdetail.setFlowId(flow.getId());
            flowdetail.setAction(TaskActionConstant.HOSTING_UNIT_LEADER_REJECTED);
            flowdetail.setExcuteTime(new Date());
            flowdetail.setNote(option);
            flowdetailMapper.insert(flowdetail);
        }

        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult getApprovedTasksByUserId(Integer userId) {

        //        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询该部门flow状态为“6”和“8”
        List<String> arrayCheckFlowStatus = Arrays.asList(TaskConstant.TASK_EXECUTED, TaskConstant.UNIT_LEADER_SUBMITTED);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getDeptId, users.getDeptId())
                .and(wrapper -> wrapper.in(Flow::getStatus, arrayCheckFlowStatus));
        List<Flow> flowsToFindTask = flowMapper.selectList(flowLambdaQueryWrapper);

//        定义VO封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flowsToFindTask) {

            List<String> supervisingUnitStatusList = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getState, supervisingUnitStatusList)
                    .and(wrapper -> wrapper.eq(Task::getId, flow.getTaskId())).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

//          获取查询结果
            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);
//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);
        }

//        按创建时间排序，由近到远
        taskVoData.sort(Comparator.comparing(TaskContentNormalOneStatusVo::getStartTime).reversed());

//        返回Vo封装类
        return ResponseResult.okResult(taskVoData);
    }


    //    获取逾期或者上报被驳回的事项"3" "7" "9"
    @Override
    public ResponseResult getDeductedTasks(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询状态为3 7 9  且部门为自己部门的flow
        String[] checkStatues = {"3", "7", "9"};
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.in(Flow::getStatus, checkStatues)
                .and(wrapper -> wrapper.eq(Flow::getDeptId, users.getDeptId()));
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        if (flows.isEmpty()) {
            return ResponseResult.okResult();
        }

//        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);
    }


    @Override
    public ResponseResult getUnclaimedTasks(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询状态为2 9  且部门为自己部门的flow
        String[] checkStatues = {"2", "9"};
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.in(Flow::getStatus, checkStatues)
                .and(wrapper -> wrapper.eq(Flow::getExcuter, userId));
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        if (flows.size() == 0) {
            return ResponseResult.okResult(new ArrayList<>());
        }

//        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult claimTask(Integer taskId, Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        if (!users.getRoleId().equals(UserRoleConstant.OFFICE_STAFF)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户身份不是办事单位执行人");
        }

//        获取事项数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
        }
//      获取对应flow
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getExcuter, userId).eq(Flow::getTaskId, taskId);
        Flow flow = flowMapper.selectOne(flowLambdaQueryWrapper);
        if (flow == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到执行人要领取的事项");
        }

        flow.setStatus(TaskConstant.TASK_ASSIGNED);
        flowMapper.updateById(flow);

        Flowdetail flowdetail = new Flowdetail();
        flowdetail.setExcuter(userId);
        flowdetail.setExcutername(users.getNickname());
        flowdetail.setFlowId(flow.getId());
        flowdetail.setAction(TaskActionConstant.ASSIGNED);
        flowdetail.setExcuteTime(new Date());
        flowdetailMapper.insert(flowdetail);
        return ResponseResult.okResult();
    }


    //    状态4 未反馈  和 状态7  反馈逾期
    @Override
    public ResponseResult getUnfeedbackTasks(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        List<Flow> flows = new ArrayList<>();

        LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowdetailLambdaQueryWrapper.eq(Flowdetail::getAction, TaskActionConstant.ASSIGNED)
                .eq(Flowdetail::getExcuter, userId).orderByDesc(Flowdetail::getExcuteTime);
        List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

//        查询为状态4 和 7 的流程
        for (Flowdetail flowdetail : flowdetails) {
            Flow flow = flowMapper.selectById(flowdetail.getFlowId());
            if (flow.getExcuter() == userId &&
                    (flow.getStatus().equals(TaskConstant.TASK_ASSIGNED) ||
                            flow.getStatus().equals(TaskConstant.FEEDBACK_OVERDUE))
                    && !flow.getStatus().equals(TaskConstant.PASS_EXAMINATION)
                    && !flow.getStatus().equals(TaskConstant.TASK_ARCHIVED)) {
                flows.add(flow);
            }
        }

        if (flows.isEmpty()) {
            return ResponseResult.okResult(new ArrayList<>());
        }

//        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

//        查询出来的会有驳回和已领办记录，导致事项id重复，这里做一个去重。
        flows = flows.stream()
                .collect(Collectors.toMap(Flow::getTaskId,
                        flow -> flow,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .collect(Collectors.toList());


        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult submitResult(SubmitResultVO submitResultVO) {

        Integer userId = submitResultVO.getUserId();
        Integer taskId = submitResultVO.getTaskId();
        String result = submitResultVO.getResult();

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        if (!users.getRoleId().equals(UserRoleConstant.OFFICE_STAFF)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户角色不是办事单位执行人");
        }

//        获取事项数据
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
        }

//      获取对应flow
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getExcuter, userId).eq(Flow::getTaskId, taskId);
        Flow flow = flowMapper.selectOne(flowLambdaQueryWrapper);
        if (flow == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到执行人要领取的事项");
        }

//        新增附件
        if (submitResultVO.getFiles() != null && submitResultVO.getFiles().size() != 0) {
            List<AttachmentVo> attachmentVos = (List<AttachmentVo>) JSON.parse(task.getAttachment());
            attachmentVos.addAll(submitResultVO.getFiles());
            task.setAttachment(JSON.toJSONString(attachmentVos));
            taskMapper.updateById(task);
        }


//        获取督办主任
        LambdaQueryWrapper<Users> usersWrapper = new LambdaQueryWrapper<>();
        usersWrapper.eq(Users::getRoleId, UserRoleConstant.SUPERINTENDENT).eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users usersSuperIntendent = usersMapper.selectOne(usersWrapper);

        flow.setStatus(TaskConstant.UNIT_LEADER_SUBMITTED);
        flow.setExcuter(usersSuperIntendent.getId());
        flowMapper.updateById(flow);

        Flowdetail flowdetail = new Flowdetail();
        usersWrapper.clear();
        usersWrapper.eq(Users::getDeptId, users.getDeptId()).eq(Users::getRoleId, UserRoleConstant.OFFICE_STAFF).eq(Users::getState, UserStateConstant.USER_NORMAL);
        Users usersSuper = usersMapper.selectOne(usersWrapper);
        if (usersSuper == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL, "主办单位执行人为空");
        }
        flowdetail.setExcuter(usersSuper.getId());
        flowdetail.setExcutername(usersSuper.getNickname());
        flowdetail.setFlowId(flow.getId());
        flowdetail.setNote(result);
        flowdetail.setAction(TaskActionConstant.ALLOCATED);
        flowdetail.setExcuteTime(new Date());
        flowdetailMapper.insert(flowdetail);
        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult getSubmittedTasksByUserId(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询状态为8  且部门为自己部门的flow
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.in(Flow::getStatus, TaskConstant.UNIT_LEADER_SUBMITTED)
                .and(wrapper -> wrapper.eq(Flow::getDeptId, users.getDeptId()));
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        if (flows.isEmpty()) {
            return ResponseResult.okResult(new ArrayList<>());
        }

        //        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED)).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult getOverdueTasksByUserId(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询状态为3 7  且部门为自己部门的flow
        String[] checkStatues = {TaskConstant.OVERDUE_APPLICATION, TaskConstant.FEEDBACK_OVERDUE};
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.in(Flow::getStatus, checkStatues)
                .and(wrapper -> wrapper.eq(Flow::getExcuter, userId));
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        if (flows.isEmpty()) {
            return ResponseResult.okResult(new ArrayList<>());
        }

//        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));


            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult getRejectedTasksByUserId(Integer userId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

//        查询状态为6 且执行人为自己
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getStatus, TaskConstant.TASK_EXECUTED)
                .and(wrapper -> wrapper.eq(Flow::getExcuter, userId));
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        if (flows.isEmpty()) {
            return ResponseResult.okResult();
        }

        //        定义task封装类
        List<TaskContentNormalOneStatusVo> taskVoData = new ArrayList<>();

        for (Flow flow : flows) {

//        查询对应task
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getId, flow.getTaskId()).ne(Task::getState, TaskConstant.PASS_EXAMINATION)
                    .and(wrapper -> wrapper.ne(Task::getState, TaskConstant.TASK_ARCHIVED));

            Task task = taskMapper.selectOne(taskLambdaQueryWrapper);

//          封装Vo类
            TaskContentNormalOneStatusVo taskResponseVo = new TaskContentNormalOneStatusVo();
            taskResponseVo = BeanCopyUtils.copyBean(task, taskResponseVo.getClass());
            taskResponseVo.setTaskId(task.getId());
            taskResponseVo.setStasker(getTaskerName(task.getStasker()));
            taskResponseVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置合并状态
            taskResponseVo.setTaskStatus(getFirstStatus(task.getId()));
//            设置所有状态
            taskResponseVo.setStatus(flow.getStatus());

            taskVoData.add(taskResponseVo);

        }

        return ResponseResult.okResult(taskVoData);

    }

    @Override
    public ResponseResult getTaskFlowByUserId(Integer userId, Integer taskId) {

//        获取用户数据
        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        List<FlowVo> flowVos = new ArrayList<>();

//        如果为督查室单位
        if (users.getRoleId().equals(UserRoleConstant.SUPERINTENDENT) ||
                users.getRoleId().equals(UserRoleConstant.SUPERVISOR)) {
            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.eq(Flow::getTaskId, taskId);
            List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);

//                只保留一个办结销号的
            FlowVo completedFlowVo = null;

            for (Flow flow : flows) {
                LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
                flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flow.getId());
                List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

                String deptName = deptMapper.selectById(flow.getDeptId()).getDeptName();


                for (Flowdetail flowdetail : flowdetails) {
                    FlowVo flowVo = new FlowVo();

                    flowVo.setAction(getFlowdtailAction(flowdetail));
                    if (getFlowdtailAction(flowdetail).equals("反馈情况")) {
                        flowVo.setAction(getFlowdtailAction(flowdetail) + " " + flowdetail.getNote());
                    }
                    flowVo.setOpinion(flowdetail.getNote());
                    if (flowdetail.getExcutername() != null) {
                        flowVo.setExcuter(flowdetail.getExcutername() + " (" + deptName + ")");
                    }
                    flowVo.setExcute_time(flowdetail.getExcuteTime());

                    if (flowdetail.getAction().equals("办结销号")) {
                        completedFlowVo = flowVo;
                        continue;
                    } else {
                        completedFlowVo = null;
                    }

                    flowVos.add(flowVo);
                }

            }

            if (completedFlowVo != null) {
                flowVos.add(completedFlowVo);
            }

        }

//     协办单位
        if (users.getRoleId().equals(UserRoleConstant.OFFICE_DIRECTOR) ||
                users.getRoleId().equals(UserRoleConstant.OFFICE_STAFF)) {

//            获取督查室和自己部门id
            LambdaQueryWrapper<Dept> deptLambdaQueryWrapper = new LambdaQueryWrapper<>();
            deptLambdaQueryWrapper.eq(Dept::getDeptName, "督查室").eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
            Dept deptDuCha = deptMapper.selectOne(deptLambdaQueryWrapper);

            List<Integer> deptIds = Arrays.asList(deptDuCha.getId(), users.getDeptId());


            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.in(Flow::getDeptId, deptIds).eq(Flow::getTaskId, taskId);

            List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);

//                只保留一个办结销号的
            FlowVo completedFlowVo = null;


            for (Flow flow : flows) {
                LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
                flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flow.getId());

                List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

                Task task = taskMapper.selectById(flow.getTaskId());
                if (task == null) {
                    return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
                }

                String deptName = deptMapper.selectById(flow.getDeptId()).getDeptName();

                for (Flowdetail flowdetail : flowdetails) {
                    FlowVo flowVo = new FlowVo();
                    flowVo.setAction(flowdetail.getAction());
                    if (getFlowdtailAction(flowdetail).equals("反馈情况")) {
                        flowVo.setAction(getFlowdtailAction(flowdetail) + " " + flowdetail.getNote());
                    }
                    flowVo.setOpinion(flowdetail.getNote());
                    flowVo.setExcuter(flowdetail.getExcutername() + " (" + deptName + ")");
                    flowVo.setExcute_time(flowdetail.getExcuteTime());


                    if (flowdetail.getAction().equals("办结销号")) {
                        completedFlowVo = flowVo;
                        continue;
                    } else {
                        completedFlowVo = null;
                    }

                    flowVos.add(flowVo);
                }

            }

            if (completedFlowVo != null) {
                flowVos.add(completedFlowVo);
            }

        }

        return ResponseResult.okResult(flowVos);
    }


    @Override
    public ResponseResult getExportScore(String beginDate, String endDate) {

        List<ScoreFileDataVO> scoreFileDataVOS = new ArrayList<>();

//        flowDetail查询
        LambdaQueryWrapper<Flowdetail> flowdetailWrapper = new LambdaQueryWrapper<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

//        如果日期不为空，则带上日期参加进行查询。
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
// 根据执行时间由近到远进行排序
        flowdetails.sort(Comparator.comparing(Flowdetail::getExcuteTime).reversed());

        Map<Integer, List<BigDecimal>> deptScore = new HashMap<>();

//        往积分结果集合中添加所有未删除部门
        LambdaQueryWrapper<Dept> deptWrapper = new LambdaQueryWrapper<>();
        deptWrapper.eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);

        List<Dept> depts = deptMapper.selectList(deptWrapper);

        for (Dept dept : depts) {

//            每个部门初始分数为100
            List<BigDecimal> monthScores = new ArrayList<>(Collections.nCopies(12, BigDecimal.valueOf(100)));
            deptScore.put(dept.getId(), monthScores);

            ScoreFileDataVO scoreFileDataVO =
                    new ScoreFileDataVO(dept.getId(), dept.getDeptName(), 100.0, new ArrayList<>());

            scoreFileDataVOS.add(scoreFileDataVO);
        }

//        按照流程进行扣分，在对应月份的分数里进行
        //  时间转换，返回时间对应月份
        Calendar calendar = Calendar.getInstance();

        for (Flowdetail flowdetail : flowdetails) {

//            获取对应流程信息
            Flow flow = flowMapper.selectById(flowdetail.getFlowId());

//            如果事件为中止事项则跳出
            if (flow.getStatus().equals(TaskConstant.TASK_ARCHIVED)) {
                continue;
            }

//            如果有扣分情况，则在对应分数事件添加扣分流程
            if (flowdetail.getScore() < 0) {
//                查找对应的部分分数VO类,添加扣分事项
                for (ScoreFileDataVO scoreFileDataVO : scoreFileDataVOS) {
                    if (scoreFileDataVO.getDeptId() == flow.getDeptId()) {

                        Task task = taskMapper.selectById(flow.getTaskId());

                        DeductionTaskVO deductionTaskVO =
                                new DeductionTaskVO(task.getTaskNo(), task.getTaskName(), flowdetail.getAction(), flowdetail.getExcuteTime(), flowdetail.getScore());
                        List<DeductionTaskVO> deductionTaskVOS = scoreFileDataVO.getDTasks();
                        deductionTaskVOS.add(deductionTaskVO);
                        scoreFileDataVO.setDTasks(deductionTaskVOS);
                        break;
                    }
                }
            }

            if (deptScore.containsKey(flow.getDeptId())) {
//                获取每个流程的扣分
                BigDecimal deductionScore = BigDecimal.valueOf(flowdetail.getScore());
//                获取对应执行时间月份(0~11)，比正常月份少1，方便数组加减
                calendar.setTime(flowdetail.getExcuteTime());
                int month = calendar.get(Calendar.MONTH);
//                修改数据
                List<BigDecimal> monthScores = deptScore.get(flow.getDeptId());
                BigDecimal currentScore = monthScores.get(month);
                BigDecimal updatedScore = currentScore.add(deductionScore);
                monthScores.set(month, updatedScore);

//                更新部门分数
                deptScore.put(flow.getDeptId(), monthScores);
            }

        }

//        移除督查室积分
        LambdaQueryWrapper<Dept> deptLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deptLambdaQueryWrapper.eq(Dept::getDeptName, "督查室").eq(Dept::getState, DeptStateConstant.DEPT_NORMAL);
        Dept deptDuCha = deptMapper.selectOne(deptLambdaQueryWrapper);


// 遍历deptScores属性
        for (Map.Entry<Integer, List<BigDecimal>> entry : deptScore.entrySet()) {

            int deptId = entry.getKey();

            if (deptDuCha != null) {
                if (deptDuCha.getId() == deptId) {
                    continue;
                }
            }

//            计算平均值
            BigDecimal average = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()));

            Double score = average.doubleValue();

//                查找对应的部分分数VO类,添加分数
            for (ScoreFileDataVO scoreFileDataVO : scoreFileDataVOS) {
                if (scoreFileDataVO.getDeptId() == deptId) {
                    scoreFileDataVO.setScore(score);
                    break;
                }
            }

        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileStream", writeDataFromScore(scoreFileDataVOS));
        return ResponseResult.okResult(jsonObject);
    }


    @Override
    public ResponseResult exportTask(String status) {

        List<Task> tasks = new ArrayList<>();

        if (!status.equals("")) {
            String[] dataArray = status.split(",");
            LambdaQueryWrapper<Task> taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
            taskLambdaQueryWrapper.in(Task::getState, dataArray);
            tasks = taskMapper.selectList(taskLambdaQueryWrapper);
        }

        if (status.equals("")) {
            tasks = taskMapper.selectList(null);
        }


//        按创建时间排序，由近到远
        tasks.sort(Comparator.comparing(Task::getStartTime).reversed());


        // 创建一个工作簿，注意使用 SXSSFWorkbook
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // 100表示每次在内存中保持的行数

        // 创建一个工作表
        Sheet sheet = workbook.createSheet("图表数据");

        Row firstRow = sheet.createRow(0);
        firstRow.createCell(0).setCellValue("编号");
        firstRow.createCell(1).setCellValue("类型");
        firstRow.createCell(2).setCellValue("重大");
        firstRow.createCell(3).setCellValue("事项");
        firstRow.createCell(4).setCellValue("主办单位");
        firstRow.createCell(5).setCellValue("协办单位");
        firstRow.createCell(6).setCellValue("交办时间");
        firstRow.createCell(7).setCellValue("办理时限");
        firstRow.createCell(8).setCellValue("紧急程度");
        firstRow.createCell(9).setCellValue("反馈时间");
        firstRow.createCell(10).setCellValue("办理情况");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 1; i < tasks.size() + 1; i++) {

            Row row = sheet.createRow(i);
            Task task = tasks.get(i - 1);
            row.createCell(0).setCellValue(task.getTaskNo());
            row.createCell(1).setCellValue(convertNumberToStringToTaskType(task.getTaskType()));
            row.createCell(2).setCellValue(TaskUtils.converToStringForIsVip(task.getIsVip()));
            row.createCell(3).setCellValue(task.getTaskName());
            row.createCell(4).setCellValue(coverListStringToString(getTaskerName(task.getMtasker())));
            row.createCell(5).setCellValue(coverListStringToString(getTaskerName(task.getStasker())));
            row.createCell(6).setCellValue(formatter.format(task.getStartTime()));
            row.createCell(7).setCellValue(formatter.format(task.getEndTime()));
            row.createCell(8).setCellValue(task.getUrgency());

            String fankuiTime = "";
            LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowLambdaQueryWrapper.eq(Flow::getTaskId, task.getId());
            List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);
            for (Flow flow : flows) {

                String deptName = deptMapper.selectById(flow.getDeptId()).getDeptName();

                LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
                flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flow.getId());
                List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

                if (flowdetails.size() != 0) {
//                获取执行时间最近的
                    Flowdetail latestFlowdetail = flowdetails.get(0);
                    for (int j = 1; j < flowdetails.size(); j++) {
                        Flowdetail currentFlowdetail = flowdetails.get(j);
                        if (currentFlowdetail.getExcuteTime().after(latestFlowdetail.getExcuteTime())) {
                            latestFlowdetail = currentFlowdetail;
                        }
                    }
                    fankuiTime += deptName + ":" + formatter.format(latestFlowdetail.getExcuteTime()) + ",";
                }


            }


            row.createCell(9).setCellValue(fankuiTime);
            row.createCell(10).setCellValue(convertNumberToStringToTaskResult(task.getState()));

        }

        //定义excel文件名称
        String fileName = "事项数据.xlsx";
        //向云服务器保存文件，并接收其返回的json数据(具体见接口文档，其主要包括文件下载链接，文件名称)
        //新建对象：储存文件后的返回链接,文件二进制流,返回的文件名称
        String returnUrl = "";
        String blob = "";
        String returnName = "";

        //将MultipartFile类型的文件转为blob二进制流发送出去
        try {

            //创建临时文件
            File tempFile = File.createTempFile("tempExcel", ".xlsx");

            // 输出SXSSFWorkbook到临时文件
            FileOutputStream fileOut = new FileOutputStream(tempFile);
            workbook.write(fileOut);
            fileOut.close();
            //读取其文件字节
            byte[] byteArray = readBytesFromFile(tempFile);
            blob = Base64.getEncoder().encodeToString(byteArray);

            //发送请求链接
            if (blob != "") {
                //目标接口地址
                String url = "http://124.220.42.243:8188/saveFile";
                //设置请求数据(文件名称，在云服务器的相对路径，文件二进制流)
                JSONObject postData = new JSONObject();
                postData.put("fileName", fileName);
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

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileStream", returnUrl);

        return ResponseResult.okResult(jsonObject);
    }

    @Override
    public ResponseResult getDeptStatus(Integer taskId) {


        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
        }


        Integer[] strakIds = TaskUtils.convertToIntArray(task.getStasker());

        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();

        flowLambdaQueryWrapper.eq(Flow::getTaskId, taskId);

        if (strakIds.length != 0) {
            flowLambdaQueryWrapper.in(Flow::getDeptId, strakIds);
        } else {
            return ResponseResult.okResult(new ArrayList<>());
        }

        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper).stream()
                .collect(Collectors.toMap(Flow::getDeptId, Function.identity(), (oldFlow, newFlow) -> oldFlow))
                .values()
                .stream()
                .collect(Collectors.toList());


        List<Integer> deptIds = new ArrayList<>();

        for (Flow flow : flows) {
            if (!flow.getStatus().equals(TaskConstant.UNIT_LEADER_SUBMITTED)) {
                deptIds.add(flow.getDeptId());
            }
        }

        List<String> deptName = new ArrayList<>();
        for (Integer deptId : deptIds) {
            Dept dept = deptMapper.selectById(deptId);
            deptName.add(dept.getDeptName());
        }

        return ResponseResult.okResult(deptName);
    }

    @Override
    public ResponseResult getAllDeptTasks(Integer userId) {

        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getDeptId, users.getDeptId());

        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);

        List<TaskContentNormalOneStatusVo> taskVos = new ArrayList<>();
        for (Flow flow : flows) {
            Task task = taskMapper.selectById(flow.getTaskId());
            TaskContentNormalOneStatusVo taskVo = BeanCopyUtils.copyBean(task, TaskContentNormalOneStatusVo.class);

//        定义事项id
            taskVo.setTaskId(task.getId());

//        设置协办单位名称数组(List<String>)
            taskVo.setStasker(getTaskerName(task.getStasker()));
//        设置主办单位名称
            taskVo.setMtasker(getTaskerName(task.getMtasker()));
//            设置状态
            taskVo.setStatus(flow.getStatus());

            taskVos.add(taskVo);
        }

        return ResponseResult.okResult(taskVos);


    }

    @Override
    public ResponseResult deleteTask(Integer taskId) {

        Task task = taskMapper.selectById(taskId);

        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.DATA_NULL);
        }

        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getTaskId, task.getId());
        List<Flow> flows = flowMapper.selectList(flowLambdaQueryWrapper);

        for (Flow flow : flows) {

            LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper =
                    new LambdaQueryWrapper<>();
            flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flow.getId());

            List<Flowdetail> flowdetails =
                    flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

            for (Flowdetail flowdetail : flowdetails) {
                flowdetailMapper.deleteById(flowdetail.getId());
            }

            flowMapper.deleteById(flow.getId());

        }

        taskMapper.deleteById(taskId);

        return ResponseResult.okResult();
    }


    @Override
    public ResponseResult DeptTaskResult(Integer userId, Integer taskId) {


        Users users = usersMapper.selectById(userId);
        if (users == null || users.getState().equals(UserStateConstant.USER_IS_DELETE)) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "用户为空");
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应事项");
        }

        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getTaskId, taskId)
                .eq(Flow::getDeptId, users.getDeptId());

        Flow flow = flowMapper.selectOne(flowLambdaQueryWrapper);
        if (flow == null) {
            return ResponseResult.errorResult(ErrorCode.SEARACH_NULL, "未找到对应流程");
        }

        LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flow.getId());
        List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);

        String result = " ";

        if (flowdetails.size() != 0) {
//                获取执行时间最近的
            Flowdetail latestFlowdetail = flowdetails.get(0);
            for (int j = 1; j < flowdetails.size(); j++) {
                Flowdetail currentFlowdetail = flowdetails.get(j);
                if (currentFlowdetail.getAction().equals(TaskActionConstant.ALLOCATED)) {
                    if (currentFlowdetail.getExcuteTime().after(latestFlowdetail.getExcuteTime())) {
                        latestFlowdetail = currentFlowdetail;
                    }
                    if (!latestFlowdetail.getAction().equals(TaskActionConstant.ALLOCATED)) {
                        latestFlowdetail = currentFlowdetail;
                    }
                }

            }

            if (latestFlowdetail.getNote() != null) {
                result = latestFlowdetail.getNote();
            }
        }


        return ResponseResult.okResult(result);

    }


    public String coverListStringToString(List<String> data) {
        String result = "";
        for (int i = 0; i < data.size(); i++) {
            String d = data.get(i);
            result += d;
            if (i != data.size() - 1) {
                result += "，";
            }
        }
        return result;
    }


    public String writeDataFromScore(List<ScoreFileDataVO> scoreFileDataVOS) {

//        时间转换
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 创建一个工作簿，注意使用 SXSSFWorkbook
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // 100表示每次在内存中保持的行数

        // 创建一个工作表
        Sheet sheet = workbook.createSheet("图表数据");

//        Excel标题行
        Row firstRow = sheet.createRow(0);
        firstRow.createCell(0).setCellValue("序号");
        firstRow.createCell(1).setCellValue("部门单位");
        firstRow.createCell(2).setCellValue("年度分值");
        firstRow.createCell(3).setCellValue("扣分事项编号");
        firstRow.createCell(4).setCellValue("扣分事项名称");
        firstRow.createCell(5).setCellValue("扣分原因");
        firstRow.createCell(6).setCellValue("扣分时间");
        firstRow.createCell(7).setCellValue("事项扣分");

//        额外添加行数
        int extraAddRow = 0;
//        Excel内容输入
        for (int i = 1; i < scoreFileDataVOS.size() + 1; i++) {

            ScoreFileDataVO scoreFileDataVO = scoreFileDataVOS.get(i - 1);

            Row row = sheet.createRow(i + extraAddRow);
            row.createCell(0).setCellValue(i);
            row.createCell(1).setCellValue(scoreFileDataVO.getDeptName());
            row.createCell(2).setCellValue(scoreFileDataVO.getScore());

            List<DeductionTaskVO> deductionTaskVOS = scoreFileDataVO.getDTasks();

            if (deductionTaskVOS.size() != 0) {
                for (int h = 0; h < deductionTaskVOS.size(); h++) {

                    DeductionTaskVO deductionTaskVO = deductionTaskVOS.get(h);
                    Row taksRow;
                    if (h == 0) {
                        taksRow = row;
                        extraAddRow--;
                    } else {
                        taksRow = sheet.createRow(i + h);
                    }
                    extraAddRow++;
                    taksRow.createCell(3).setCellValue(deductionTaskVO.getTaskNumber());
                    taksRow.createCell(4).setCellValue(deductionTaskVO.getTaskName());
                    taksRow.createCell(5).setCellValue(deductionTaskVO.getReason());
                    taksRow.createCell(6).setCellValue(simpleDateFormat.format(deductionTaskVO.getTime()).toString());
                    taksRow.createCell(7).setCellValue(deductionTaskVO.getScore().toString());
                }
            }

            if (deductionTaskVOS.size() == 0) {
                row.createCell(3).setCellValue("无扣分事项");
            }

        }

        //定义excel文件名称
        String fileName = "积分数据.xlsx";
        //向云服务器保存文件，并接收其返回的json数据(具体见接口文档，其主要包括文件下载链接，文件名称)
        //新建对象：储存文件后的返回链接,文件二进制流,返回的文件名称
        String returnUrl = "";
        String blob = "";
        String returnName = "";

        //将MultipartFile类型的文件转为blob二进制流发送出去
        try {

            //创建临时文件
            File tempFile = File.createTempFile("tempExcel", ".xlsx");

            // 输出SXSSFWorkbook到临时文件
            FileOutputStream fileOut = new FileOutputStream(tempFile);
            workbook.write(fileOut);
            fileOut.close();
            //读取其文件字节
            byte[] byteArray = readBytesFromFile(tempFile);
            blob = Base64.getEncoder().encodeToString(byteArray);

            //发送请求链接
            if (blob != "") {
                //目标接口地址
                String url = "http://124.220.42.243:8188/saveFile";
                //设置请求数据(文件名称，在云服务器的相对路径，文件二进制流)
                JSONObject postData = new JSONObject();
                postData.put("fileName", fileName);
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


        return returnUrl;
    }


    public String getFirstStatus(Integer taskId) {
        Task task = taskMapper.selectById(taskId);
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getTaskId, taskId);

        List<Flow> flowList = flowMapper.selectList(flowLambdaQueryWrapper);

        if (flowList.isEmpty()) {
            return task.getState();
        }

        for (Flow flow : flowList) {
            if (flow.getStatus().equals(TaskConstant.OVERDUE_APPLICATION)) {
                return flow.getStatus();
            }
            if (flow.getStatus().equals(TaskConstant.FEEDBACK_OVERDUE)) {
                return flow.getStatus();
            }
            if (flow.getStatus().equals(TaskConstant.FEEDBACK_REJECTION)) {
                return flow.getStatus();
            }
        }

        return task.getState();
    }


    public JSONObject getAllTaskStatue(Integer taskId) {
        //          获取事项下对应部门的状态
        LambdaQueryWrapper<Flow> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Flow::getTaskId, taskId);
        List<Flow> flows = flowMapper.selectList(flowWrapper);
        // 使用比较器对 flows 列表进行排序
        List<String> sortOrder = Arrays.asList("3", "7", "9", "2", "4", "5", "6", "8");
        Comparator<Flow> flowComparator = Comparator.comparingInt(flow ->
                sortOrder.indexOf(String.valueOf(flow.getStatus())));
        Collections.sort(flows, flowComparator);
        // 封装对象
        JSONObject jsonObject = new JSONObject();
        for (Flow flow : flows) {
            Dept dept = deptMapper.selectById(flow.getDeptId());
            if (dept == null || dept.getState().equals(DeptStateConstant.DEPT_IS_DELETE)) {
                continue;
            }
            jsonObject.put(dept.getDeptName(), flow.getStatus());
        }
        return jsonObject;
    }


    //从File文件读取其byte字节
    public byte[] readBytesFromFile(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fileInputStream.read(bytes);
        fileInputStream.close();
        return bytes;
    }

    public String convertNumberToStringToTaskType(String number) {
        if (number == null || number.equals("")) {
            return "";
        }

        switch (number) {
            case "1":
                return "上级交办事项";
            case "2":
                return "县委决策事项";
            case "3":
                return "领导交办事项";
            case "4":
                return "群众反映事项";
            case "5":
                return "专题事项";
            case "6":
                return "重点工作";
            case "7":
                return "其他事项";
            default:
                return "未知事项";
        }
    }


    public String convertNumberToStringToTaskResult(String number) {
        if (number == null) {
            return "";
        }
        switch (number) {
            case "1":
                return "待交办";
            case "2":
                return "待领办";
            case "3":
                return "领办逾期";
            case "4":
                return "已领办";
            case "5":
            case "6":
                return "推进中";
            case "7":
                return "上报逾期";
            case "8":
                return "已上报";
            case "9":
                return "上报驳回";
            case "10":
                return "已办结";
            case "11":
                return "已终止";
            default:
                return "未知状态";
        }
    }


    public String getFlowdtailAction(Flowdetail flowdetail) {
        if (flowdetail.getAction().equals(TaskActionConstant.HOSTING_UNIT_LEADER_REJECTED)) {
            LambdaQueryWrapper<Flowdetail> flowdetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            flowdetailLambdaQueryWrapper.eq(Flowdetail::getFlowId, flowdetail.getFlowId())
                    .eq(Flowdetail::getAction, flowdetail.getAction())
                    .orderByAsc(Flowdetail::getExcuteTime);
            List<Flowdetail> flowdetails = flowdetailMapper.selectList(flowdetailLambdaQueryWrapper);
            int index = flowdetails.indexOf(flowdetail) + 1;
            return "第" + index + "次" + flowdetail.getAction();
        }
        return flowdetail.getAction();
    }

}
