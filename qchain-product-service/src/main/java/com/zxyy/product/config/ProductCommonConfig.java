package com.zxyy.product.config;

import com.zxyy.common.util.AESGCMUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AESGCMUtil.class)
public class ProductCommonConfig {
}
