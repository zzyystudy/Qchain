package com.zxyy.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.zxyy.api.client.ProductClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients(clients = ProductClient.class)
@EnableScheduling
public class OrderApplication {
    public static void main(String[] args){
        SpringApplication.run(OrderApplication.class, args);
    }
}
