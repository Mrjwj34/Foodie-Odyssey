package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.entity.Voucher;
import org.jwj.fo.mapper.VoucherMapper;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jwj.fo.utils.RedisConstants.SECKILL_STOCK_KEY;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存优惠券到redis
        Map<String, String> map = new HashMap<>();
        map.put("stock", voucher.getStock().toString());
        map.put("beginTime", voucher.getBeginTime().toString());
        map.put("endTime", voucher.getEndTime().toString());
        stringRedisTemplate.opsForHash().putAll("seckill:voucher:" + voucher.getId(), map);
    }

}
