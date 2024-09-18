package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.CuratorConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rebbitmq.MQSender;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;

import com.hmdp.utils.ILockImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.ZookeeperLock;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;



/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private  ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate redisTemplate  ;

    @Resource
    private MQSender mqSender;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    @Override
    public Result seckillvoucher(Long voucherId)  {
        SeckillVoucher voucher=  seckillVoucherService.getById(voucherId) ;

        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("活动未开始");
        }

        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("活动已结束");
        }
        if(voucher.getStock()<=0){
            return Result.fail("数量不足") ;
        }

        // 3、创建订单（使用分布式锁）
        Long userId = UserHolder.getUser().getId();
      //  ILockImpl lock = new ILockImpl(redisTemplate, "order:" + userId);

        CuratorFramework client = CuratorConfig.createCuratorClient();
        String lockPath = "/locks";
        ZookeeperLock lock =new ZookeeperLock(client ,lockPath) ;
        boolean isLock = false;
        try {
            isLock = lock.tryLock();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!isLock) {
            // 索取锁失败，重试或者直接抛异常（这个业务是一人一单，所以直接返回失败信息）
            return Result.fail("一人只能下一单");
        }
        try {
            // 索取锁成功，创建代理对象，使用代理对象调用第三方事务方法， 防止事务失效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(userId, voucherId);
        } finally {
            try {
                lock.unLock(); //
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 创建订单
     * @param userId
     * @param voucherId
     * @return
     */
    @Transactional
    public Result createVoucherOrder(Long userId, Long voucherId){
        //2.3创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.4订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.5用户id
        voucherOrder.setUserId(userId);
        //2.6代金卷id
        voucherOrder.setVoucherId(voucherId);

        //2.7将信息放入MQ中
        mqSender.sendSeckillMessage(JSON.toJSONString(voucherOrder));

        return Result.ok(orderId);
    }
}
