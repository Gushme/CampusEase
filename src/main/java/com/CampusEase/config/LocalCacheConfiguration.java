package com.CampusEase.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * ClassName: LocalCacheConfiguration
 * Package: com.CampusEase.config
 * Description:
 *
 * @Author Gush
 * @Create 2024/12/11 19:01
 */
@Configuration
public class LocalCacheConfiguration {
    @Bean("localCacheManager")
    public Cache<String, Object> localCacheManager() {
        return Caffeine.newBuilder()
                //写入或者更新120s后，缓存过期并失效
                .expireAfterWrite(120, TimeUnit.SECONDS)
                // 初始的缓存空间大小
                .initialCapacity(50)
                // 缓存的最大条数，通过 Window TinyLfu算法控制整个缓存大小
                .maximumSize(500)
                //打开数据收集功能
                .recordStats()
                .build();
    }
}
