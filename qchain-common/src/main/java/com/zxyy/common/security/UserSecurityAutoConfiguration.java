package com.zxyy.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({OncePerRequestFilter.class, StringRedisTemplate.class, SecurityFilterChain.class})
@EnableConfigurationProperties(GatewayIdentityProperties.class)
@EnableMethodSecurity
public class UserSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LoginSessionStore loginSessionStore(StringRedisTemplate redis, ObjectMapper mapper) {
        return new LoginSessionStore(redis, mapper);
    }

    @Bean
    public UserContextFilter userContextFilter(GatewayIdentityProperties properties, LoginSessionStore sessions,
                                                ObjectMapper mapper) {
        return new UserContextFilter(properties, sessions, mapper);
    }

    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration(UserContextFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
