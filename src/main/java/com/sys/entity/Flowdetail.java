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
 * (Flowdetail)表实体类
 *
 * @author zrx
 * @since 2024-01-22 10:27:07
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("flowdetail")
public class Flowdetail  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //流程id
    private Integer flowId;
    //执行动作
    private String action;
    //意见
    private String note;
    //执行人id
    private Integer excuter;
    //执行人姓名
    private String excutername;
    //执行时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date excuteTime;
    //扣分;默认0
    private Double score;


}
