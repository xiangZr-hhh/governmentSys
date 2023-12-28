package com.sys.entity.RequestVo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/*
        张睿相   Java
*/
@Data
public class FromVo {

    private String taskNo;

    private String taskType;

    private String isVip;

    private String taskName;

    private String taskDetail;

    private Integer mTaskerid;

    private Integer[]  sTaskerid;

    private String startTime;

    private String endTime;

    private String urgency;

    private String notes;

    private List<AttachmentVo> attachment;

}
