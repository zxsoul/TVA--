package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（技术文档 §5.1③）
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;
}
