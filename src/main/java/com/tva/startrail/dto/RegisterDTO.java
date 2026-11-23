package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（技术文档 §5.1①）
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @Schema(description = "登录账号，4~32 位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度须为 4~32 位")
    private String username;

    @Schema(description = "密码，8~64 位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度须为 8~64 位")
    private String password;

    @Schema(description = "绑定邮箱（唯一，一邮箱一账号）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    @Schema(description = "邮箱验证码（先调 /api/auth/email-code 获取）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字")
    private String emailCode;

    @Schema(description = "昵称（选填，缺省 = username）")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;
}
