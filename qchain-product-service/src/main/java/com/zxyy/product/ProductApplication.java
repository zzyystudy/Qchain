package com.zxyy.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.zxyy.api.client.OrderClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients(clients = OrderClient.class)
@EnableScheduling
public class ProductApplication {
    public static void main(String[] args){
        SpringApplication.run(ProductApplication.class, args);
    }
}
