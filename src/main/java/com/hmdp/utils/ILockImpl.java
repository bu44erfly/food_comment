package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ILockImpl implements ILock {

    private String name ;
    private RedisTemplate template;

    public ILockImpl(String name ,RedisTemplate template) {
        this.name = name;
        this.template = template;
    }

    private static final  String KET_PREFIX="lock:";
    private static final  String ID_PREFIX= UUID.randomUUID().toString(true)+"-";;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 超时自动释放锁
     * @return 是否成功获取锁 true成功 false失败
     */
    @Override
    public boolean tryLock(Long timeoutSec) {
        String tid =ID_PREFIX+ Thread.currentThread().getId();
        boolean ok = template.opsForValue().setIfAbsent(KET_PREFIX+name,tid,timeoutSec, TimeUnit.MINUTES);

        return ok;
    }

   @Override
    public void unLock(){
       template.execute(UNLOCK_SCRIPT
               , Collections.singletonList(KET_PREFIX+name)
               ,ID_PREFIX+Thread.currentThread().getId());
    }

//    @Override
//    public void unLock() {
//        String threadId =ID_PREFIX+ Thread.currentThread().getId();
//        //获取锁中标识
//        String id =(String) template.opsForValue().get(KET_PREFIX + name);
//        //判断时候一致
//        if (StringUtils.equals(id,threadId)){
//            //一致 释放锁
//           template.delete(KET_PREFIX + name);
//        }
//        template.delete(KET_PREFIX+name);
//    }
}
