package org.jwj.fo.config;

import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.entity.Shop;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.service.IShopService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jwj.fo.utils.RedisConstants.SHOP_GEO_KEY;

@Configuration
public class RedisConfig {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IShopService shopService;
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://172.20.249.104:6379").setPassword("123456");
        return Redisson.create(config);
    }
    @PostConstruct
    public void initVoucherStock() {
        // 将数据库中的库存与开始结束时间同步到Redis
        List<SeckillVoucher> vouchers = seckillVoucherService.list();
        for (SeckillVoucher voucher : vouchers) {
            Map<String, String> map = new HashMap<>();
            map.put("stock", voucher.getStock().toString());
            map.put("beginTime", voucher.getBeginTime().toString());
            map.put("endTime", voucher.getEndTime().toString());
            stringRedisTemplate.opsForHash()
                    .putAll("seckill:voucher:" + voucher.getVoucherId(), map);
        }
    }
    @PostConstruct
    public void initShopData() {
        // 查询店铺信息
        List<Shop> shops = shopService.list();
        // 把店铺按typeId分组
        Map<Long, List<Shop>> shopMap = shops.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));
        // 分批完成写入Redis
        shopMap.forEach((typeId, shopList) -> {
            List<RedisGeoCommands.GeoLocation<String>> locations = shopList.stream()
                    .map(shop -> new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),
                            new Point(shop.getX(), shop.getY())))
                    .collect(Collectors.toList());
            // 写入Redis, GEOADD key 经度 纬度 member
            stringRedisTemplate.opsForGeo()
                    .add(SHOP_GEO_KEY + typeId, locations);
        });
    }
}
