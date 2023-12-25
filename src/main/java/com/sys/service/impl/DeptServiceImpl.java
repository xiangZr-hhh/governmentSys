package com.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.entity.Dept;
import com.sys.mapper.DeptMapper;
import com.sys.service.DeptService;
import org.springframework.stereotype.Service;

/**
 * (Dept)表服务实现类
 *
 * @author zrx
 * @since 2023-12-21 20:13:15
 */
@Service("deptService")
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

}
