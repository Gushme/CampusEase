package com.CampusEase.service.impl;

import com.CampusEase.dto.OrderPaymentDTO;
import com.CampusEase.dto.Result;
import com.CampusEase.entity.SeckillVoucher;
import com.CampusEase.entity.VoucherOrder;
import com.CampusEase.mapper.VoucherOrderMapper;
import com.CampusEase.service.ISeckillVoucherService;
import com.CampusEase.service.IVoucherOrderService;
import com.CampusEase.utils.RedisConstants;
import com.CampusEase.utils.SimpleRedisLock;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CampusEase.utils.RedisIdWorker;
import com.CampusEase.utils.UserHolder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Objects;
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
    private RedisTemplate redisTemplate;
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

        long currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long userID = UserHolder.getUser().getId();
        try {
            // 1. 执行lua脚本
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userID.toString(),
                    String.valueOf(currentTime)
            );
            switch (Objects.requireNonNull(result).intValue()) {
                case 0:
                    // 2.秒杀成功，发送消息到MQ
                    long orderId = redisIdWorker.nextId("order");
                    VoucherOrder voucherOrder = new VoucherOrder();
                    voucherOrder.setId(orderId);
                    voucherOrder.setUserId(userID);
                    voucherOrder.setVoucherId(voucherId);
                    rabbitTemplate.convertAndSend("CampusEase.direct", "direct.seckill", voucherOrder);
                    // 返回订单id
                    return Result.ok(orderId);
                case 1:
                    return Result.fail("Redis缺少数据");
                case 2:
                    return Result.fail("秒杀尚未开始");
                case 3:
                    return Result.fail("秒杀已经结束");
                case 4:
                    return Result.fail("库存不足");
                case 5:
                    return Result.fail("重复购买");
                default:
                    return Result.fail("未知错误");
            }
        } catch (Exception e) {
            log.error("处理订单异常", e);
            return Result.fail("未知错误");
        }
    }
    // Redisson(SET NX) 实现分布式锁，解决限购问题
    @Override
    public Result limitVoucher(Long voucherId) {
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
        // SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate); // 使用 SET NX EXPIRE 实现分布式锁
        RLock lock = redissonClient.getLock("lock:order:" + userId); // 方案一：使用 Redisson 实现分布式锁，锁的key是用户id，每个用户一把锁
        // RLock lock = redissonClient.getLock("lock:order:" + voucherId); // 方案二：锁的粒度更大
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
            lock.unlock();
        }
    }

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

    /*
    * 支付方法
    * */
    @Override
    public Result payment(OrderPaymentDTO orderPaymentDTO) {
        Long orderId = orderPaymentDTO.getOrderId();

        // 1.查询redis中是否存在此订单
        boolean isRedisExist = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisConstants.SECKILL_ORDER_KEY, orderId));
        // 2.查询mysql中是否有此订单
        Long userId = UserHolder.getUser().getId();
        VoucherOrder voucherOrder = this.getOne(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getId, orderId));

        if (isRedisExist) {
            // 2. 秒杀订单业务流程
            if (voucherOrder == null) {
                //2.1 数据库不存在订单，等待后重试
                try {
                    Thread.sleep(1000);
                    return payment(orderPaymentDTO);
                } catch (Exception e) {
                    return Result.fail("未知错误");
                }
            }
        } else {
            // 3.普通、限购订单业务流程
            if (voucherOrder == null) {
                //3.1 数据库不存在订单，返回错误信息
                return Result.fail("订单不存在");
            }
        }
        // 此时已经在mysql查询到订单信息
        // TODO 3.进入付款流程
        return Result.ok();
    }
}
