package org.jwj.fo.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1735689600L;
    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 32L;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    public long nextId(String keyPrefix) {
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long timeStamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        // 生成序列号
        // 获取当前日期字符串
        String dateStr = now.toLocalDate().toString().replace("-", "");
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + dateStr);

        // 拼接并返回
        return timeStamp << SEQUENCE_BITS | count;
    }


}
