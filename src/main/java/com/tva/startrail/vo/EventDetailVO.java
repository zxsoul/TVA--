package com.tva.startrail.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件详情（技术文档 §5.2⑦⑨）：含按 happenTime ASC, id ASC 排序的节点序列
 */
@Data
@Schema(description = "事件详情")
public class EventDetailVO {

    @Schema(description = "事件 ID")
    private Long id;

    @Schema(description = "事件名称")
    private String name;

    @Schema(description = "主线：10生活/20学业/30事业/40情感")
    private Integer type;

    @Schema(description = "事件起点（最早节点时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "事件终点（最晚节点时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "节点序列（按发生时间正序）")
    private List<NodeVO> nodes;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
