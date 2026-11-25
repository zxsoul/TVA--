package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建事件请求（技术文档 §5.2⑦）：事件与首个节点同一事务写入
 */
@Data
@Schema(description = "创建事件请求")
public class EventCreateDTO {

    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 128, message = "事件名称不能超过 128 字")
    private String name;

    @Schema(description = "主线：10生活/20学业/30事业/40情感", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主线类型不能为空")
    private Integer type;

    @Schema(description = "内嵌首个时间节点", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "首个节点不能为空")
    @Valid
    private FirstNodeDTO firstNode;
}
