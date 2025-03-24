package org.jwj.fo.config;

import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.service.ISeckillVoucherService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Configuration
public class RedisConfig {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://49.235.142.165:6379").setPassword("123456");
        return Redisson.create(config);
    }
    @PostConstruct
    public void initVoucherStock() {
        // 将数据库中的库存同步到Redis
        List<SeckillVoucher> vouchers = seckillVoucherService.list();
        for (SeckillVoucher voucher : vouchers) {
            stringRedisTemplate.opsForValue().set(
                    "seckill:stock:" + voucher.getVoucherId(),
                    voucher.getStock().toString()
            );
        }
    }
}
