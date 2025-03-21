package org.jwj.fo.interceptor;

import cn.hutool.core.bean.BeanUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.User;
import org.jwj.fo.utils.RedisConstants;
import org.jwj.fo.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否ThreadLocal是否存在User
        if(UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        //放行
        return true;
    }
}
