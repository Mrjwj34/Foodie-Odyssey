package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.mapper.BlogMapper;
import org.jwj.fo.service.IBlogService;
import org.springframework.stereotype.Service;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
