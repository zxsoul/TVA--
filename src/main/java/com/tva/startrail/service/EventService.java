package com.tva.startrail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tva.startrail.dto.EventCreateDTO;
import com.tva.startrail.dto.EventQueryDTO;
import com.tva.startrail.dto.EventUpdateDTO;
import com.tva.startrail.entity.EventsInfo;
import com.tva.startrail.vo.EventDetailVO;
import com.tva.startrail.vo.EventListItemVO;
import com.tva.startrail.vo.PageVO;

/**
 * 事件业务（技术文档 §5.2、§5.4）
 */
public interface EventService extends IService<EventsInfo> {

    /**
     * ⑦ 创建事件（内嵌首个节点，同一事务）
     */
    EventDetailVO create(EventCreateDTO dto);

    /**
     * ⑧ 我的事件分页列表（可按主线筛选）
     */
    PageVO<EventListItemVO> page(EventQueryDTO query);

    /**
     * ⑨ 事件详情（含按时间排序的节点序列）
     */
    EventDetailVO detail(Long id);

    /**
     * ⑩ 修改事件（字段均选填）
     */
    EventDetailVO update(Long id, EventUpdateDTO dto);

    /**
     * ⑪ 删除事件（逻辑删除，级联节点，同一事务）
     */
    void delete(Long id);

    /**
     * 取当前用户名下的事件；不存在或不属于当前用户一律 404（不暴露他人资源存在性）
     */
    EventsInfo getOwnedEvent(Long eventId);
}
