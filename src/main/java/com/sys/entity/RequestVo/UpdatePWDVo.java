package com.sys.entity.RequestVo;
/*
        张睿相   Java
*/

import lombok.Data;

/**
 * 总体描述
 * <p>创建时间：2024/1/3 12:17</p>
 *
 * @author 张睿相
 * @since v1.0
 */
@Data
public class UpdatePWDVo {

    private String uerId;

    private String oldPwd;

    private String newPwd;

}


