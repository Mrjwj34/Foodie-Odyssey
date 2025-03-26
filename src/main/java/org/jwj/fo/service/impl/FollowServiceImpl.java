package org.jwj.fo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.Follow;
import org.jwj.fo.entity.User;
import org.jwj.fo.mapper.FollowMapper;
import org.jwj.fo.service.IFollowService;
import org.jwj.fo.service.IUserService;
import org.jwj.fo.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jwj.fo.utils.RedisConstants.FOLLOW_KEY;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IUserService userService;

    @Override
    public Result follow(Long id, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        // 判断是关注还是取关
        if(isFollow){
            // 关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            boolean isSuccess = save(follow);
            if(isSuccess) {
                // 将关注信息存储到redis集合中
                stringRedisTemplate.opsForSet().add(FOLLOW_KEY + userId, id.toString());
            }
        } else {
            // 取关
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", id));
            if(isSuccess) {
                // 将关注信息从redis集合中删除
                stringRedisTemplate.opsForSet().remove(FOLLOW_KEY + userId, id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        Follow follow = getOne(new QueryWrapper<Follow>()
                .eq("user_id", userId)
                .eq("follow_user_id", id));
        return Result.ok(follow != null);
    }

    @Override
    public Result commonFollow(Long id) {
        // 获取当前用户
        Long currentUserId = UserHolder.getUser().getId();
        // 从redis中求当前用户的关注列表与目标用户关注列表的交集
        Set<String> ids = stringRedisTemplate.opsForSet()
                        .intersect(FOLLOW_KEY + currentUserId, FOLLOW_KEY + id);
        if (ids == null || ids.isEmpty()) {
            return Result.ok();
        }

        List<Long> intersect = ids.stream().map(Long::valueOf).collect(Collectors.toList());
        // 解析id
        List<UserDTO> results = userService.listByIds(intersect).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(results);
    }
}
