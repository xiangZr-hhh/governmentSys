package com.sys.entity.RequestVo;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/*
        张睿相   Java
*/
@Data
public class SubmitTaskRequestVo {

    @NotNull(message = "创建者id不能为空")
    private Integer creatorId;

    private FromVo form;
}
