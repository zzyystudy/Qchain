package com.zxyy.api;

import com.sun.net.httpserver.HttpServer;
import com.zxyy.api.client.ProductClient;
import com.zxyy.api.dto.product.ProductBatchQueryDTO;
import feign.Client;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FeignOkHttpIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "qchain.internal.token=test-service-token")
class FeignOkHttpIntegrationTest {
    private static final AtomicReference<String> TOKEN = new AtomicReference<>();
    private static final AtomicReference<String> BODY = new AtomicReference<>();
    private static final HttpServer SERVER = startServer();
    @Autowired ProductClient productClient;
    @Autowired FeignClientFactory clientFactory;
    @Autowired OkHttpClient httpClient;
    @Autowired ConnectionPool pool;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("qchain.clients.product-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @Test
    void actualFeignRequestUsesOkHttpPoolAndSerializesSharedContracts() {
        Client transport = clientFactory.getInstance("productClient", Client.class);
        assertThat(transport).isInstanceOf(FeignBlockingLoadBalancerClient.class);
        assertThat(((FeignBlockingLoadBalancerClient) transport).getDelegate()).isInstanceOf(feign.okhttp.OkHttpClient.class);
        assertThat(httpClient.connectionPool()).isSameAs(pool);
        assertThat(httpClient.connectTimeoutMillis()).isEqualTo(3000);
        assertThat(httpClient.readTimeoutMillis()).isEqualTo(10000);
        assertThat(httpClient.dispatcher().getMaxRequests()).isEqualTo(50);
        assertThat(httpClient.dispatcher().getMaxRequestsPerHost()).isEqualTo(50);
        var result = productClient.orderSnapshots(new ProductBatchQueryDTO(List.of("P1")));
        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getData().get(0).getPrice()).isEqualByComparingTo("12.50");
        productClient.orderSnapshots(new ProductBatchQueryDTO(List.of("P1")));
        assertThat(TOKEN.get()).isEqualTo("test-service-token");
        assertThat(BODY.get()).contains("\"productNos\":[\"P1\"]");
        assertThat(pool.connectionCount()).isPositive();
    }

    @AfterAll static void stopServer() { SERVER.stop(0); }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/products/order-snapshots", exchange -> {
                TOKEN.set(exchange.getRequestHeaders().getFirst("X-Qchain-Internal-Token"));
                BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "{\"code\":1,\"data\":[{\"id\":1,\"productNo\":\"P1\",\"price\":12.50}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = ProductClient.class)
    static class TestApp {}
}
