package com.sys.entity.ResponseVo;

import lombok.Data;

import java.util.Date;

/*
        张睿相   Java

        归档事项Vo类
*/
@Data
public class ArchiveIdTaskResponseVo {

//    归档ID
    private Integer archiveId;
//    归档编号
    private String archiveNo;
//    归档日期
    private Date archiveDate;
//    归档人
    private Integer archivePerson;
//    事项编号
    private String taskNo;
//    事项类型
    private String taskType;
//    是否重大
    private String isVip;
//    事项名称
    private String taskName;
//    主办单位名称
    private String mTasker;
//    办结日期
    private Date completeDate;

}
