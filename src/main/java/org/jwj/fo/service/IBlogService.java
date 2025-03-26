package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.Blog;


public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryById(Long id);

    Result queryMyBlog(Integer current);

    Result likeBlog(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogLikes(Long id);

    Result queryBlogByUserId(Integer current, Long id);

    Result queryBlogOfFollow(Long max, Integer offset);
}
