package com.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.entity.Users;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Users)表数据库访问层
 *
 * @author zrx
 * @since 2024-01-22 11:43:35
 */
@Mapper
public interface UsersMapper extends BaseMapper<Users> {

}
