package com.sys.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * (Dept)表实体类
 *
 * @author zrx
 * @since 2023-12-21 20:13:15
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("dept")
public class Dept  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //部门名称
    private String deptName;
    //部门状态：0-有效、1-删除
    private String state;


}
