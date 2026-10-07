package com.example.delivery.global.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 Bearer 토큰이 유효하면 AuthUser를 SecurityContext에 저장한다.
 * 토큰이 없거나 잘못돼도 거절하지 않고 인증 정보 없이 넘긴다. 거절은 Security URL 규칙(AuthorizationFilter)이 한다.
 * SecurityConfig에서 직접 생성해 Security 필터 체인에만 등록한다 (빈으로 등록하면 서블릿 필터로도 한 번 더 등록된다).
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            jwtProvider.parse(header.substring(BEARER_PREFIX.length()))
                    .ifPresent(JwtAuthenticationFilter::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private static void authenticate(AuthUser authUser) {
        var authority = new SimpleGrantedAuthority(ROLE_PREFIX + authUser.role().name());
        var authentication = new UsernamePasswordAuthenticationToken(authUser, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
