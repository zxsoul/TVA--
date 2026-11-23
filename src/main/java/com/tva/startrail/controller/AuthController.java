package com.tva.startrail.controller;

import com.tva.startrail.common.Result;
import com.tva.startrail.dto.EmailCodeDTO;
import com.tva.startrail.dto.LoginDTO;
import com.tva.startrail.dto.RefreshDTO;
import com.tva.startrail.dto.RegisterDTO;
import com.tva.startrail.service.AuthService;
import com.tva.startrail.vo.LoginVO;
import com.tva.startrail.vo.RefreshVO;
import com.tva.startrail.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权模块（技术文档 §5.1）
 */
@Tag(name = "鉴权模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送邮箱验证码（60s 重发冷却）")
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@RequestBody @Valid EmailCodeDTO dto) {
        authService.sendEmailCode(dto);
        return Result.ok("验证码已发送，5 分钟内有效", null);
    }

    @Operation(summary = "注册（邮箱验证码校验 + 邮箱查重，成功即登录）")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "刷新 Access Token（凭 Refresh Token）")
    @PostMapping("/refresh")
    public Result<RefreshVO> refresh(@RequestBody @Valid RefreshDTO dto) {
        return Result.ok(authService.refresh(dto));
    }

    @Operation(summary = "登出（黑名单 + 吊销 Refresh Token）")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return Result.ok();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }
}
