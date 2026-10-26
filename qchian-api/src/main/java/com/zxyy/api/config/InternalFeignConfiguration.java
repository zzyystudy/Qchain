package com.zxyy.api.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** 仅由指定的 Feign Client 加载，后台 MQ/定时任务也可以调用内部接口。 */
public class InternalFeignConfiguration {
    public static final String TOKEN_HEADER = "X-Qchain-Internal-Token";

    @Bean
    public RequestInterceptor internalTokenInterceptor(@Value("${qchain.internal.token:}") String token) {
        return template -> {
            if (template.url().startsWith("/internal/")) {
                template.header(TOKEN_HEADER, token);
            }
        };
    }
}
