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
 * (Task)表实体类
 *
 * @author zrx
 * @since 2023-12-21 20:13:33
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("task")
public class Task  {

    @TableId(type= IdType.AUTO)
    private Integer id;
    //文号，例：[2023]01号
    private String taskNo;
    //任务类型
    private String taskType;
    //是否重大
    private String isVip;
    //任务名称
    private String taskName;
    //任务描述
    private String taskDetail;
    //主办单位id
    private String mtasker;
    //协办单位id拼接字符串（用逗号作分隔符）
    private String stasker;
    //交办时间
    private Date startTime;
    //办理时限
    private Date endTime;
    //紧急程度
    private String urgency;
    //备注
    private String notes;
    //附件地址拼接字符串（用逗号作分隔符）
    private String attachment;
    //创建人
    private Integer createBy;
    //创建时间
    private Date createTime;
    //是否黄牌警告（0：否；1：是）
    private Integer yellowCard;
    //任务状态
    private String state;


}
