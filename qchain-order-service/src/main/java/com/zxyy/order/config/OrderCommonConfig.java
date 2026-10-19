package com.zxyy.order.config;

import com.zxyy.common.util.AESGCMUtil;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AESGCMUtil.class)
public class OrderCommonConfig {
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }
}
