package com.hmdp.utils;
import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenIntercepter implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenIntercepter.class);
    private RedisTemplate template ;

    public RefreshTokenIntercepter(RedisTemplate template) {
        this.template = template;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //   String token =(String) request.getSession().getAttribute("user");
        String token = request.getHeader("authorization");

        log.info("token:{}", token);
        if(token==null||token.isBlank()){
            return true ;
        }
        Map<String ,Object> map =
                template.opsForHash().entries(LOGIN_USER_KEY +token) ;
        if(map.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.mapToBean(map, UserDTO.class,false) ;
        //
        UserHolder.saveUser(userDTO);
        //刷新token 有效期
        template.expire(LOGIN_USER_KEY +token , LOGIN_USER_TTL , TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}

