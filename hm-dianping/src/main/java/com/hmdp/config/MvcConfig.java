package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig  implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
   //添加拦截器方法
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                //排除不需要拦截的用户
                // .excludePathPatterns用于指定哪些路径模式应该被排除在外 也就是别的都能访问 这些不行 要拦截
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                ).order(1); //order（1）数字越小越先执行
        //token刷新拦截器  addPathPatterns("/**")用于定义拦截器匹配路径 /** 表示匹配任何路径
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
