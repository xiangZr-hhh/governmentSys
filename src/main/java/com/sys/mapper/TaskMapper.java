package com.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Task)表数据库访问层
 *
 * @author zrx
 * @since 2024-01-22 11:43:34
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

}
