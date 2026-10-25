package com.zxyy.product.config;

import com.zxyy.common.security.UserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.zxyy.api.config.InternalFeignConfiguration;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(0)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http, UserContextFilter userContextFilter,
            AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler,
            @Value("${qchain.internal.token:}") String token) throws Exception {
        return configureDefaults(http, entryPoint, deniedHandler)
                .securityMatcher("/internal/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) -> {
                    String supplied = context.getRequest().getHeader(InternalFeignConfiguration.TOKEN_HEADER);
                    return new AuthorizationDecision(!token.isBlank() && supplied != null
                            && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                                    supplied.getBytes(StandardCharsets.UTF_8)));
                }))
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, UserContextFilter userContextFilter,
            AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler) throws Exception {
        return configureDefaults(http, entryPoint, deniedHandler)
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http, UserContextFilter userContextFilter,
            AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler) throws Exception {
        return configureDefaults(http, entryPoint, deniedHandler)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/guest/**", "/doc.html", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs-ext/**", "/swagger-resources/**",
                                "/webjars/**", "/favicon.ico").permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private HttpSecurity configureDefaults(HttpSecurity http, AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler deniedHandler) throws Exception {
        return http.cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler));
    }
}
