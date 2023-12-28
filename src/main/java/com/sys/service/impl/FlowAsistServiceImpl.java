package com.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.entity.FlowAsist;
import com.sys.mapper.FlowAsistMapper;
import com.sys.service.FlowAsistService;
import org.springframework.stereotype.Service;

/**
 * (FlowAsist)表服务实现类
 *
 * @author zrx
 * @since 2023-12-27 23:53:39
 */
@Service("flowAsistService")
public class FlowAsistServiceImpl extends ServiceImpl<FlowAsistMapper, FlowAsist> implements FlowAsistService {

}
