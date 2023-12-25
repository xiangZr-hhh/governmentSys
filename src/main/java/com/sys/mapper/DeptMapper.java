package com.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Dept)表数据库访问层
 *
 * @author zrx
 * @since 2023-12-21 20:13:15
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

}
