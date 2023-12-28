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
 * (Flow)表实体类
 *
 * @author zrx
 * @since 2023-12-21 20:13:25
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("flow")
public class Flow  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //任务id
    private Integer taskId;
    //操作名称
    private String action;
    //执行人id
    private Integer excuter;
    //执行时间
    private Date excuteTime;
    //执行前任务状态
    private String stateBefore;
    //执行后任务状态
    private String stateAfte;
    //下一个执行人id
    private Integer nextExcuter;
    //意见
    private String note;
    //得分或扣分
    private Integer score;


    public Flow(Integer taskId, String action, Integer excuter, Date excuteTime, String stateBefore, String stateAfte, Integer nextExcuter, String note, Integer score) {
        this.taskId = taskId;
        this.action = action;
        this.excuter = excuter;
        this.excuteTime = excuteTime;
        this.stateBefore = stateBefore;
        this.stateAfte = stateAfte;
        this.nextExcuter = nextExcuter;
        this.note = note;
        this.score = score;
    }
}
