package com.hmdp.intercepter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: LoginIntercepter
 * Package: com.hmdp.intercepter
 * Description:
 *
 * @Author Gush
 * @Create 2024-03-14 16:21
 */
public class LoginIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否需要拦截(ThreadLocal中是否有用户)
        // 从ThreadLocal中获取用户，判断是否存在
        UserDTO user = UserHolder.getUser();
        if(user == null) {
            response.setStatus(401);
            return false;
        }
        // 有用户，则放行
        return true;
    }

}
