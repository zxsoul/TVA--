package com.tva.startrail.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时间节点响应（技术文档 §5.3）
 */
@Data
@Schema(description = "时间节点")
public class NodeVO {

    @Schema(description = "节点 ID")
    private Long id;

    @Schema(description = "所属事件 ID")
    private Long eventId;

    @Schema(description = "发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime happenTime;

    @Schema(description = "节点内容")
    private String content;
}
