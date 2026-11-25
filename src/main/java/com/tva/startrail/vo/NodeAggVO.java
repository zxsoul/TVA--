package com.tva.startrail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点聚合结果（技术文档 §5.4.3-⑧）：仅供事件列表聚合查询内部使用，不作接口出参
 */
@Data
@Schema(hidden = true)
public class NodeAggVO {

    private Long eventId;

    private Long nodeCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
