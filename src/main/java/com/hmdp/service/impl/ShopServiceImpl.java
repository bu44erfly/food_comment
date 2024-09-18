package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CheckUtil;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate template ;

    @Resource
    private CheckUtil checkUtil;

    private boolean tryLock(String key) {
        Boolean flag = template.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key) {
        template.delete(key);
    }

    @Override
    public Result queryById(Long id) {
     //   Shop shop = queryWithPassThrough(id) ;
        Shop shop = queryWithMutex(id);
        if(shop == null){
            return Result.fail("id不存在") ;
        }
        return Result.ok(shop);
    }

    /**
     *  互斥锁
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id) {

        Shop cacheshop= (Shop) template.opsForValue().get(CACHE_SHOP_KEY + id);

        String key  = CACHE_SHOP_KEY + id;
        if (!checkUtil.checkWithBloomFilter("whitelistShop", key)) {
            log.info("白名单没有此信息，不可以访问" + key);
            return null;
        }

        if(cacheshop!=null){
            if(-1L==cacheshop.getId())
                return null;
            return cacheshop ;
        }

        String lockKey = LOCK_SHOP_KEY + id;

        try {
            while(tryLock(lockKey) ==false){
                Thread.sleep(50);
            }
            //成功 通过id查询数据库
            Shop shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);
            if (shop == null) {
                //redis写入空值
                template.opsForValue().set(CACHE_SHOP_KEY+id,new Shop(-1L),
                        RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES) ;
                return null;
            }
            //数据库存在 写入redis
            template.opsForValue().set(CACHE_SHOP_KEY+id,
                    shop, CACHE_SHOP_TTL, TimeUnit.MINUTES);

            return shop;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }

    }
//    private Shop queryWithPassThrough(Long id) {
//        Shop cacheshop= (Shop) template.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        //解决缓存穿透    ：redis存空值
//        if(cacheshop!=null){
//            if(-1L==cacheshop.getId())
//                return null;
//            return cacheshop ;
//        }
//
//
//        Shop shop=this.getById(id) ;
//        if(shop==null){
//            template.opsForValue().set(CACHE_SHOP_KEY+id,new Shop(-1L),
//                    RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES) ; //
//
//            return null ;
//        }
//
//        template.opsForValue().set(CACHE_SHOP_KEY+id, shop, CACHE_SHOP_TTL, TimeUnit.MINUTES) ; //
//        return shop ;
//    }
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        template.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
