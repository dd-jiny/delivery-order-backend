package com.example.delivery.global.infrastructure.security;

import com.example.delivery.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증 (HS256). 클레임은 sub(회원 PK)·username·role·exp만 담는다.
 * 현재 시각은 주입받은 Clock에서 가져와 테스트에서 만료를 고정 시각으로 검증할 수 있게 한다.
 */
@Component
public class JwtProvider {

    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final int MIN_SECRET_BYTES = 32;
    private static final String USERNAME_CLAIM = "username";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Clock clock;

    public JwtProvider(@Value("${jwt.secret}") String secret, Clock clock) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT 비밀키는 32바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.clock = clock;
    }

    public String createToken(Long userId, String username, UserRole role) {
        Date expiration = Date.from(clock.instant().plus(EXPIRATION));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(USERNAME_CLAIM, username)
                .claim(ROLE_CLAIM, role.name())
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 서명 불일치·만료·형식 오류면 빈 값을 돌려준다. 거절 여부는 호출하는 쪽(Security URL 규칙)이 정한다.
     */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(USERNAME_CLAIM, String.class),
                    UserRole.valueOf(claims.get(ROLE_CLAIM, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return EXPIRATION.toSeconds();
    }
}
