package org.jwj.fo.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.entity.User;
import org.jwj.fo.service.IBlogService;
import org.jwj.fo.service.IUserService;
import org.jwj.fo.utils.SystemConstants;
import org.jwj.fo.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBlog(current);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }
    @GetMapping("/{id}")
    public Result queryBlog(@PathVariable("id") Long id) {
        return blogService.queryById(id);
    }
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }
    @GetMapping("/of/user")
    public Result queryBlogByUserId(Integer current, Long id){
        return blogService.queryBlogByUserId(current, id);
    }
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(max, offset);
    }
}
