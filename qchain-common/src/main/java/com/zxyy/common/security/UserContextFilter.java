package com.zxyy.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/** 微服务只接收可信转发身份，不再解析 JWT。必须加入 SecurityFilterChain。 */
public class UserContextFilter extends OncePerRequestFilter {
    private final GatewayIdentityProperties properties;
    private final LoginSessionStore sessions;
    private final ObjectMapper mapper;

    public UserContextFilter(GatewayIdentityProperties properties, LoginSessionStore sessions, ObjectMapper mapper) {
        this.properties = properties;
        this.sessions = sessions;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "/admin/admins/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        try {
            String id = request.getHeader(IdentityHeaders.USER_ID);
            if (id == null) {
                chain.doFilter(request, response);
                return;
            }
            String supplied = request.getHeader(IdentityHeaders.GATEWAY_TOKEN);
            String expected = properties.getGatewayToken();
            if (expected == null || expected.isBlank() || supplied == null
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                reject(response, 401, "用户身份来源不可信");
                return;
            }
            Long userId;
            try {
                userId = Long.valueOf(id);
                if (userId <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                reject(response, 401, "用户身份无效");
                return;
            }
            LoginSession session;
            try {
                session = sessions.findByUserId(userId);
            } catch (DataAccessException | IllegalStateException e) {
                reject(response, 503, "登录状态服务暂不可用");
                return;
            }
            if (session == null || !userId.equals(session.userId())) {
                reject(response, 401, "登录状态已失效");
                return;
            }
            var authorities = session.permissions().stream().filter(Objects::nonNull)
                    .filter(permission -> !permission.isBlank()).map(SimpleGrantedAuthority::new).toList();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(session, null, authorities));
            SecurityContextHolder.setContext(context);
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), new Result<>(code, message, null));
    }
}
