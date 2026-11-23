package com.tva.startrail.util;

/**
 * Redis 键统一管理（技术文档 §2.3、§5.1②）
 */
public class RedisKeys {

    /** Refresh Token：refresh:{userId} → jti，TTL 7 天 */
    public static String refresh(Long userId) {
        return "refresh:" + userId;
    }

    /** 登出黑名单：blacklist:{jti} → 1，TTL = Token 剩余有效期 */
    public static String blacklist(String jti) {
        return "blacklist:" + jti;
    }

    /** 邮箱验证码本体：email_code:{email} → 6 位数字，TTL 300s，一次性使用 */
    public static String emailCode(String email) {
        return "email_code:" + email;
    }

    /** 重发冷却：email_cooldown:{email} → 1，TTL 60s，SET NX 占位 */
    public static String emailCooldown(String email) {
        return "email_cooldown:" + email;
    }

    /** 防爆破计数：email_code_err:{email} → 输错次数，TTL 300s，达 5 次作废 */
    public static String emailCodeErr(String email) {
        return "email_code_err:" + email;
    }
}
