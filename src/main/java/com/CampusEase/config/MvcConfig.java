package com.CampusEase.config;

import com.CampusEase.intercepter.LoginIntercepter;
import com.CampusEase.intercepter.RefreshTokenIntercepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ClassName: MvcConfig
 * Package: com.CampusEase.config
 * Description:
 *
 * @Author Gush
 * @Create 2024-03-14 16:41
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // token拦截器 后执行 拦截需要登陆的功能
        registry.addInterceptor(new LoginIntercepter())
                .excludePathPatterns(   // 排除不需要登陆就能看的内容
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/voucher/**"
                ).order(1);

        // token刷新拦截器 先执行 拦截所有路径，确保每一次请求都刷新token有效期
        registry.addInterceptor(new RefreshTokenIntercepter(stringRedisTemplate))
                .excludePathPatterns(
                        "/user/code",
                        "/user/login"
                ).order(0);

    }

}
