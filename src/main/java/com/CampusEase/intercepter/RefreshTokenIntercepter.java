package com.CampusEase.intercepter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.CampusEase.dto.UserDTO;
import com.CampusEase.utils.RedisConstants;
import com.CampusEase.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: RefreshTokenIntercepter
 * Package: com.CampusEase.intercepter
 * Description:
 *
 * @Author Gush
 * @Create 2024-03-17 16:31
 */
public class RefreshTokenIntercepter implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的token
        String token = request.getHeader("authorization");
        // 3. 判断用户是否存在
        if (StrUtil.isBlank(token)) {
            // 4. 不存在，放行，交由下一级拦截器拦截
            return true;
        }
        // 2. 基于token获取redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        if (userMap.isEmpty()) {
            return true;
        }
        // 将查询到的Hash数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 5. 存在，保存用户到ThreadLocal，放行
        UserHolder.saveUser(userDTO);

        // 6. 刷新token有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 防止内存泄露
        UserHolder.removeUser();
    }
}
