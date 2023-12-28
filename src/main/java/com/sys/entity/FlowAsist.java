package com.sys.entity;

import java.util.Date;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
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
    //任务id
    private Integer taskId;
    //协办部门id
    private Integer deptId;
    //操作名称
    private String action;
    //执行人id
    private Integer excuter;
    //执行时间
    private Date excuteTime;
    //意见
    private String note;


}
