package com.zxyy.user.config;

import com.zxyy.user.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;
    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    // JwtFilter 只由 Security 过滤器链执行，避免被 Servlet 容器重复注册。
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    //密码处理
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //security配置
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // 当前方案由前端在请求头携带 JWT，不使用 Session 登录
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
                    httpSecurityExceptionHandlingConfigurer.accessDeniedHandler(accessDeniedHandler);
                    httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(authenticationEntryPoint);
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/admin/admins/login").permitAll()
                        .requestMatchers(
                                "/guest/**",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs-ext/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()
                        .anyRequest().authenticated())
                //添加过滤器
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    //注册manager为bean 可以让其他组件调用
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
