package com.faijan.interviewhub.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean RedisCacheManagerBuilderCustomizer questionCacheDefaults() {
        return builder -> builder.withCacheConfiguration("question", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15)).disableCachingNullValues());
    }
}
