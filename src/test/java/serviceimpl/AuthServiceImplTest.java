package serviceimpl;

import com.tva.startrail.common.BizException;
import com.tva.startrail.common.UserContext;
import com.tva.startrail.dto.EmailCodeDTO;
import com.tva.startrail.dto.LoginDTO;
import com.tva.startrail.dto.RefreshDTO;
import com.tva.startrail.dto.RegisterDTO;
import com.tva.startrail.entity.User;
import com.tva.startrail.service.MailService;
import com.tva.startrail.service.UserService;
import com.tva.startrail.service.impl.AuthServiceImpl;
import com.tva.startrail.util.JwtUtil;
import com.tva.startrail.util.RedisKeys;
import com.tva.startrail.vo.LoginVO;
import com.tva.startrail.vo.RefreshVO;
import com.tva.startrail.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 全分支单元测试
 */
@DisplayName("鉴权模块单元测试")
@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    private static final String EMAIL = "1009200727@qq.com";
    private static final String CODE = "123456";
    private static final String BCRYPT_HASH = BCrypt.hashpw("password123", BCrypt.gensalt());

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Mock
    MailService mailService;
    @Mock
    UserService userService;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    Claims claims;
    @InjectMocks
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // 公共桩：几乎所有用例都会走到 redisTemplate.opsForValue()
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ---------------- sendEmailCode ----------------

    @Test
    @DisplayName("发送邮箱验证码——60秒冷却未到返回详细冷却参数")
    void sendEmailCode_CooldownNotExpired_RejectRequest() {
        EmailCodeDTO dto = new EmailCodeDTO();
        dto.setEmail(EMAIL);

        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);
        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenReturn(40L);

        BizException e = assertThrows(BizException.class, () -> authService.sendEmailCode(dto));
        assertEquals(400, e.getCode());
        assertEquals("请 40 秒后重试", e.getMessage());
    }

    @Test
    @DisplayName("发送邮箱验证码——冷却位无 TTL 时按默认 60 秒提示")
    void sendEmailCode_CooldownWithoutTtl_Default60() {
        EmailCodeDTO dto = new EmailCodeDTO();
        dto.setEmail(EMAIL);

        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);
        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.sendEmailCode(dto));
        assertEquals("请 60 秒后重试", e.getMessage());
    }

    @Test
    @DisplayName("发送邮箱验证码——成功发送验证码并异步发邮件")
    void sendEmailCode_Success() {
        EmailCodeDTO dto = new EmailCodeDTO();
        dto.setEmail(EMAIL);

        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        authService.sendEmailCode(dto);

        verify(valueOps).setIfAbsent(eq(RedisKeys.emailCooldown(EMAIL)), eq("1"), any(Duration.class));
        verify(valueOps).set(eq(RedisKeys.emailCode(EMAIL)), matches("\\d{6}"), eq(Duration.ofSeconds(300)));
        verify(redisTemplate).delete(RedisKeys.emailCodeErr(EMAIL));
        verify(mailService).sendEmailCode(eq(EMAIL), matches("\\d{6}"));
    }

    // ---------------- register ----------------

    private RegisterDTO buildRegisterDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail(EMAIL);
        dto.setNickname("Tony");
        dto.setUsername("tony2024");
        dto.setPassword("password123");
        dto.setEmailCode(CODE);
        return dto;
    }

    /** 桩住验证码校验链：Redis 中存的码 = 入参的码 */
    private void stubEmailCodeOK() {
        when(valueOps.get(RedisKeys.emailCode(EMAIL))).thenReturn(CODE);
    }

    private void stubTokenIssuance() {
        when(jwtUtil.createAccessToken(any())).thenReturn("access_token");
        when(jwtUtil.createRefreshToken(any(), anyString())).thenReturn("refresh_token");
    }

    @Test
    @DisplayName("注册——成功注册（验证码通过、双 Token 签发、密码 BCrypt 落库）")
    void register_Success() {
        RegisterDTO dto = buildRegisterDTO();
        stubEmailCodeOK();
        when(userService.getByUsername(dto.getUsername())).thenReturn(null);
        when(userService.getByEmail(dto.getEmail())).thenReturn(null);
        stubTokenIssuance();

        LoginVO vo = authService.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("tony2024", saved.getUsername());
        assertEquals("Tony", saved.getNickname());
        assertTrue(BCrypt.checkpw("password123", saved.getPassword()), "密码应以 BCrypt 哈希落库");

        assertEquals("Tony", vo.getUser().getNickname());
        assertEquals("tony2024", vo.getUser().getUsername());
        assertEquals(EMAIL, vo.getUser().getEmail());
        assertEquals("access_token", vo.getAccessToken());
        assertEquals("refresh_token", vo.getRefreshToken());
        // refresh jti 写入 Redis
        verify(valueOps).set(startsWith("refresh:"), anyString(), any(Duration.class));
        // 验证码一次性作废
        verify(redisTemplate).delete(RedisKeys.emailCode(EMAIL));
    }

    @Test
    @DisplayName("注册——未填昵称时缺省使用用户名")
    void register_NoNickname_DefaultToUsername() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setNickname(null);
        stubEmailCodeOK();
        when(userService.getByUsername(dto.getUsername())).thenReturn(null);
        when(userService.getByEmail(dto.getEmail())).thenReturn(null);
        stubTokenIssuance();

        authService.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertEquals("tony2024", captor.getValue().getNickname());
    }

    @Test
    @DisplayName("注册——验证码已过期（Redis 中无记录）")
    void register_CodeExpired_Throw() {
        RegisterDTO dto = buildRegisterDTO();
        when(valueOps.get(RedisKeys.emailCode(EMAIL))).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals(400, e.getCode());
        assertEquals("验证码已过期，请重新获取", e.getMessage());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("注册——验证码错误（首次错误计数并设置 TTL）")
    void register_CodeWrong_FirstError() {
        RegisterDTO dto = buildRegisterDTO();
        when(valueOps.get(RedisKeys.emailCode(EMAIL))).thenReturn("999999");
        when(valueOps.increment(RedisKeys.emailCodeErr(EMAIL))).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("验证码错误，请重新输入", e.getMessage());
        // 首次计数需为 err key 设置 TTL
        verify(redisTemplate).expire(RedisKeys.emailCodeErr(EMAIL), Duration.ofSeconds(300));
        // 未满 5 次不应作废验证码
        verify(redisTemplate, never()).delete(RedisKeys.emailCode(EMAIL));
    }

    @Test
    @DisplayName("注册——连续错 5 次验证码作废")
    void register_CodeWrong_5TimesInvalidate() {
        RegisterDTO dto = buildRegisterDTO();
        when(valueOps.get(RedisKeys.emailCode(EMAIL))).thenReturn("999999");
        when(valueOps.increment(RedisKeys.emailCodeErr(EMAIL))).thenReturn(5L);

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("错误次数过多，验证码已作废，请重新获取", e.getMessage());
        verify(redisTemplate).delete(RedisKeys.emailCode(EMAIL));
        verify(redisTemplate).delete(RedisKeys.emailCodeErr(EMAIL));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("注册——用户名已存在")
    void register_UsernameExists_Throw() {
        RegisterDTO dto = buildRegisterDTO();
        stubEmailCodeOK();
        when(userService.getByUsername(dto.getUsername())).thenReturn(new User());

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("用户名已存在，请更换", e.getMessage());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("注册——邮箱已注册")
    void register_EmailExists_Throw() {
        RegisterDTO dto = buildRegisterDTO();
        stubEmailCodeOK();
        when(userService.getByUsername(dto.getUsername())).thenReturn(null);
        when(userService.getByEmail(dto.getEmail())).thenReturn(new User());

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("该邮箱已注册，可直接登录或找回密码", e.getMessage());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("注册——并发撞唯一索引兜底转业务异常")
    void register_DuplicateKey_Throw() {
        RegisterDTO dto = buildRegisterDTO();
        stubEmailCodeOK();
        when(userService.getByUsername(dto.getUsername())).thenReturn(null);
        when(userService.getByEmail(dto.getEmail())).thenReturn(null);
        when(userService.save(any(User.class))).thenThrow(new DuplicateKeyException("uk_username"));

        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("用户名或邮箱已被注册", e.getMessage());
    }

    // ---------------- login ----------------

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("tony2024");
        user.setNickname("Tony");
        user.setEmail(EMAIL);
        user.setPassword(BCRYPT_HASH);
        return user;
    }

    @Test
    @DisplayName("登录——成功登录签发双 Token")
    void login_Success() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("tony2024");
        dto.setPassword("password123");
        when(userService.getByUsername("tony2024")).thenReturn(buildUser());
        stubTokenIssuance();

        LoginVO vo = authService.login(dto);

        assertEquals("access_token", vo.getAccessToken());
        assertEquals("refresh_token", vo.getRefreshToken());
        assertEquals("tony2024", vo.getUser().getUsername());
        verify(valueOps).set(eq("refresh:1"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("登录——用户不存在")
    void login_UserNotFound_Throw() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("nobody");
        dto.setPassword("password123");
        when(userService.getByUsername("nobody")).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(400, e.getCode());
        assertEquals("用户名或密码错误", e.getMessage());
    }

    @Test
    @DisplayName("登录——密码错误")
    void login_WrongPassword_Throw() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("tony2024");
        dto.setPassword("wrong_password");
        when(userService.getByUsername("tony2024")).thenReturn(buildUser());

        BizException e = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals("用户名或密码错误", e.getMessage());
    }

    // ---------------- refresh ----------------

    private RefreshDTO buildRefreshDTO() {
        RefreshDTO dto = new RefreshDTO();
        dto.setRefreshToken("refresh_token");
        return dto;
    }

    /** 桩住一个合法的 refresh Claims（sub=1, jti=jti-1）；部分用例用不到全部桩，故 lenient */
    private void stubRefreshClaims(String type) {
        lenient().when(jwtUtil.parse("refresh_token")).thenReturn(claims);
        when(claims.get(JwtUtil.CLAIM_TYPE, String.class)).thenReturn(type);
        lenient().when(claims.getSubject()).thenReturn("1");
        lenient().when(claims.getId()).thenReturn("jti-1");
    }

    @Test
    @DisplayName("刷新——成功换新 Access Token")
    void refresh_Success() {
        RefreshDTO dto = buildRefreshDTO();
        stubRefreshClaims(JwtUtil.TYPE_REFRESH);
        when(valueOps.get("refresh:1")).thenReturn("jti-1");
        when(jwtUtil.createAccessToken(1L)).thenReturn("new_access_token");

        RefreshVO vo = authService.refresh(dto);

        assertEquals("new_access_token", vo.getAccessToken());
    }

    @Test
    @DisplayName("刷新——Token 验签失败")
    void refresh_InvalidToken_Throw() {
        RefreshDTO dto = buildRefreshDTO();
        when(jwtUtil.parse("refresh_token")).thenThrow(new RuntimeException("bad signature"));

        BizException e = assertThrows(BizException.class, () -> authService.refresh(dto));
        assertEquals(401, e.getCode());
        assertEquals("登录已失效，请重新登录", e.getMessage());
    }

    @Test
    @DisplayName("刷新——Token 类型不是 refresh")
    void refresh_WrongType_Throw() {
        RefreshDTO dto = buildRefreshDTO();
        stubRefreshClaims(JwtUtil.TYPE_ACCESS);

        BizException e = assertThrows(BizException.class, () -> authService.refresh(dto));
        assertEquals("Token 类型不正确", e.getMessage());
    }

    @Test
    @DisplayName("刷新——jti 与 Redis 不一致（重放攻击 / 已登出 / 被挤下线）")
    void refresh_JtiMismatch_Throw() {
        RefreshDTO dto = buildRefreshDTO();
        stubRefreshClaims(JwtUtil.TYPE_REFRESH);
        when(valueOps.get("refresh:1")).thenReturn("another-jti");

        BizException e = assertThrows(BizException.class, () -> authService.refresh(dto));
        assertEquals(401, e.getCode());
        assertEquals("登录已失效，请重新登录", e.getMessage());
    }

    @Test
    @DisplayName("刷新——Redis 中已无 jti（Refresh Token 已吊销）")
    void refresh_NoSavedJti_Throw() {
        RefreshDTO dto = buildRefreshDTO();
        stubRefreshClaims(JwtUtil.TYPE_REFRESH);
        when(valueOps.get("refresh:1")).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.refresh(dto));
        assertEquals("登录已失效，请重新登录", e.getMessage());
    }

    // ---------------- logout ----------------

    @Test
    @DisplayName("登出——加入黑名单并吊销 Refresh Token")
    void logout_Success() {
        when(jwtUtil.parse("access_token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.remainingSeconds(claims)).thenReturn(120L);

        authService.logout("Bearer access_token");

        verify(valueOps).set(RedisKeys.blacklist("jti-1"), "1", Duration.ofSeconds(120));
        verify(redisTemplate).delete("refresh:1");
    }

    @Test
    @DisplayName("登出——Token 无效")
    void logout_InvalidToken_Throw() {
        when(jwtUtil.parse("bad_token")).thenThrow(new RuntimeException("expired"));

        BizException e = assertThrows(BizException.class, () -> authService.logout("Bearer bad_token"));
        assertEquals(401, e.getCode());
        assertEquals("Token 无效或已过期", e.getMessage());
    }

    // ---------------- me ----------------

    @Test
    @DisplayName("当前用户——成功返回用户信息")
    void me_Success() {
        UserContext.set(1L);
        when(userService.getById(1L)).thenReturn(buildUser());

        UserVO vo = authService.me();

        assertEquals(1L, vo.getId());
        assertEquals("tony2024", vo.getUsername());
    }

    @Test
    @DisplayName("当前用户——未登录抛 401")
    void me_NotLogin_Throw() {
        BizException e = assertThrows(BizException.class, () -> authService.me());
        assertEquals(401, e.getCode());
        assertEquals("未登录或 Token 已失效", e.getMessage());
    }

    @Test
    @DisplayName("当前用户——用户不存在或已注销")
    void me_UserGone_Throw() {
        UserContext.set(1L);
        when(userService.getById(1L)).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.me());
        assertEquals("用户不存在或已注销", e.getMessage());
    }
}
