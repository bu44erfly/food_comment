package com.hmdp.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * @author 晓风残月Lx
 * @date 2023/3/29 17:05
 * 布隆过滤器白名单初始化工具类，一开始就设置一部分数据为白名单所有
 * 白名单业务默认规定：布隆过滤器有，redis 有可能有   布隆过滤器无 redis一定无
 * 白名单业务默认规定：whitelistCustomer
 */
@Component
@Slf4j
public class WhiteList_init {

    @Resource
    private RedisTemplate redisTemplate;

    @PostConstruct // 初始化白名单数据
    public void init() {
        // 1.白名单客户加载到布隆过滤器
        List<Long> list = new ArrayList<>();
        for (long i = 1; i <= 10; i++)
            list.add(i);

        for (Long shopId : list) {
            String key = CACHE_SHOP_KEY +shopId;

            // 2.计算hashValue，由于存在计算出来负数的可能，取绝对值
            int v = Math.abs(key.hashCode());
            // 3.通过hashValue 和 2的32次方后取余，获得对应的下标坑位
            long index = (long) (v % Math.pow(2, 32));
            log.info(key + "对应的坑位 index： {}", index);
            // 4.设置redis里面的bitmap对应类型的坑位，将该值设置为1
            redisTemplate.opsForValue().setBit("whitelistCustomer", index, true);
        }
    }
}
