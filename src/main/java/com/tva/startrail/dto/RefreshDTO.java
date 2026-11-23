package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Access Token 请求（技术文档 §5.1④）
 */
@Data
@Schema(description = "刷新 Access Token 请求")
public class RefreshDTO {

    @Schema(description = "Refresh Token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
