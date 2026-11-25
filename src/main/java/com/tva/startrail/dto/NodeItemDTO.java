package com.tva.startrail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内嵌时间节点（技术文档 §5.2⑦）：创建事件时随事件批量写入
 */
@Data
@Schema(description = "内嵌时间节点")
public class NodeItemDTO {

    @Schema(description = "发生时间", requiredMode = Schema.RequiredMode.REQUIRED)//在接口文档标记为必填
    @NotNull(message = "节点时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime happenTime;

    @Schema(description = "节点内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点内容不能为空")
    @Size(max = 512, message = "节点内容不能超过 512 字")
    private String content;
}
