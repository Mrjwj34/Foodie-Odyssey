package org.jwj.fo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.ScrollResult;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.entity.User;
import org.jwj.fo.mapper.BlogMapper;
import org.jwj.fo.service.IBlogService;
import org.jwj.fo.service.IFollowService;
import org.jwj.fo.service.IUserService;
import org.jwj.fo.utils.SystemConstants;
import org.jwj.fo.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.jwj.fo.utils.RedisConstants.BLOG_LIKED_KEY;
import static org.jwj.fo.utils.RedisConstants.FEED_KEY;
import static org.jwj.fo.utils.SystemConstants.MAX_PAGE_SIZE;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::queryBolgUser);
        return Result.ok(records);
    }

    @Override
    public Result queryById(Long id) {
        // 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博文不存在");
        }
        // 查询blog相关用户
        queryBolgUser(blog);
        return Result.ok(blog);
    }


    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        // 判断当前用户是否已经点赞
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请登录后再点赞");
        }
        Double isMember = stringRedisTemplate.opsForZSet()
                .score(BLOG_LIKED_KEY + id, user.getId().toString());
        // 未点赞, 点赞数量加一
        if(isMember == null) {
            // 数据库点赞数加一
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到redis集合
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(BLOG_LIKED_KEY + id, user.getId().toString(), System.currentTimeMillis());
            }
        } else {
            // 已经点赞, 点赞数量减一
            // 数据库点赞数减一
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 从redis集合删除用户
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(BLOG_LIKED_KEY + id, user.getId().toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        // 查询笔记作者的所有粉丝
        if (!isSuccess) {
            return Result.fail("保存失败");
        }
        followService.query().eq("follow_user_id", user.getId()).list()
                .forEach(follow -> {
                    // 推送笔记给粉丝
                    String key = FEED_KEY + follow.getUserId();
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                });
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogLikes(Long id) {
        // 查询前五个点赞用户id
        List<Long> userIds = Objects.requireNonNull(stringRedisTemplate.opsForZSet()
                        .reverseRange(BLOG_LIKED_KEY + id, 0, 4))
                        .stream().map(Long::valueOf).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        String join = StrUtil.join(",", userIds);
        // 返回用户列表
        List<UserDTO> userDTOs =  userService.query()
                .in("id", userIds).last("order by field(id, " + join + ")")
                .list().stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOs);
    }

    @Override
    public Result queryBlogByUserId(Integer current, Long id) {
        Page<Blog> page = query().eq("user_id", id).page(new Page<>(current, MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取当前用户
        UserDTO user = UserHolder.getUser();
        // 查询收件箱
        String key = FEED_KEY + user.getId();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 解析推文id: bolgId, mintime(时间戳), offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            ids.add(Long.valueOf(typedTuple.getValue()));
            long time = typedTuple.getScore().longValue();
            if(time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        // 根据id查询blog
        String join = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("order by field(id, " + join + ")").list();
        for (Blog blog : blogs) {
            queryBolgUser(blog);
            blog.setIsLike(isLiked(blog.getId()));
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(os);
        // 封装并返回
        return Result.ok(scrollResult);

    }

    private void queryBolgUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        // 是否点赞
        blog.setIsLike(isLiked(blog.getId()));
    }
    private boolean isLiked(Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return false;
        }
        return stringRedisTemplate.opsForZSet()
                .score(BLOG_LIKED_KEY + id, user.getId().toString()) != null;
    }
}
