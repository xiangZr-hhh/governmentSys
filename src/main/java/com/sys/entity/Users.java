package com.sys.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * (Users)表实体类
 *
 * @author zrx
 * @since 2023-12-21 20:25:41
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("users")
public class Users  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //用户编号（工号），登录用
    private String jobNumber;
    //密码，md5加密
    private String password;
    //用户名
    private String username;
    //性别，用中文：男，女
    private String sex;
    //手机号
    private String phone;
    //归属部门id
    private Integer deptId;
    //角色id:系统管理员（“1”），督办主任（“2”），督办人员（“3”），办事单位领导（“4”），办事单位执行人（“5”）
    private String roleId;
    //用户状态：0-有效、1-删除
    private String state;

    public Users(String username, String phone, Integer deptId, String roleId) {
        this.username = username;
        this.phone = phone;
        this.deptId = deptId;
        this.roleId = roleId;
    }
}
