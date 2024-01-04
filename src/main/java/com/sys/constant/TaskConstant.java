package com.sys.constant;

/*
    张睿相   Java
*/
public interface TaskConstant {

    // 事项状态--0---事项未创建
    String TASK_NO_CREATE = "0";

    // 事项状态--1---督办人员已提交（待交办）
    String SUPERVISING_PERSONNEL_SUBMITTED = "1";

    // 事项状态--2---督办主任驳回（提交驳回）
    String SUPERVISING_DIRECTOR_REJECTED = "2";

    // 事项状态--3---督办主任审核通过（已交办）
    String SUPERVISING_DIRECTOR_APPROVED = "3";

    // 事项状态--4---已领办
    String TASK_ASSIGNED = "4";

    // 事项状态--5---已分配
    String TASK_ALLOCATED = "5";

    // 事项状态--6---已执行
    String TASK_EXECUTED = "6";

    // 事项状态--7---主办单位领导驳回
    String UNIT_LEADER_REJECTED = "7";

    // 事项状态--8---主办单位领导提交
    String UNIT_LEADER_SUBMITTED = "8";

    // 事项状态--9---督办主任驳回
    String SUPERVISING_DIRECTOR_REJECTED_2 = "9";

    // 事项状态--10---督办主任审核通过（已办结）
    String SUPERVISING_DIRECTOR_APPROVED_2 = "10";

    // 事项状态--11---已归档
    String TASK_ARCHIVED = "11";

    // 事项状态--12---已删除
    String TASK_DELETED = "12";

    // 事项状态--13---已中止
    String TASK_TERMINATED = "13";

    // 事项状态--14---领办逾期
    String TASK_ASSIGNED_OVERDUE = "14";

    // 事项状态--15---反馈逾期
    String FEEDBACK_OVERDUE = "15";

}
