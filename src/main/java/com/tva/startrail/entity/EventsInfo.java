package com.tva.startrail.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 事件表（本期仅建实体与 Mapper，事件模块接口后续迭代实现）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("events_info")
public class EventsInfo extends BaseEntity {

    /**
     * 所属用户
     */
    private Long userId;

    /**
     * 事件名称
     */
    private String name;

    /**
     * 主线：10生活 / 20学业 / 30事业 / 40情感
     */
    private Integer type;

    /**
     * 可见性：10公开 / 20私密（本期默认私密）
     */
    private Integer visibility;
}
