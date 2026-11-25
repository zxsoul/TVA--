package com.tva.startrail.service.impl;

import com.tva.startrail.common.BizException;
import com.tva.startrail.common.UserContext;
import com.tva.startrail.dto.EmailCodeDTO;
import com.tva.startrail.dto.LoginDTO;
import com.tva.startrail.dto.RefreshDTO;
import com.tva.startrail.dto.RegisterDTO;
import com.tva.startrail.entity.User;
import com.tva.startrail.service.AuthService;
import com.tva.startrail.service.MailService;
import com.tva.startrail.service.UserService;
import com.tva.startrail.util.JwtUtil;
import com.tva.startrail.util.RedisKeys;
import com.tva.startrail.vo.LoginVO;
import com.tva.startrail.vo.RefreshVO;
import com.tva.startrail.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 鉴权业务实现（技术文档 §5.1）
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration EMAIL_CODE_TTL = Duration.ofSeconds(300);
    private static final Duration EMAIL_COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final int EMAIL_CODE_MAX_ERR = 5;

    private final UserService userService;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /**
     * ② 发送邮箱验证码2
     * 流程：格式校验 → SET NX 占冷却位 → 生成 6 位验证码写 Redis → 异步发邮件
     */
    @Override
    public void sendEmailCode(EmailCodeDTO dto) {
        String email = dto.getEmail();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.emailCooldown(email), "1", EMAIL_COOLDOWN_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            Long ttl = redisTemplate.getExpire(RedisKeys.emailCooldown(email), TimeUnit.SECONDS);
            throw new BizException(400, "请 " + (ttl != null && ttl > 0 ? ttl : 60) + " 秒后重试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(RedisKeys.emailCode(email), code, EMAIL_CODE_TTL);
        redisTemplate.delete(RedisKeys.emailCodeErr(email));

        mailService.sendEmailCode(email, code);
    }

    /**
     * ① 注册：验证码校验 → 查重 → BCrypt 落库 → 签发双 Token（注册成功直接视为登录）
     */
    @Override
    public LoginVO register(RegisterDTO dto) {
        verifyEmailCode(dto.getEmail(), dto.getEmailCode());

        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BizException(400, "用户名已存在，请更换");
        }
        if (userService.getByEmail(dto.getEmail()) != null) {
            throw new BizException(400, "该邮箱已注册，可直接登录或找回密码");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        try {
            userService.save(user);
        } catch (DuplicateKeyException e) {
            // 并发撞唯一索引兜底
            throw new BizException(400, "用户名或邮箱已被注册");
        }

        return buildLoginVO(user);
    }

    /**
     * ③ 登录
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(400, "用户名或密码错误");
        }
        return buildLoginVO(user);
    }

    /**
     * ④ 刷新 Access Token：验签 → type=refresh → 与 Redis 中 jti 比对
     */
    @Override
    public RefreshVO refresh(RefreshDTO dto) {
        Claims claims;
        try {
            claims = jwtUtil.parse(dto.getRefreshToken());
        } catch (Exception e) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        if (!JwtUtil.TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TYPE, String.class))) {
            throw new BizException(401, "Token 类型不正确");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String savedJti = redisTemplate.opsForValue().get(RedisKeys.refresh(userId));
        if (savedJti == null || !savedJti.equals(claims.getId())) {
            // 已登出 / 被挤下线 / 盗用嫌疑
            throw new BizException(401, "登录已失效，请重新登录");
        }

        RefreshVO vo = new RefreshVO();
        vo.setAccessToken(jwtUtil.createAccessToken(userId));
        return vo;
    }

    /**
     * ⑤ 登出：当前 Access Token 加入黑名单（TTL=剩余有效期），并吊销 Refresh Token
     */
    @Override
    public void logout(String authorization) {
        String token = authorization.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            throw new BizException(401, "Token 无效或已过期");
        }
        long remain = jwtUtil.remainingSeconds(claims);
        redisTemplate.opsForValue().set(RedisKeys.blacklist(claims.getId()), "1", Duration.ofSeconds(remain));
        redisTemplate.delete(RedisKeys.refresh(Long.valueOf(claims.getSubject())));
    }

    /**
     * ⑥ 当前用户信息
     */
    @Override
    public UserVO me() {
        Long userId = UserContext.require();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在或已注销");
        }
        return toUserVO(user);
    }

    // ---------- 私有方法 ----------

    /**
     * 验证码校验链：不存在 → 过期；不一致 → 错误计数，连错 5 次作废；通过 → 一次性删除
     */
    private void verifyEmailCode(String email, String inputCode) {
        String key = RedisKeys.emailCode(email);
        String code = redisTemplate.opsForValue().get(key);
        if (code == null) {
            throw new BizException(400, "验证码已过期，请重新获取");
        }
        if (!code.equals(inputCode)) {
            Long err = redisTemplate.opsForValue().increment(RedisKeys.emailCodeErr(email));
            if (err != null && err == 1L) {
                redisTemplate.expire(RedisKeys.emailCodeErr(email), EMAIL_CODE_TTL);
            }
            if (err != null && err >= EMAIL_CODE_MAX_ERR) {
                redisTemplate.delete(key);
                redisTemplate.delete(RedisKeys.emailCodeErr(email));
                throw new BizException(400, "错误次数过多，验证码已作废，请重新获取");
            }
            throw new BizException(400, "验证码错误，请重新输入");
        }
        // 验证通过，一次性作废
        redisTemplate.delete(key);
        redisTemplate.delete(RedisKeys.emailCodeErr(email));
    }

    /**
     * 签发双 Token，refresh 的 jti 写入 Redis（SET refresh:{userId} {jti} EX 604800）
     */
    private LoginVO buildLoginVO(User user) {
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessToken(user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), jti);
        redisTemplate.opsForValue()
                .set(RedisKeys.refresh(user.getId()), jti, Duration.ofSeconds(jwtUtil.getRefreshTtlSeconds()));

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(toUserVO(user));
        return vo;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }
}
/*
* jwt refresh token 刷新机制，用户只要是登录进去，就会自动创建refresh token，登出就会自动删除refresh token，refresh token 刷新命令由前端调用返回401时执行 refresh 命令
* 业务逻辑总结
* 登录：数据库查验，重置toekn，返回信息
* 注册：验证邮箱密码，数据库查验是否存在相同的 用户名或者邮箱，数据库创建，唯一索引兜底并发问题，重置token,返回信息
* 登出：删除token信息，黑名单做标记，返回
* 邮箱发送：判断是否在冷静期，发送邮箱
* 验证邮箱密码（私有）：redis查询验证码，判断是否符合，不符合记录次数，超过次数，强制废除，并删除记录，否则返回验证码失效，如果一样，那自动废除之前用于判断次数error和email_key，
* */