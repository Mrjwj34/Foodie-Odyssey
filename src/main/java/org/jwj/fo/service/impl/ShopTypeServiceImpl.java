package org.jwj.fo.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.ShopType;
import org.jwj.fo.mapper.ShopTypeMapper;
import org.jwj.fo.service.IShopTypeService;
import org.jwj.fo.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.jwj.fo.utils.RedisConstants.CACHE_SHOP_KEY;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result getList() {
        // 从redis中获取商店种类列表
        String typeListJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY);
        if(!StringUtil.isNullOrEmpty(typeListJson)) {
            List<ShopType> typeList = JSONUtil.toList(typeListJson, ShopType.class);
            // 缓存命中直接返回
            return Result.ok(typeList);
        }
        // 缓存未命中从数据库中查找
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 将数据库中数据写入redis
        String jsonStr = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY, jsonStr);
        // 返回数据
        return Result.ok(typeList);
    }
}
