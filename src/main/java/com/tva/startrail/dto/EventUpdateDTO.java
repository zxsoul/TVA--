package com.tva.startrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改事件请求（技术文档 §5.2⑩）：字段均选填，传什么改什么
 * visibility 本期不开放修改，默认私密（20）
 */
@Data
@Schema(description = "修改事件请求")
public class EventUpdateDTO {

    @Schema(description = "事件名称")
    @Size(max = 128, message = "事件名称不能超过 128 字")
    private String name;

    @Schema(description = "主线：10生活/20学业/30事业/40情感")
    private Integer type;
}
