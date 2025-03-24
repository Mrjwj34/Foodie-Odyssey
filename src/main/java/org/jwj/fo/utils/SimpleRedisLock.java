package org.jwj.fo.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.jwj.fo.utils.RedisConstants.KEY_PREFIX;

public class SimpleRedisLock implements Ilock {
    private final StringRedisTemplate stringRedisTemplate;
    private final String lockKey;
    private final String id = UUID.randomUUID().toString(true) +
            "-" + Thread.currentThread().getId();
    private static final DefaultRedisScript<Long> unlockScript;
    static {
        unlockScript = new DefaultRedisScript<>();
        unlockScript.setLocation(new ClassPathResource("unlock.lua"));
        unlockScript.setResultType(Long.class);
    }
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String lockKey) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockKey = lockKey;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue() // 注意自动拆箱的空指针风险
                .setIfAbsent(KEY_PREFIX + lockKey, id, timeoutSec, TimeUnit.SECONDS));
    }

//    @Override
//    public void unlock() {
//        // 获取锁中标识
//        String lockValue = stringRedisTemplate.opsForValue().get(KEY_PREFIX + lockKey);
//        if (id.equals(lockValue)) {
//            stringRedisTemplate.delete(KEY_PREFIX + lockKey);
//        }
    @Override
    public void unlock() {
        //调用lua脚本删除key, 保证原子性
        stringRedisTemplate.execute(unlockScript,
                Collections.singletonList(KEY_PREFIX + lockKey), id);
    }
}
