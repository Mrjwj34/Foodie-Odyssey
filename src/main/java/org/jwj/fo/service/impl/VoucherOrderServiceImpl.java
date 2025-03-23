package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.entity.VoucherOrder;
import org.jwj.fo.mapper.VoucherOrderMapper;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.service.IVoucherOrderService;
import org.jwj.fo.utils.RedisIdWorker;
import org.jwj.fo.utils.SimpleRedisLock;
import org.jwj.fo.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.jwj.fo.utils.RedisConstants.KEY_PREFIX;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result secKillVoucher(Long voucherId, String serviceType) {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 未开始
            return Result.fail("秒杀未开始");
        }
        // 判断是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已结束
            return Result.fail("秒杀已结束");
        }
        // 判断是否有库存
        if (voucher.getStock() <= 0) {
            // 已结束
            return Result.fail("库存不足");
        }
        // 获取锁
        Long userId = UserHolder.getUser().getId();
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, serviceType + userId);
        if (!simpleRedisLock.tryLock(5)) {
            return Result.fail("请勿重复购买");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);// 获取代理对象
        } finally {
            simpleRedisLock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 判断是否多次购买
        Long id = UserHolder.getUser().getId();
        long count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("不能重复购买");
        }
        // 判断库存是否充足
        // 扣减库存
        boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!isSuccess) {
            return Result.fail("库存不足");
        }
        // 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        // 写入订单
        save(voucherOrder);
        // 返回结果
        return Result.ok(orderId);
    }
}
