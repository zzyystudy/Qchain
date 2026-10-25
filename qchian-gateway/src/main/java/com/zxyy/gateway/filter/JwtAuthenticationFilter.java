package com.zxyy.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.result.Result;
import com.zxyy.common.security.GatewayIdentityProperties;
import com.zxyy.common.security.IdentityHeaders;
import com.zxyy.common.security.JwtProperties;
import com.zxyy.common.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/** JWT 只在网关验签；响应式线程不使用 ThreadLocal SecurityContextHolder。 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Set<String> PUBLIC_GET = Set.of("/guest/category", "/guest/product",
            "/guest/shopOrder", "/guest/shopOrder/simple");
    private static final Set<String> PUBLIC_POST = Set.of("/admin/admins/login", "/guest/shopOrder/submit",
            "/guest/shopOrder/detail", "/guest/notify", "/guest/notify/");
    private final JwtProperties jwt;
    private final GatewayIdentityProperties identity;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtProperties jwt, GatewayIdentityProperties identity, ObjectMapper mapper) {
        this.jwt = jwt;
        this.identity = identity;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst(jwt.getAdminTokenName());
        // 先移除所有外部身份和服务凭据，白名单请求也不能携带伪造身份。
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(IdentityHeaders.USER_ID);
            headers.remove(IdentityHeaders.GATEWAY_TOKEN);
            headers.remove("X-Qchain-Internal-Token");
            headers.remove(jwt.getAdminTokenName());
        }).build();
        var clean = exchange.mutate().request(request).build();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        if (HttpMethod.OPTIONS.equals(method)
                || HttpMethod.GET.equals(method) && PUBLIC_GET.contains(path)
                || HttpMethod.POST.equals(method) && PUBLIC_POST.contains(path)) {
            return chain.filter(clean);
        }
        if (path.equals("/internal") || path.startsWith("/internal/")) {
            return reject(clean, HttpStatus.FORBIDDEN, "内部接口不对外开放");
        }
        if (token == null || token.isBlank()) return reject(clean, HttpStatus.UNAUTHORIZED, "请先登录");
        Long userId;
        try {
            var claims = JwtUtil.parseJWT(jwt.getAdminSecretKey(), token);
            userId = claims.get("adminId", Long.class);
            if (userId == null || userId <= 0 || claims.getExpiration() == null) {
                return reject(clean, HttpStatus.UNAUTHORIZED, "令牌无效");
            }
        } catch (JwtException | IllegalArgumentException e) {
            return reject(clean, HttpStatus.UNAUTHORIZED, "令牌无效或已过期");
        }
        if (identity.getGatewayToken() == null || identity.getGatewayToken().isBlank()) {
            return reject(clean, HttpStatus.SERVICE_UNAVAILABLE, "未配置身份转发凭据");
        }
        var authenticated = request.mutate().headers(headers -> {
            headers.remove(jwt.getAdminTokenName());
            headers.set(IdentityHeaders.USER_ID, userId.toString());
            headers.set(IdentityHeaders.GATEWAY_TOKEN, identity.getGatewayToken());
        }).build();
        return chain.filter(clean.mutate().request(authenticated).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = mapper.writeValueAsBytes(new Result<>(status.value(), message, null));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() { return -100; }
}
