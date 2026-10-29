package com.zxyy.product.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.config.RandomTtlCacheConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisProperties.class)
public class ProductCacheConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            RedisProperties properties,
            ObjectMapper objectMapper) {

        Config config = new Config();

        // 使用 JSON 序列化，并复用 Spring 配置的 Jackson 模块
        config.setCodec(new JsonJacksonCodec(objectMapper.copy()));

        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + properties.getHost()
                        + ":" + properties.getPort())
                .setDatabase(properties.getDatabase())
                .setConnectTimeout(2000)
                .setTimeout(1500)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(16);

        if (StringUtils.hasText(properties.getUsername())) {
            server.setUsername(properties.getUsername());
        }

        if (StringUtils.hasText(properties.getPassword())) {
            server.setPassword(properties.getPassword());
        }

        return Redisson.create(config);
    }

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> caches = new HashMap<>();

        // 演示设置为 30 秒，便于观察过期后的重新加载
        // 两个参数：TTL、最大空闲时间，单位均为毫秒
        // 随机生成 30～40 秒，包含两端

        CacheConfig sharedConfig = new RandomTtlCacheConfig(Duration.ofSeconds(30),
                Duration.ofSeconds(10));

        List.of(
                "qchain:cache:product:list",
                "qchain:cache:category:list",
                "qchain:cache:product:detail"
        ).forEach(name -> caches.put(name, sharedConfig));

        RedissonSpringCacheManager manager =
                new RedissonSpringCacheManager(redissonClient, caches);

        // 允许缓存 null，空 List 也可以正常缓存
        manager.setAllowNullValues(true);

        // 普通缓存失效操作在数据库事务成功提交后执行
        manager.setTransactionAware(true);

        // 只允许使用已注册的缓存名称
        manager.setCacheNames(caches.keySet());

        return manager;
    }
}