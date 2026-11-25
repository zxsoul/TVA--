package com.tva.startrail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 添加时间节点请求（技术文档 §5.3⑫）
 */
@Data
@Schema(description = "添加时间节点请求")
public class NodeCreateDTO {

    @Schema(description = "发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发生时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime happenTime;

    @Schema(description = "节点内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点内容不能为空")
    @Size(max = 512, message = "节点内容不能超过 512 字")
    private String content;
}
