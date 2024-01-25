package com.sys.entity.ResponseVo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/*
        张睿相   Java

        归档事项Vo类
*/
@Data
public class ArchiveIdTaskResponseVo {


//    事项编号
    private String taskNo;
//    事项名称
    private String taskName;
//    主办单位名称
    private List<String> mTasker;
    //协办单位名称数组
    private List<String> sTaske;
//    办结日期
    private Date completeDate;

}
