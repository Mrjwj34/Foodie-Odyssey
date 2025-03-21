package org.jwj.fo.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.entity.Shop;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.jwj.fo.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CHACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    public <O, ID> O queryWithPassThrough(String keyPrefix, ID id, Class<O> type, Function<ID, O> dbFallback, Long time, TimeUnit unit){
        String key = CACHE_SHOP_KEY + id;
        // 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存是否存在
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                return null; // 代表之前查询过，数据库里也没有
            }
            return JSONUtil.toBean(shopJson, type);
        }
        // 不存在根据id查询数据库
        O o = dbFallback.apply(id);
        // 数据库不存在则返回空值并写入redis
        if(o == null){
            // 将空值写入redis，设置较短的过期时间，比如2分钟
            this.set(key, "", time, unit);
            return null;
        }
        // 存在则写入redis并返回
        this.set(key, o, time, unit);
        return o;
    }
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack,  Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存是否存在
        if (shopJson == null) {
            return null; // 代表之前查询过，数据库里也没有
        }
        // 命中判断过期时间
        // 反序列化json
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 未过期, 返回店铺信息
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        // 已过期需要缓存重建
        // 获取互斥锁
        String lockkey = LOCK_SHOP_KEY + id;
        // 成功获取锁
        boolean isLocked = getLock(lockkey);
        if(isLocked) {
            CHACHE_REBUILD_EXECUTOR.submit(() ->{
                try {
                    R r1 = dbFallBack.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockkey);
                }
            });
        }
        // 开启新线程实现缓存重建
        // 失败获取锁
        // 返回过期的商铺信息
        return r;
    }
    private boolean getLock(String key){
        boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
