package com.sys.entity.RequestVo;
/*
        张睿相   Java
*/

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 总体描述
 * <p>创建时间：2024/1/3 12:17</p>
 *
 * @author 张睿相
 * @since v1.0
 */
@Data
public class UpdatePWDVo {

    @NotNull(message = "用户id不能为空")
    private Integer userId;

    @NotBlank(message = "旧密码不能为空")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    private String newPwd;

}


