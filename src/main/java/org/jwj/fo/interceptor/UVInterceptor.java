package org.jwj.fo.interceptor;

import org.checkerframework.checker.units.qual.A;
import org.jwj.fo.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UVInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public UVInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long id = UserHolder.getUser().getId();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern(":yyyyMMdd"));
        stringRedisTemplate.opsForHyperLogLog().add("uv" + today, id.toString());
        return true;
    }
}
