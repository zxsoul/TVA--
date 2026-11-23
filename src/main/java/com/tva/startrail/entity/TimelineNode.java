package com.tva.startrail.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 时间节点表（本期仅建实体与 Mapper，节点模块接口后续迭代实现）
 * 排序约定：ORDER BY happen_time ASC, id ASC
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("timeline_node")
public class TimelineNode extends BaseEntity {

    /**
     * 所属事件
     */
    private Long eventId;

    /**
     * 发生时间（排序依据）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime happenTime;

    /**
     * 节点内容
     */
    private String content;
}
