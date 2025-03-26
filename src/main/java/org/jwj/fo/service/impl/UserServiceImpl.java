package org.jwj.fo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.BeanToMapCopier;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.dto.LoginFormDTO;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.entity.User;
import org.jwj.fo.mapper.UserMapper;
import org.jwj.fo.service.IBlogService;
import org.jwj.fo.service.IUserService;
import org.jwj.fo.utils.RegexUtils;
import org.jwj.fo.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.jwj.fo.utils.RedisConstants.*;
import static org.jwj.fo.utils.SystemConstants.MAX_PAGE_SIZE;
import static org.jwj.fo.utils.SystemConstants.USER_NICK_NAME_PREFIX;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //不符合返回错误信息
            return Result.fail("手机号格式错误");
        }
        //符合生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码和手机号到redis
//        session.setAttribute("code", code);
//        session.setAttribute("phone", phone);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("send success: {}", code);
        //返回ok
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 提交手机号和验证码
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        String codeByPhone = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(RegexUtils.isPhoneInvalid(phone)){
            //不符合返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 校验验证码
        if(code == null || !code.equals(codeByPhone)){
            return Result.fail("验证码错误");
        }
        // 根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 用户不存在创建新用户并保存到数据库
        if(user == null) {
            user = createUserWithPhone(phone);
        }
        // 先删除该用户的旧token
        String phoneKey = LOGIN_PHONE_KEY + phone;
        String oldToken = stringRedisTemplate.opsForValue().get(phoneKey);
        if (oldToken != null) {
            stringRedisTemplate.delete(LOGIN_USER_KEY + oldToken);
        }
        //保存用户到redis
        String uuid = UUID.randomUUID().toString();
        // 保存电话到uuid的映射
        stringRedisTemplate.opsForValue().set(phoneKey, uuid, LOGIN_USER_TTL, TimeUnit.SECONDS);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        Map<String, String> stringMap = userDTO.toMap();
        String key = LOGIN_USER_KEY + uuid;
        stringRedisTemplate.opsForHash().putAll(key, stringMap);
        //设置有效期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.SECONDS);
        return Result.ok(uuid);
    }

    @Override
    public Result logout() {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请登录后再退出");
        }
        Long id = user.getId();
        String phone = getById(id).getPhone();
        String token = stringRedisTemplate.opsForValue().get(LOGIN_PHONE_KEY + phone);
        // 删除redis中的电话-uuid映射
        stringRedisTemplate.delete(LOGIN_PHONE_KEY + phone);
        // 删除redis中的用户信息
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok();
    }

    @Override
    public Result queryUserById(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        UserDTO UserDTO = BeanUtil.copyProperties(user, org.jwj.fo.dto.UserDTO.class);
        return Result.ok(UserDTO);
    }

    @Override
    public Result sign() {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        // 获取日期
        LocalDate date = LocalDate.now();
        String suffix = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        // 拼接key
        String key = USER_SIGN_KEY + user.getId() + ":" + suffix;
        // 获取今天是第几天
        int day = date.getDayOfMonth() - 1;
        // 写入redis setbit key offset 1
        Boolean bit = stringRedisTemplate.opsForValue().setBit(key, day, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        // 获取日期
        LocalDate date = LocalDate.now();
        String suffix = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        // 拼接key
        String key = USER_SIGN_KEY + user.getId() + ":" + suffix;
        // 获取今天是第几天
        int day = date.getDayOfMonth() - 1;
        // 获取本月截取今天为止的所有签到记录, 返回一个十进制数字
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day))
                        .valueAt(0));
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 循环遍历
        int count = 0;
        while(true) {
            if ((num & 1) == 0) {
                // 与1做与运算，结果为0，未签到，结束
                break;
            } else {
                // 如果结果为1, 已签到, 计数器加一
                count++;
            }
            // 把数字右移一位, 抛弃最后一位
            num >>>= 1;
        }
        return Result.ok(count);
    }


    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
