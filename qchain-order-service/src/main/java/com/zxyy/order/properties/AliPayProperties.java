package com.zxyy.order.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "shop.alipay")
public class AliPayProperties {
    private String appId;
    private String appPrivateKey;
    private String charset = "UTF-8";
    private String alipayPublicKey;
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String format = "json";
    private String signType = "RSA2";
    private String notifyUrl;
    private String returnUrl;
}
