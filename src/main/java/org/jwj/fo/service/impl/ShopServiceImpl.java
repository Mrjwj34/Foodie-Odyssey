package org.jwj.fo.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.Shop;
import org.jwj.fo.mapper.ShopMapper;
import org.jwj.fo.service.IShopService;
import org.jwj.fo.utils.CacheClient;
import org.jwj.fo.utils.RedisConstants;
import org.jwj.fo.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.jwj.fo.utils.RedisConstants.*;

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    CacheClient client;
    @Override
    public Result queryById(Long id) {
        //缓存穿透 Shop shop = queryWithPassThrough(id);
        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        Shop shop;
        if(id == 1) {
            log.info("热点key被查询");
            shop = client.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        } else {
            log.info("普通key被查询");
            shop = client.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        }
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存是否存在
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                return null; // 代表之前查询过，数据库里也没有
            }
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 实现缓存重建
        // 获取互斥锁
        String lockkey = LOCK_SHOP_KEY + id;
        Shop shop;
        try {
            boolean isLock = getLock(lockkey);
            if(!isLock) {
                // 失败进行休眠
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 成功根据id查询数据库
            // 不存在根据id查询数据库
            shop = getById(id);
            // 模拟重建延迟
//            Thread.sleep(200);
            // 数据库不存在则返回空值并写入redis
            if(shop == null){
                // 将空值写入redis，设置较短的过期时间，比如2分钟
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 存在则写入redis并返回
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unlock(id + "");
        }
        return shop;
    }

    public void saveShop2Redis(Long id, Long expireSeconds){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
    private boolean getLock(String key){
        boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Transactional
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
