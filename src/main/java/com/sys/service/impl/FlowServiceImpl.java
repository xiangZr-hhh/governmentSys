package com.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.entity.Flow;
import com.sys.mapper.FlowMapper;
import com.sys.service.FlowService;
import org.springframework.stereotype.Service;

/**
 * (Flow)表服务实现类
 *
 * @author zrx
 * @since 2023-12-21 20:13:25
 */
@Service("flowService")
public class FlowServiceImpl extends ServiceImpl<FlowMapper, Flow> implements FlowService {

}
