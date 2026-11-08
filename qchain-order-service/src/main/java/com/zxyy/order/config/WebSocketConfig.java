package com.zxyy.order.config;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig  extends ServerEndpointConfig.Configurator
        implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    // Spring 启动时自动调用，把容器交给我们
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    // WebSocket 连接时调用，从容器获取已完成依赖注入的端点
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        if (context == null) {
            throw new IllegalStateException("Spring 容器尚未初始化");
        }
        return context.getBean(endpointClass);
    }


    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}