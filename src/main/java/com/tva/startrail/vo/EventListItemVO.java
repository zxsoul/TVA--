package com.tva.startrail.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件列表项（技术文档 §5.2⑧）
 * nodeCount / startTime / endTime 由节点聚合得出，不冗余存储
 */
@Data
@Schema(description = "事件列表项")
public class EventListItemVO {

    @Schema(description = "事件 ID")
    private Long id;

    @Schema(description = "事件名称")
    private String name;

    @Schema(description = "主线：10生活/20学业/30事业/40情感")
    private Integer type;

    @Schema(description = "节点数量")
    private Long nodeCount;

    @Schema(description = "事件起点（最早节点时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "事件终点（最晚节点时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}
