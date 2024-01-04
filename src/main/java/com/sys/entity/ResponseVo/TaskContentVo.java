package com.sys.entity.ResponseVo;

import com.sys.entity.RequestVo.AttachmentVo;
import lombok.Data;

import java.util.Date;
import java.util.List;

/*
        张睿相   Java
*/
@Data
public class TaskContentVo {

    //事项ID
    private Integer taskId;
    //文号，例：[2023]01号
    private String taskNo;
    //事项类型
    private String taskType;
    //是否重大
    private String isVip;
    //事项名称
    private String taskName;
    //事项描述
    private String taskDetail;
    //主办单位id
    private Integer mTaskerId;
    //主办单位名称
    private String mTasker;
    //协办单位id数组
    private Integer[] sTaskerId;
    //协办单位名称数组
    private List<String> sTaske;
    //交办时间
    private Date startTime;
    //办理时限
    private Date endTime;
    //紧急程度
    private String urgency;
    //备注
    private String notes;
    //附件
    private List<AttachmentVo> attachment;
    //流程
    private List<FlowVo> flow;

}
