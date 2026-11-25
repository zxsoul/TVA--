package com.tva.startrail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tva.startrail.common.GlobalExceptionHandler;
import com.tva.startrail.dto.EmailCodeDTO;
import com.tva.startrail.dto.LoginDTO;
import com.tva.startrail.dto.RefreshDTO;
import com.tva.startrail.dto.RegisterDTO;
import com.tva.startrail.service.AuthService;
import com.tva.startrail.vo.LoginVO;
import com.tva.startrail.vo.RefreshVO;
import com.tva.startrail.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController MockMvc 测试（standalone + 全局异常处理器）
 */
@DisplayName("鉴权控制器测试")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RegisterDTO buildRegisterDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("tony2024");
        dto.setPassword("password123");
        dto.setEmail("tony@example.com");
        dto.setEmailCode("123456");
        dto.setNickname("Tony");
        return dto;
    }

    @Test
    @DisplayName("注册——成功返回双 Token")
    void register_Success() throws Exception {
        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("tony2024");
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken("access_token");
        loginVO.setRefreshToken("refresh_token");
        loginVO.setUser(userVO);
        when(authService.register(any(RegisterDTO.class))).thenReturn(loginVO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.data.user.username").value("tony2024"));
    }

    @Test
    @DisplayName("注册——缺用户名触发参数校验 400")
    void register_MissingUsername_400() throws Exception {
        RegisterDTO dto = buildRegisterDTO();
        dto.setUsername(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("username 用户名不能为空"));
    }

    @Test
    @DisplayName("注册——邮箱格式错误触发参数校验 400")
    void register_InvalidEmail_400() throws Exception {
        RegisterDTO dto = buildRegisterDTO();
        dto.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("email 邮箱格式不正确"));
    }

    @Test
    @DisplayName("注册——验证码非 6 位数字触发参数校验 400")
    void register_InvalidCodePattern_400() throws Exception {
        RegisterDTO dto = buildRegisterDTO();
        dto.setEmailCode("12ab");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("emailCode 验证码为 6 位数字"));
    }

    @Test
    @DisplayName("发送验证码——成功返回提示语")
    void sendEmailCode_Success() throws Exception {
        EmailCodeDTO dto = new EmailCodeDTO();
        dto.setEmail("tony@example.com");

        mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("验证码已发送，5 分钟内有效"));

        verify(authService).sendEmailCode(any(EmailCodeDTO.class));
    }

    @Test
    @DisplayName("发送验证码——邮箱格式错误 400")
    void sendEmailCode_InvalidEmail_400() throws Exception {
        EmailCodeDTO dto = new EmailCodeDTO();
        dto.setEmail("bad-email");

        mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("登录——成功返回双 Token")
    void login_Success() throws Exception {
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken("access_token");
        loginVO.setRefreshToken("refresh_token");
        when(authService.login(any(LoginDTO.class))).thenReturn(loginVO);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("tony2024");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access_token"));
    }

    @Test
    @DisplayName("登录——缺密码触发参数校验 400")
    void login_MissingPassword_400() throws Exception {
        String body = "{\"username\":\"tony2024\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("password 密码不能为空"));
    }

    @Test
    @DisplayName("刷新——成功返回新 Access Token")
    void refresh_Success() throws Exception {
        RefreshVO refreshVO = new RefreshVO();
        refreshVO.setAccessToken("new_access_token");
        when(authService.refresh(any(RefreshDTO.class))).thenReturn(refreshVO);

        String body = "{\"refreshToken\":\"refresh_token\"}";

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
    }

    @Test
    @DisplayName("刷新——缺 refreshToken 触发参数校验 400")
    void refresh_MissingToken_400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("refreshToken refreshToken 不能为空"));
    }
}
