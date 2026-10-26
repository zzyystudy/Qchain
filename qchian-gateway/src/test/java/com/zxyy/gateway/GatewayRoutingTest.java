package com.zxyy.gateway;

import com.sun.net.httpserver.HttpServer;
import com.zxyy.common.security.IdentityHeaders;
import com.zxyy.common.security.JwtUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 Netty 网关转发到本地 HTTP 上游，不连接 Nacos、业务数据库或 Redis。 */
@SpringBootTest(classes = GatewayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.nacos.discovery.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "shop.jwt.admin-secret-key=gateway-test-signing-secret-at-least-32-bytes",
                "qchain.security.gateway-token=test-forwarding-credential"})
class GatewayRoutingTest {
    private static final AtomicReference<HttpHeaders> RECEIVED = new AtomicReference<>();
    private static final HttpServer UPSTREAM = startUpstream();
    @Autowired WebTestClient client;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        // Boot 对列表采用整体覆盖，因此按实际 YAML 路由复制完整列表，仅替换上游地址。
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        yaml.getObject().forEach((key, value) -> {
            String name = key.toString();
            if (name.startsWith("spring.cloud.gateway.server.webflux.routes[")) {
                registry.add(name, () -> name.endsWith(".uri")
                        ? "http://127.0.0.1:" + UPSTREAM.getAddress().getPort() : value);
            }
        });
    }

    @BeforeEach void reset() { RECEIVED.set(null); }
    @AfterAll static void close() { UPSTREAM.stop(0); }

    @Test
    void loginAndPaymentCallbackRouteWithoutAuthentication() {
        for (String path : new String[]{"/admin/admins/login", "/guest/notify"}) {
            client.post().uri(path).header(IdentityHeaders.USER_ID, "forged")
                    .header(IdentityHeaders.GATEWAY_TOKEN, "forged").exchange().expectStatus().isOk()
                    .expectBody().jsonPath("$.data").isEqualTo(path);
            assertThat(RECEIVED.get()).doesNotContainKeys(IdentityHeaders.USER_ID, IdentityHeaders.GATEWAY_TOKEN);
        }
    }

    @Test
    void protectedRouteForwardsOnlyValidatedIdentity() {
        String jwt = JwtUtil.createJWT("gateway-test-signing-secret-at-least-32-bytes", 60000, Map.of("adminId", 42L));
        client.get().uri("/admin/product/page").header("authentication", jwt)
                .header(IdentityHeaders.USER_ID, "999").header(IdentityHeaders.GATEWAY_TOKEN, "forged")
                .header("X-Qchain-Internal-Token", "forged").exchange().expectStatus().isOk();
        assertThat(RECEIVED.get().getFirst(IdentityHeaders.USER_ID)).isEqualTo("42");
        assertThat(RECEIVED.get().getFirst(IdentityHeaders.GATEWAY_TOKEN)).isEqualTo("test-forwarding-credential");
        assertThat(RECEIVED.get()).doesNotContainKeys("authentication", "X-Qchain-Internal-Token");
    }

    @Test
    void missingJwtStopsRequestBeforeUpstream() {
        client.get().uri("/admin/product/page").exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(401);
        assertThat(RECEIVED.get()).isNull();
    }

    @Test
    void browserPreflightIsHandledWithoutTokenOrUpstream() {
        client.options().uri("/admin/product").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authentication,content-type")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
        assertThat(RECEIVED.get()).isNull();
    }

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                var headers = new HttpHeaders();
                exchange.getRequestHeaders().forEach(headers::put);
                RECEIVED.set(headers);
                exchange.getRequestBody().readAllBytes();
                byte[] body = ("{\"code\":1,\"data\":\"" + exchange.getRequestURI().getPath() + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
