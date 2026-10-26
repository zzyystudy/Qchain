package com.zxyy.order;

import com.zxyy.api.client.ProductClient;
import com.zxyy.order.listener.DeliveryItemListener;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrderApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:order-boot;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.sql.init.mode=always", "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.listener.direct.auto-startup=false",
        "shop.aes.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "qchain.internal.token=boot-test-token",
        "spring.mail.username=sender@example.com", "spring.mail.host=localhost"
})
class OrderApplicationContextTest {
    @Autowired ApplicationContext context;

    @Test void applicationWiresOrderListenersFeignAndOkHttpWithoutExternalInfrastructure() {
        assertThat(context.getBean(ProductClient.class)).isNotNull();
        assertThat(context.getBean(OkHttpClient.class).connectionPool()).isNotNull();
        assertThat(context.getBean(DeliveryItemListener.class)).isNotNull();
    }
}
