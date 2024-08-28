package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate template;

    @Override
    public Result queryList() {
        String key = RedisConstants.CACHE_TYPE_KEY ;
        Long size=template.opsForList().size(key) ;

        if(size!=null &&size >0){
            List<ShopType> list = template.opsForList().range(key, 0, size);
            return Result.ok(list) ;
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();
        template.opsForList().leftPushAll(key, typeList);
        return Result.ok(typeList) ;

    }
}
