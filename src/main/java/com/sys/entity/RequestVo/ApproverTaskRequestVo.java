package com.sys.entity.RequestVo;
/*
        张睿相   Java
*/

import lombok.Data;

/**
 * 总体描述
 * <p>创建时间：2024/1/3 13:01</p>
 *
 * @author 张睿相
 * @since v1.0
 */
@Data
public class ApproverTaskRequestVo {

//    任务id
    private Integer taskId;

//    (“0”通过 “1”驳回)
    private String approveResult;

//    （审批意见，可空）
    private String opinion;

}


