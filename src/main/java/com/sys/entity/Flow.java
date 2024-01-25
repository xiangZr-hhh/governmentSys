package com.sys.entity;

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
 * @since 2024-01-22 11:16:57
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("flow")
public class Flow  {
    //主键
    @TableId(type= IdType.AUTO)
    private Integer id;
    //任务id
    private Integer taskId;
    //主办/协办单位id
    private Integer deptId;
    //0:主办；1:协办
    private Integer type;
    //当前主/协办单位办理状态:2-10
    private String status;
    //待执行人id；支线完成则置空
    private Integer excuter;


}
