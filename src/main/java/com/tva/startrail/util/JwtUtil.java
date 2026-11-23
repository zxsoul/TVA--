package com.tva.startrail.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具（技术文档 §2.3）
 * 载荷：sub=userId, jti=唯一编号, type=access/refresh, exp
 */
@Component
public class JwtUtil {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtUtil(@Value("${tva.jwt.secret}") String secret,
                   @Value("${tva.jwt.access-ttl-seconds}") long accessTtlSeconds,
                   @Value("${tva.jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    /**
     * 签发 Access Token（30 分钟）
     */
    public String createAccessToken(Long userId) {
        return build(userId, TYPE_ACCESS, accessTtlSeconds, UUID.randomUUID().toString());
    }

    /**
     * 签发 Refresh Token（7 天），返回 jti 供写入 Redis
     */
    public String createRefreshToken(Long userId, String jti) {
        return build(userId, TYPE_REFRESH, refreshTtlSeconds, jti);
    }

    private String build(Long userId, String type, long ttlSeconds, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .claim(CLAIM_TYPE, type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 验签并解析载荷，失败抛 io.jsonwebtoken.JwtException
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    /**
     * Access Token 剩余有效期（秒），供登出黑名单 TTL 使用
     */
    public long remainingSeconds(Claims claims) {
        long remain = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 1);
    }
}
