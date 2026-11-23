package com.tva.startrail.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tva.startrail.common.Result;
import com.tva.startrail.common.UserContext;
import com.tva.startrail.util.JwtUtil;
import com.tva.startrail.util.RedisKeys ;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器（技术文档 §2.3）
 * 顺序：验签 → type=access 校验 → 登出黑名单 EXISTS 检查 → 写入 UserContext
 * 降级策略：Redis 不可用时跳过黑名单检查，依赖 30 分钟短有效期兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或 Token 缺失");
            return false;
        }
        String token = header.substring(7);

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            writeUnauthorized(response, "Token 无效或已过期");
            return false;
        }
        if (!JwtUtil.TYPE_ACCESS.equals(claims.get(JwtUtil.CLAIM_TYPE, String.class))) {
            writeUnauthorized(response, "Token 类型不正确");
            return false;
        }

        // 登出黑名单检查（Redis 故障时降级跳过）
        try {
            Boolean blocked = redisTemplate.hasKey(RedisKeys.blacklist(claims.getId()));
            if (Boolean.TRUE.equals(blocked)) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过登出黑名单检查：{}", e.getMessage());
        }

        UserContext.set(Long.valueOf(claims.getSubject()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, msg)));
    }
}
