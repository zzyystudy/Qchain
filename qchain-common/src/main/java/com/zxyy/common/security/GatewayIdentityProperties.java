package com.zxyy.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("qchain.security")
public class GatewayIdentityProperties {
    /** 网关与微服务之间的身份转发凭据；空值不接受转发身份。 */
    private String gatewayToken = "";
}
