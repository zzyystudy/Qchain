package com.zxyy.common.security;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;

/** 同步 Feign 调用继续传递当前用户；MQ、定时任务没有用户上下文时不附加身份。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestInterceptor.class)
@EnableConfigurationProperties(GatewayIdentityProperties.class)
public class UserFeignAutoConfiguration {
    @Bean
    public RequestInterceptor userIdentityInterceptor(GatewayIdentityProperties properties) {
        return template -> {
            template.removeHeader(IdentityHeaders.USER_ID);
            template.removeHeader(IdentityHeaders.GATEWAY_TOKEN);
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof LoginSession session) {
                String token = properties.getGatewayToken();
                if (token == null || token.isBlank()) throw new IllegalStateException("未配置网关身份转发凭据");
                template.header(IdentityHeaders.USER_ID, session.userId().toString());
                template.header(IdentityHeaders.GATEWAY_TOKEN, token);
            }
        };
    }
}
