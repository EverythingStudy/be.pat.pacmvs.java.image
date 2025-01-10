package cn.staitech.file.config;

import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/5/23 14:41:59
 */
@Configuration
public class GuavaCacheConfig {

    @Bean("cache")
    public com.google.common.cache.Cache<String, Object> guavaCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(100) // 设置最大缓存数量
                .expireAfterWrite(1, TimeUnit.HOURS) // 缓存项在写入后1小时过期
                .build();
    }
}
