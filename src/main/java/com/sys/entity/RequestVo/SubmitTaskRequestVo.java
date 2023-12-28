package com.sys.entity.RequestVo;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestBody;

/*
        张睿相   Java
*/
@Data
public class SubmitTaskRequestVo {

    private Integer creatorId;

    private FromVo form;
}
