package com.zxyy.order.config;

import com.zxyy.common.security.UserContextFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http, UserContextFilter userContextFilter,
            AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler) throws Exception {
        return defaults(http, entryPoint, deniedHandler).authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/guest/shopOrder", "/guest/shopOrder/simple",
                        "/doc.html", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/v3/api-docs-ext/**", "/swagger-resources/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/guest/shopOrder/submit", "/guest/shopOrder/detail",
                        "/guest/notify", "/guest/notify/").permitAll()
                .anyRequest().denyAll())
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private HttpSecurity defaults(HttpSecurity http, AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler deniedHandler) throws Exception {
        return http.cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler));
    }
}
