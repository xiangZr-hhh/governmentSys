package com.sys.mapper;

import com.sys.entity.Users;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Users)表数据库访问层
 *
 * @author zrx
 * @since 2023-12-21 20:13:42
 */
@Mapper
public interface UsersMapper extends BaseMapper<Users> {

}
