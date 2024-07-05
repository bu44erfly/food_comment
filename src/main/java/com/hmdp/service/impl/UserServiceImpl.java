package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate template;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone) ==false)
            return Result.fail("格式不正确") ;

        String code0=RandomUtil.randomString(6) ;
       // session.setAttribute("code",code0);
        template.opsForValue().set(LOGIN_CODE_KEY+phone ,code0,
                LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("已发送短信验证码 " +code0);

        return Result.ok() ;
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String number = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(number) ==false){
            return Result.fail("格式不正确") ;
        }

        //获取验证码，并验证
        String FormCode = loginForm.getCode();
        String cacheCode =(String) template .opsForValue().get(LOGIN_CODE_KEY+ number) ;

        if(cacheCode==null ||FormCode.equals(cacheCode) == false){
            return Result.fail("验证码匹配失败") ;
        }


//        LambdaQueryWrapper<User>  wrapper= new LambdaQueryWrapper<>();
//        wrapper.eq(User::getPhone,number);
   //    User user =getOne(wrapper) ;
        User user= query().eq("phone",number).one() ;
//        User user = baseMapper
//                .selectOne(new LambdaQueryWrapper<User>()
//                        .eq(User::getPhone, number));
        if(user ==null)
            user =  createUserByNumber(number) ;

        String token = UUID.randomUUID().toString() ;
        //save userInfo

        UserDTO userDTO =BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> map = BeanUtil.beanToMap(userDTO) ;
       //  session.setAttribute("user", token);
        template.opsForHash().putAll(LOGIN_USER_KEY +token ,map);
        template.expire(LOGIN_USER_KEY +token , LOGIN_USER_TTL ,TimeUnit.MINUTES);
        return Result.ok(token) ;
    }

    public User createUserByNumber(String number){
        User user = new User();
        user.setPhone(number);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
