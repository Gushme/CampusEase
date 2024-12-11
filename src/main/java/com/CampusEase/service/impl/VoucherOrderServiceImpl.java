package com.CampusEase.service.impl;

import com.CampusEase.dto.Result;
import com.CampusEase.entity.SeckillVoucher;
import com.CampusEase.entity.VoucherOrder;
import com.CampusEase.mapper.VoucherOrderMapper;
import com.CampusEase.service.ISeckillVoucherService;
import com.CampusEase.service.IVoucherOrderService;
import com.CampusEase.utils.SimpleRedisLock;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CampusEase.utils.RedisIdWorker;
import com.CampusEase.utils.UserHolder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private RateLimiter rateLimiter= RateLimiter.create(50);

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Transactional
    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        //1.所有信息从当前消息实体中拿
        Long voucherId = voucherOrder.getVoucherId();
        //2.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                // 判断当前库存是否大于0就可以决定是否能抢池子中的券了
                .gt("stock", 0)
                .update();
        //3.创建订单
        if(success)
            save(voucherOrder);
    }


    private IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //令牌桶算法 限流
        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)){
            return Result.fail("目前网络正忙，请重试");
        }

        Long userID = UserHolder.getUser().getId();
        // 1. 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userID.toString()
        );
        // 2. 判断结果是否为0
        if(result.intValue() != 0) {
            // 3. 不为0，代表没有购买资格
            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
        }
        long orderId = redisIdWorker.nextId("order");
        // 4. 为0，有购买资格，把下单信息保存到 rabbitmq消息队列
        // 4.1 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 4.2 订单id
        voucherOrder.setId(orderId);
        // 4.3 用户id
        voucherOrder.setUserId(userID);
        // 4.4 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 存入 rabbitmq消息队列
        rabbitTemplate.convertAndSend("CampusEase.direct", "direct.seckill", voucherOrder);

        return Result.ok(orderId);
    }
/*SET NX 和 Redisson 实现分布式锁代码    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已经结束
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate); // 使用 SET NX EXPIRE 实现分布式锁
        RLock lock = redissonClient.getLock("lock:order:" + userId); // 使用 Redisson 实现分布式锁 ， 锁的key是用户id，每个用户一把锁
        // 获取锁
        boolean success = lock.tryLock();
        if (!success) {
            Result.fail("不允许重复下单！");
        }
        try {
            // 获取代理对象(事务)，确保事务生效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            simpleRedisLock.unlock();
        }
    }*/

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //5，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0) // 用乐观锁解决库存超卖 where id = ? and stock > 0
                .update();
        if (!success) {
            //扣减库存
            return Result.fail("库存不足！");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2.用户id
        voucherOrder.setUserId(userId);
        // 6.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);

    }
}
