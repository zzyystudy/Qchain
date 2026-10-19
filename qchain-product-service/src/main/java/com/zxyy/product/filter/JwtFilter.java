package com.zxyy.product.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.result.Result;
import com.zxyy.common.security.LoginSession;
import com.zxyy.product.properties.JwtProperties;
import com.zxyy.product.security.LoginSessionStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 只挂在 /admin/** 对应的 Security 过滤器链中。 */
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtProperties jwtProperties;
    private final LoginSessionStore sessionStore;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtProperties jwtProperties, LoginSessionStore sessionStore,
                     AuthenticationEntryPoint authenticationEntryPoint, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.sessionStore = sessionStore;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId;
        try {
            // 与用户服务保持一致：使用配置字符串的 UTF-8 字节构造 HMAC 验签密钥。
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtProperties.getAdminSecretKey()
                            .getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(token).getPayload();
            userId = Long.valueOf(claims.get("adminId").toString());
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("令牌无效", exception));
            return;
        }

        LoginSession session;
        try {
            session = sessionStore.findByUserId(userId);
        } catch (DataAccessException | IllegalStateException exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    new Result<>(503, "登录状态服务暂不可用", null));
            return;
        }
        if (session == null || !Objects.equals(session.userId(), userId)) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("登录状态已失效"));
            return;
        }

        var authorities = session.permissions().stream().filter(Objects::nonNull)
                .filter(permission -> !permission.isBlank())
                .map(SimpleGrantedAuthority::new).toList();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(session, null, authorities));
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }
}
