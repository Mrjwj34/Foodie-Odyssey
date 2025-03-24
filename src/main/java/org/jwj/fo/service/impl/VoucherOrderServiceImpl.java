package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.entity.VoucherOrder;
import org.jwj.fo.mapper.VoucherOrderMapper;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.service.IVoucherOrderService;
import org.jwj.fo.utils.RedisIdWorker;
import org.jwj.fo.utils.SimpleRedisLock;
import org.jwj.fo.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jwj.fo.utils.RedisConstants.KEY_PREFIX;
import static org.jwj.fo.utils.RedisConstants.LOCK_SERVICE_ORDER_PREFIX;


@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private IVoucherOrderService proxy;
    private static final DefaultRedisScript<Long> seckillScript;
    static {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("seckill.lua"));
        seckillScript.setResultType(Long.class);
    }
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService seckillOrderExecutor = Executors.newSingleThreadExecutor();
    @PostConstruct
    private void init() {
        seckillOrderExecutor.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 保存订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("订单处理失败", e);
                }
            }
        }
    }
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock simpleRedisLock = redissonClient.getLock(KEY_PREFIX + LOCK_SERVICE_ORDER_PREFIX + userId);
        if (!simpleRedisLock.tryLock()) {// 防止分布式环境下重复购买
            log.error("请勿重复购买");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);// 获取代理对象
        } finally {
            simpleRedisLock.unlock();
        }
    }

    @Override
    public Result secKillVoucher(Long voucherId) {
        this.proxy = (IVoucherOrderService) AopContext.currentProxy();
        Long id = UserHolder.getUser().getId();
        // 执行lua脚本, redis的单线程机制保证了原子性
        int result = Objects.requireNonNull(stringRedisTemplate.execute(
                seckillScript,
                Collections.emptyList(),
                voucherId.toString(), id.toString()
        )).intValue();
        // 判断结果是否为0
        if(result != 0) {
            // 不为0代表没有购买资格
            return Result.fail(result == 2 ? "请勿重复购买" : "库存不足");
        }
        // 为0则有购买资格, 把下单信息写入消息队列
        long orderId = redisIdWorker.nextId("order");
        // TODO: 保存订单信息到消息队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        orderTasks.add(voucherOrder);
        // 返回订单id
        return Result.ok(orderId);
    }
//    @Override
//    public Result secKillVoucher(Long voucherId, String serviceType) {
//        // 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 判断是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 未开始
//            return Result.fail("秒杀未开始");
//        }
//        // 判断是否结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            // 已结束
//            return Result.fail("秒杀已结束");
//        }
//        // 判断是否有库存
//        if (voucher.getStock() <= 0) {
//            // 已结束
//            return Result.fail("库存不足");
//        }
//        // 获取锁
//        Long userId = UserHolder.getUser().getId();
////        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, serviceType + userId);
//        RLock simpleRedisLock = redissonClient.getLock(KEY_PREFIX + serviceType + userId);
//        if (!simpleRedisLock.tryLock()) {
//            return Result.fail("请勿重复购买");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);// 获取代理对象
//        } finally {
//            simpleRedisLock.unlock();
//        }
//    }

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
    @Transactional
    public Result createVoucherOrder(VoucherOrder voucherOrder) {
        boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                .update();
        if (!isSuccess) {
            log.error("库存扣减失败，voucherId:{}", voucherOrder.getVoucherId());
            return Result.fail("下单失败");
        }
        save(voucherOrder);
        // 返回结果
        return Result.ok(voucherOrder.getVoucherId());
    }
}
