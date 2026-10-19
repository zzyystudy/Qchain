package com.zxyy.product.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.security.LoginSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 只读取用户服务维护的登录状态，不在商品服务恢复或签发登录状态。 */
@Component
public class LoginSessionStore {
    private static final String LOGIN_KEY_PREFIX = "qchain:user:login:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LoginSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public LoginSession findByUserId(Long userId) {
        String json = redisTemplate.opsForValue().get(LOGIN_KEY_PREFIX + userId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginSession.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("登录状态数据无法解析", exception);
        }
    }
}
