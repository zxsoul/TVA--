package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 事件分页查询（技术文档 §5.2⑧）
 */
@Data
@Schema(description = "事件分页查询")
public class EventQueryDTO {

    @Schema(description = "页码，从 1 起", defaultValue = "1")
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，上限 100", defaultValue = "10")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private Integer pageSize = 10;

    @Schema(description = "按主线筛选：10生活/20学业/30事业/40情感（选填）")
    private Integer type;
}
