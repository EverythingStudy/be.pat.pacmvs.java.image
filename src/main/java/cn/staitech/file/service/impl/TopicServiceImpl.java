package cn.staitech.file.service.impl;

import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.system.api.domain.SysUser;
import cn.staitech.file.mapper.TopicMapper;
import cn.staitech.file.domain.Topic;
import cn.staitech.file.service.TopicService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author: wangfeng
 * @create: 2023-06-02 14:06:14
 * @Description: 切片（原图片）专题 TopicServiceImpl
 */
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

}
