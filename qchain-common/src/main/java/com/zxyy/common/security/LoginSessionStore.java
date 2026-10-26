package com.zxyy.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/** 所有服务共用的登录状态格式，权限以 Redis 中的当前值为准。 */
public class LoginSessionStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public LoginSessionStore(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public LoginSession findByUserId(Long userId) {
        String json = redis.opsForValue().get(IdentityHeaders.LOGIN_KEY_PREFIX + userId);
        if (json == null) return null;
        try {
            return mapper.readValue(json, LoginSession.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("登录状态数据无法解析", e);
        }
    }

    public void save(LoginSession session, Duration ttl) {
        try {
            redis.opsForValue().set(IdentityHeaders.LOGIN_KEY_PREFIX + session.userId(),
                    mapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("登录信息序列化失败", e);
        }
    }

    public void deleteByUserId(Long userId) {
        redis.delete(IdentityHeaders.LOGIN_KEY_PREFIX + userId);
    }
}
