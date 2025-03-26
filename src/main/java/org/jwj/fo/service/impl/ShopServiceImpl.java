package org.jwj.fo.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.jwj.fo.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
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
        // 缓存穿透 Shop shop = queryWithPassThrough(id);
        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        Shop shop;
        if(id == -1) {
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
            // Thread.sleep(200);
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 判断是否需要坐标查询
        if(x == null || y == null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page);
        }
        // 计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 查询redis, 按照距离排序, 分页, 结果: shopId, distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key, GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        List<String> ids = new ArrayList<>(content.size());
        Map<String, Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(geo -> {
            // 根据id查询shop
            String shopId = geo.getContent().getName();
            Distance distance = geo.getDistance();
            Shop shop = getById(Long.parseLong(shopId));
            // 返回
            ids.add(shop.getId().toString());
            distanceMap.put(shopId, distance);
        });
        if (ids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 根据id查询shop, 且保证有序
        List<Shop> shops = query().in("id", ids)
                .last("order by field(id, " + StringUtil.join(",", ids) + ")").list();
        shops.forEach(shop -> shop.setDistance(distanceMap.get(shop.getId().toString()).getValue()));
        // 返回
        return Result.ok(shops);
    }
}
