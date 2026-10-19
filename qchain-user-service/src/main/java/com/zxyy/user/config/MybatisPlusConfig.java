package com.zxyy.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.zxyy.user.mapper")
public class MybatisPlusConfig {
}
