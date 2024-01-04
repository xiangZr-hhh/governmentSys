package com.sys.entity.ResponseVo;
/*
        张睿相   Java
*/

import com.sys.entity.RequestVo.AttachmentVo;
import lombok.Data;

import java.util.List;

/**
 * 总体描述
 * <p>创建时间：2024/1/3 12:48</p>
 *
 * @author 张睿相
 * @since v1.0
 */
@Data
public class TaskResultResponseVo {

    private String result;

    private List<AttachmentVo> attachment;
}


