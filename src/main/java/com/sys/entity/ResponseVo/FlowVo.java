package com.sys.entity.ResponseVo;

import lombok.Data;

import java.util.Date;

/*
        流程表封装的Vo类
*/
@Data
public class FlowVo {

    //操作名称
    private String action;
    //执行人id
    private Integer excuter;
    //执行时间
    private Date excuteTime;
    //意见
    private String note;
}
