package com.zxyy.user.filter;

import com.zxyy.common.constant.MessageConstant;
import com.zxyy.user.constant.JwtClaimsConstant;
import com.zxyy.user.constant.RedisConstant;
import com.zxyy.common.security.LoginSession;
import com.zxyy.user.properties.JwtProperties;
import com.zxyy.user.util.JwtUtil;
import com.zxyy.user.util.RedisCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtProperties jwtProperties;
    private final RedisCache redisCache;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtFilter(JwtProperties jwtProperties, RedisCache redisCache,
                     AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtProperties = jwtProperties;
        this.redisCache = redisCache;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/admin/admins/login".equals(request.getServletPath());
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
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            userId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException(MessageConstant.TOKEN_INVALID, exception));
            return;
        }

        LoginSession session = redisCache.getCacheObject(RedisConstant.USER_LOGIN + userId);
        if (session == null || !Objects.equals(userId, session.userId())) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException(MessageConstant.USER_NOT_LOGIN));
            return;
        }
        List<SimpleGrantedAuthority> authorities = session.permissions() == null ? List.of()
                : session.permissions().stream().filter(Objects::nonNull)
                .filter(permission -> !permission.isBlank())
                .map(SimpleGrantedAuthority::new).toList();
        var authentication = new UsernamePasswordAuthenticationToken(session, null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }
}
