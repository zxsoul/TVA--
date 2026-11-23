package com.tva.startrail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录 / 注册成功响应（技术文档 §5.1①③）
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "Access Token（30 分钟）")
    private String accessToken;

    @Schema(description = "Refresh Token（7 天）")
    private String refreshToken;

    @Schema(description = "当前用户信息")
    private UserVO user;
}
