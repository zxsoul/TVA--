package com.tva.startrail.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RedisKeys 键拼接格式测试
 */
@DisplayName("Redis 键命名测试")
class RedisKeysTest {

    @Test
    @DisplayName("refresh 键格式：refresh:{userId}")
    void refreshKey() {
        assertEquals("refresh:42", RedisKeys.refresh(42L));
    }

    @Test
    @DisplayName("blacklist 键格式：blacklist:{jti}")
    void blacklistKey() {
        assertEquals("blacklist:jti-abc", RedisKeys.blacklist("jti-abc"));
    }

    @Test
    @DisplayName("emailCode 键格式：email_code:{email}")
    void emailCodeKey() {
        assertEquals("email_code:a@b.com", RedisKeys.emailCode("a@b.com"));
    }

    @Test
    @DisplayName("emailCooldown 键格式：email_cooldown:{email}")
    void emailCooldownKey() {
        assertEquals("email_cooldown:a@b.com", RedisKeys.emailCooldown("a@b.com"));
    }

    @Test
    @DisplayName("emailCodeErr 键格式：email_code_err:{email}")
    void emailCodeErrKey() {
        assertEquals("email_code_err:a@b.com", RedisKeys.emailCodeErr("a@b.com"));
    }
}
