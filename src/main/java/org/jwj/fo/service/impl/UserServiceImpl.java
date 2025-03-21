package org.jwj.fo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.BeanToMapCopier;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.dto.LoginFormDTO;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.User;
import org.jwj.fo.mapper.UserMapper;
import org.jwj.fo.service.IUserService;
import org.jwj.fo.utils.RegexUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.jwj.fo.utils.RedisConstants.*;
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
        return Result.ok();
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

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
