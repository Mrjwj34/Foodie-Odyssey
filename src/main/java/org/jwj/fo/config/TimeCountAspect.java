package org.jwj.fo.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

//@Aspect
//@Component
//@Slf4j
public class TimeCountAspect {
//     @Around("execution(* org.jwj.fo.utils.RedisIdWorker.nextId(..))")
//     @Around("execution(* org.jwj.fo.interceptor.RefreshTokenInterceptor(..))")
//     public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
//         long start = System.currentTimeMillis();
//         Object proceed = joinPoint.proceed();
//         long end = System.currentTimeMillis();
//         log.info(joinPoint.getSignature().getName() + "方法执行时间：" + (end - start) + "ms");
//         return proceed;
//     }
}
