package com.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.entity.Flow;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Flow)表数据库访问层
 *
 * @author zrx
 * @since 2023-12-21 20:13:25
 */
@Mapper
public interface FlowMapper extends BaseMapper<Flow> {

}
