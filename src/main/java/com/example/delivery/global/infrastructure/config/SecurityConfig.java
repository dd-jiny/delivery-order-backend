package com.example.delivery.global.infrastructure.config;

import com.example.delivery.global.infrastructure.security.JwtAuthenticationFilter;
import com.example.delivery.global.infrastructure.security.JwtProvider;
import com.example.delivery.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * URL 규칙은 03 API 명세 5.1을 그대로 옮긴다. 위에서부터 적용되므로 구체적인 규칙을 먼저 둔다.
 * 역할만으로 거절할 수 있는 것(403)만 여기서 막고, "본인 것만" 규칙은 Service가 검사한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String OWNER = UserRole.OWNER.name();
    private static final String CUSTOMER = UserRole.CUSTOMER.name();

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // JWT를 헤더로 보내는 API라 CSRF 공격 대상이 아니다. 켜 두면 POST·PUT·PATCH·DELETE가 모두 403이 된다.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 허용하지 않으면 400·404·409·500 응답이 모두 본문 없는 403으로 바뀐다 (발제 3-5)
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menus", "/api/menus/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/menus/**").hasRole(OWNER)
                        .requestMatchers(HttpMethod.PUT, "/api/menus/**").hasRole(OWNER)
                        .requestMatchers(HttpMethod.DELETE, "/api/menus/**").hasRole(OWNER)
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/cancel").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/accept", "/api/orders/*/complete").hasRole(OWNER)
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/payments").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/payments").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/*").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 기본 위임 인코더는 "{bcrypt}" 접두사를 붙여 저장하므로, 순수 BCrypt 해시($2a$...)가 저장되도록 직접 등록한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
