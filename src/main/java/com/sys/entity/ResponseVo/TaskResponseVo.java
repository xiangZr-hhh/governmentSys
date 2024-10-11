package com.sys.entity.ResponseVo;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/*
        张睿相   Java
*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseVo {

    //事项id
    private Integer taskId;
    //文号，例：[2023]01号
    private String taskNo;
    //事项类型
    private String taskType;
    //是否重大
    private String isVip;
    //事项名称
    private String taskName;
    //主办单位id
    private List<String> mtasker;
    //协办单位id拼接字符串（用逗号作分隔符）
    private List<String> stasker;
    //交办时间
    private Date startTime;
    //紧急程度
    private String urgency;
    //办理时限
    private Date endTime;
    //事项状态
    private JSONObject status;

}
