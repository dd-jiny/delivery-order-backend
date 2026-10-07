package com.example.delivery.global.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-must-be-at-least-32-bytes";
    private static final Instant ISSUED_AT = Instant.parse("2026-10-07T05:00:00Z");

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, fixedClock(ISSUED_AT));

    @Test
    @DisplayName("발급한 토큰을 해석하면 회원 PK·아이디·역할을 얻는다")
    void createToken_thenParse() {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        Optional<AuthUser> authUser = jwtProvider.parse(token);

        // then
        assertThat(authUser).contains(new AuthUser(1L, "owner1", UserRole.OWNER));
    }

    @Test
    @DisplayName("토큰에는 sub·username·role·exp 클레임만 담긴다")
    void createToken_claims() {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(ISSUED_AT))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // then
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "username", "role", "exp");
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("username", String.class)).isEqualTo("owner1");
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("토큰은 발급 시각으로부터 1시간 뒤에 만료된다")
    void createToken_expiration() {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(ISSUED_AT))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // then
        assertThat(claims.getExpiration().toInstant()).isEqualTo(Instant.parse("2026-10-07T06:00:00Z"));
        assertThat(jwtProvider.getExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("만료 전인 토큰은 해석된다")
    void parse_beforeExpiration() {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);
        JwtProvider later = new JwtProvider(SECRET, fixedClock(Instant.parse("2026-10-07T05:59:59Z")));

        // when
        Optional<AuthUser> authUser = later.parse(token);

        // then
        assertThat(authUser).isPresent();
    }

    @Test
    @DisplayName("만료된 토큰은 거절한다")
    void parse_expired() {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);
        JwtProvider later = new JwtProvider(SECRET, fixedClock(Instant.parse("2026-10-07T06:00:01Z")));

        // when
        Optional<AuthUser> authUser = later.parse(token);

        // then
        assertThat(authUser).isEmpty();
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰은 거절한다")
    void parse_otherSecret() {
        // given
        JwtProvider other = new JwtProvider("other-secret-key-for-jwt-must-be-at-least-32-bytes", fixedClock(ISSUED_AT));
        String token = other.createToken(1L, "owner1", UserRole.OWNER);

        // when
        Optional<AuthUser> authUser = jwtProvider.parse(token);

        // then
        assertThat(authUser).isEmpty();
    }

    @Test
    @DisplayName("본문이 변조된 토큰은 거절한다")
    void parse_tamperedPayload() {
        // given
        String token = jwtProvider.createToken(1L, "cust1", UserRole.CUSTOMER);
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"username\":\"cust1\",\"role\":\"OWNER\",\"exp\":1791349200}"
                        .getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        // when
        Optional<AuthUser> authUser = jwtProvider.parse(tampered);

        // then
        assertThat(authUser).isEmpty();
    }

    @Test
    @DisplayName("JWT 형식이 아닌 문자열은 거절한다")
    void parse_malformed() {
        // when
        Optional<AuthUser> authUser = jwtProvider.parse("not-a-jwt");

        // then
        assertThat(authUser).isEmpty();
    }

    @Test
    @DisplayName("비밀키가 32바이트보다 짧으면 JwtProvider를 만들 수 없다")
    void constructor_weakSecret() {
        // when & then
        assertThatThrownBy(() -> new JwtProvider("short-secret", fixedClock(ISSUED_AT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
