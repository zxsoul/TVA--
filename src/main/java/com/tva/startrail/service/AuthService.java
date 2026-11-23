package com.tva.startrail.service;

import com.tva.startrail.dto.EmailCodeDTO;
import com.tva.startrail.dto.LoginDTO;
import com.tva.startrail.dto.RefreshDTO;
import com.tva.startrail.dto.RegisterDTO;
import com.tva.startrail.vo.LoginVO;
import com.tva.startrail.vo.RefreshVO;
import com.tva.startrail.vo.UserVO;

public interface AuthService {

    /**
     * 发送邮箱验证码（60s 重发冷却）
     */
    void sendEmailCode(EmailCodeDTO dto);

    /**
     * 注册（邮箱验证码校验 + 查重 + 签发双 Token）
     */
    LoginVO register(RegisterDTO dto);

    /**
     * 登录（签发双 Token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新 Access Token
     */
    RefreshVO refresh(RefreshDTO dto);

    /**
     * 登出（Access Token 加黑名单 + 吊销 Refresh Token）
     */
    void logout(String authorization);

    /**
     * 当前用户信息
     */
    UserVO me();
}
