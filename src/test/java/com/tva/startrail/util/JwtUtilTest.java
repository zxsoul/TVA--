package com.tva.startrail.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtUtil 单元测试（真实签发/验签，不 Mock）
 */
@DisplayName("JWT 工具测试")
class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef"; // 32 字节 ≥ HS256 要求

    private JwtUtil newJwtUtil(long accessTtl, long refreshTtl) {
        return new JwtUtil(SECRET, accessTtl, refreshTtl);
    }

    @Test
    @DisplayName("Access Token 签发与解析往返一致（sub / type / jti / exp）")
    void accessToken_RoundTrip() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);

        String token = jwtUtil.createAccessToken(42L);
        Claims claims = jwtUtil.parse(token);

        assertEquals("42", claims.getSubject());
        assertEquals(JwtUtil.TYPE_ACCESS, claims.get(JwtUtil.CLAIM_TYPE, String.class));
        assertNotNull(claims.getId());
        assertFalse(claims.getId().isBlank());
        long ttl = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(30_000, ttl, "过期时间应等于签发时间 + 30s");
    }

    @Test
    @DisplayName("Refresh Token 携带指定 jti")
    void refreshToken_CarriesJti() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);

        String token = jwtUtil.createRefreshToken(7L, "jti-abc");
        Claims claims = jwtUtil.parse(token);

        assertEquals("jti-abc", claims.getId());
        assertEquals(JwtUtil.TYPE_REFRESH, claims.get(JwtUtil.CLAIM_TYPE, String.class));
        assertEquals("7", claims.getSubject());
    }

    @Test
    @DisplayName("Token 过期后解析抛 ExpiredJwtException")
    void parse_ExpiredToken_Throw() throws InterruptedException {
        JwtUtil jwtUtil = newJwtUtil(1, 604800); // 1 秒过期
        String token = jwtUtil.createAccessToken(1L);

        Thread.sleep(1100); // 等真实过期

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parse(token));
    }

    @Test
    @DisplayName("Token 被篡改后验签失败")
    void parse_TamperedToken_Throw() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);
        String token = jwtUtil.createAccessToken(1L);
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }

    @Test
    @DisplayName("type claim 可用于区分 access / refresh")
    void typeClaim_Distinguishes() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);

        Claims access = jwtUtil.parse(jwtUtil.createAccessToken(1L));
        Claims refresh = jwtUtil.parse(jwtUtil.createRefreshToken(1L, "j"));

        assertEquals(JwtUtil.TYPE_ACCESS, access.get(JwtUtil.CLAIM_TYPE, String.class));
        assertEquals(JwtUtil.TYPE_REFRESH, refresh.get(JwtUtil.CLAIM_TYPE, String.class));
    }

    @Test
    @DisplayName("remainingSeconds——未过期返回剩余秒数")
    void remainingSeconds_Future() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);
        Claims claims = jwtUtil.parse(jwtUtil.createAccessToken(1L));

        long remain = jwtUtil.remainingSeconds(claims);
        assertTrue(remain > 0 && remain <= 30);
    }

    @Test
    @DisplayName("remainingSeconds——已过期也至少返回 1 秒（黑名单 TTL 下限）")
    void remainingSeconds_Past_ClampToOne() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 10_000));

        assertEquals(1, jwtUtil.remainingSeconds(claims));
    }

    @Test
    @DisplayName("TTL 配置正确暴露")
    void ttl_Getters() {
        JwtUtil jwtUtil = newJwtUtil(30, 604800);
        assertEquals(30, jwtUtil.getAccessTtlSeconds());
        assertEquals(604800, jwtUtil.getRefreshTtlSeconds());
    }
}
