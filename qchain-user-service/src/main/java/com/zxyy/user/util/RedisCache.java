package com.zxyy.user.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.security.LoginSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 用户服务的登录缓存，使用固定类型的 JSON，不启用动态类型反序列化。 */
@Component
public class RedisCache {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void setCacheObject(String key, LoginSession session, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("登录信息序列化失败", exception);
        }
    }

    public LoginSession getCacheObject(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginSession.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("登录信息反序列化失败", exception);
        }
    }

    public boolean deleteObject(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
