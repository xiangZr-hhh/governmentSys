package com.sys.entity;

import java.util.Date;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * (FlowAsist)表实体类
 *
 * @author zrx
 * @since 2023-12-27 23:53:39
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("flow_asist")
public class FlowAsist  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //事项id
    private Integer taskId;
    //协办部门id
    private Integer deptId;
    //操作名称
    private String action;
    //执行人id
    private Integer excuter;
    //执行时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date excuteTime;
    //意见
    private String note;

    public FlowAsist(Integer taskId, Integer deptId, String action, Integer excuter, Date excuteTime, String note) {
        this.taskId = taskId;
        this.deptId = deptId;
        this.action = action;
        this.excuter = excuter;
        this.excuteTime = excuteTime;
        this.note = note;
    }
}
