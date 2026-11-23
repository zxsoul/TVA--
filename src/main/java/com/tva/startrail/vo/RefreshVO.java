package com.tva.startrail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 刷新 Token 响应（技术文档 §5.1④）
 */
@Data
@Schema(description = "刷新 Token 响应")
public class RefreshVO {

    @Schema(description = "新的 Access Token（30 分钟）")
    private String accessToken;
}
