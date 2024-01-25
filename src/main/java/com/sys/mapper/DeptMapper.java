package com.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Dept)表数据库访问层
 *
 * @author zrx
 * @since 2024-01-22 11:43:33
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

}
