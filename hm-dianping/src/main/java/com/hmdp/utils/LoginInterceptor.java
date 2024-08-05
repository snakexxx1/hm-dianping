package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

  /*  private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }*/

    //拦截器放行前逻辑
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取session
        //HttpSession session = request.getSession();
        //2.获取session中的用户
        // Object user = session.getAttribute("user");

       /* //1.获取请求头中的token
        String token = request.getHeader("authorization");
        //StrUtil.isBlank判断是否为空
        if (StrUtil.isBlank(token)) {
            //不存在，拦截
            response.setStatus(401);
            return  false;
        }

        //2.基于redis获取redis中的用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        //3.判断用户是否存在
        if (userMap.isEmpty()) {
            //4.不存在，拦截
            response.setStatus(401);
            return  false;
        }
        //5.将查询到的Hash数据转为UserDto对象
        UserDTO userDTO  = BeanUtil.fillBeanWithMap(userMap,new UserDTO() , false);
        //6.存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //7.刷新token有效期
        stringRedisTemplate.expire(key , RedisConstants.CACHE_SHOP_TTL , TimeUnit.MINUTES);
        //7.放行
        return true;*/


        // 1.判断是否需要拦截（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            // 没有，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
        return true;

    }

/*    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       //移除用户
        UserHolder.removeUser();
    }*/
}
