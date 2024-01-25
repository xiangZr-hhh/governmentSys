package com.sys.entity.RequestVo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/*
        张睿相   Java
*/
@Data
public class FromVo {

    @NotBlank(message = "任务编号不能为空")
    private String taskNo;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "是否重大不能为空")
    private String isVip;

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotBlank(message = "任务描述不能为空")
    private String taskDetail;

    @NotNull(message = "主办单位id不能为空")
    private Integer[] mTaskerid;

    @NotNull(message = "协办单位id不能为空")
    private Integer[] sTaskerid;

    @NotBlank(message = "交办时间不能为空")
    private String startTime;

    @NotBlank(message = "办理时限不能为空")
    private String endTime;

    @NotBlank(message = "紧急程度不能为空")
    private String urgency;

    @NotBlank(message = "备注不能为空")
    private String notes;

    @NotNull(message = "附件不能为空")
    private List<AttachmentVo> attachment;

}
