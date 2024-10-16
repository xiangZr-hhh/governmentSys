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
 * @since 2024-03-14 19:34:27
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("users")
public class Users  {
    @TableId(type= IdType.AUTO)
    private Integer id;
    //密码，md5加密
    private String password;
    //账号
    private String username;
    //用户昵称
    private String nickname;
    //性别，用中文：男，女
    private String sex;
    //手机号1
    private String phone1;
    //手机号2
    private String phone2;
    //归属部门id
    private Integer deptId;
    //角色id:系统管理员（“1”），督办主任（“2”），督办人员（“3”），办事单位领导（“4”），办事单位执行人（“5”）
    private String roleId;
    //用户状态：0-有效、1-删除
    private String state;


    public Users(String username, String phone1, String phone2, Integer deptId, String roleId) {
        this.username = username;
        this.phone1 = phone1;
        this.phone2 = phone2;
        this.deptId = deptId;
        this.roleId = roleId;
    }
}
