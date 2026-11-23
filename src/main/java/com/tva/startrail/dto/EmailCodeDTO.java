package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送邮箱验证码请求（技术文档 §5.1②）
 */
@Data
@Schema(description = "发送邮箱验证码请求")
public class EmailCodeDTO {

    @Schema(description = "邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;
}
