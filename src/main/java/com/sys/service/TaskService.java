package com.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.common.ResponseResult;
import com.sys.entity.RequestVo.SubmitTaskRequestVo;
import com.sys.entity.Task;


/**
 * (Task)表服务接口
 *
 * @author zrx
 * @since 2023-12-21 20:13:33
 */
public interface TaskService extends IService<Task> {

    ResponseResult submitNewTask(SubmitTaskRequestVo submitTaskRequestVo);
}

