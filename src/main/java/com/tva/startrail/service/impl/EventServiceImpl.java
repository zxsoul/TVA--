package com.tva.startrail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tva.startrail.common.BizException;
import com.tva.startrail.common.UserContext;
import com.tva.startrail.constant.EventTypeConstant;
import com.tva.startrail.constant.VisibilityConstant;
import com.tva.startrail.dto.EventCreateDTO;
import com.tva.startrail.dto.EventQueryDTO;
import com.tva.startrail.dto.EventUpdateDTO;
import com.tva.startrail.entity.EventsInfo;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.mapper.EventsInfoMapper;
import com.tva.startrail.mapper.TimelineNodeMapper;
import com.tva.startrail.service.EventService;
import com.tva.startrail.vo.EventDetailVO;
import com.tva.startrail.vo.EventListItemVO;
import com.tva.startrail.vo.NodeAggVO;
import com.tva.startrail.vo.NodeVO;
import com.tva.startrail.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 事件业务实现（技术文档 §5.2、§5.4）
 * 说明：节点写入 / 级联删除直接走 TimelineNodeMapper，避免与 NodeService 循环依赖
 */
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventsInfoMapper, EventsInfo> implements EventService {

    private final TimelineNodeMapper timelineNodeMapper;

    /**
     * ⑦ 创建事件：事件与首个节点同一事务写入，任一失败整体回滚
     * 多表写入，常规添加事务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDetailVO create(EventCreateDTO dto) {
        checkType(dto.getType());

        EventsInfo event = new EventsInfo();
        event.setUserId(UserContext.require());
        event.setName(dto.getName());
        event.setType(dto.getType());
        event.setVisibility(VisibilityConstant.PRIVATE);
        save(event);

        TimelineNode node = new TimelineNode();
        node.setEventId(event.getId());
        node.setHappenTime(dto.getFirstNode().getHappenTime());
        node.setContent(dto.getFirstNode().getContent());
        timelineNodeMapper.insert(node);

        return buildDetailVO(event, List.of(node));
    }

    /**
     * ⑧ 分页列表 —— 两查询法：先 MP 分页查事件，再按当前页 eventIds 批量聚合节点
     * （避开 GROUP BY 与分页插件共用时 count SQL 失真的问题）
     */
    @Override
    public PageVO<EventListItemVO> page(EventQueryDTO query) {
        Long userId = UserContext.require();
        if (query.getType() != null) {
            checkType(query.getType());
        }

        Page<EventsInfo> page = page(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<EventsInfo>()
                        .eq(EventsInfo::getUserId, userId)
                        .eq(query.getType() != null, EventsInfo::getType, query.getType())
                        .orderByDesc(EventsInfo::getCreateTime));

        List<EventsInfo> events = page.getRecords();
        if (events.isEmpty()) {
            return PageVO.of(page, List.of());
        }

        List<Long> eventIds = events.stream().map(EventsInfo::getId).toList();
        Map<Long, NodeAggVO> aggMap = timelineNodeMapper.aggByEventIds(eventIds).stream()
                .collect(Collectors.toMap(NodeAggVO::getEventId, Function.identity()));

        List<EventListItemVO> records = events.stream().map(e -> {
            EventListItemVO vo = new EventListItemVO();
            vo.setId(e.getId());
            vo.setName(e.getName());
            vo.setType(e.getType());
            NodeAggVO agg = aggMap.get(e.getId());
            vo.setNodeCount(agg != null ? agg.getNodeCount() : 0L);
            if (agg != null) {
                vo.setStartTime(agg.getStartTime());
                vo.setEndTime(agg.getEndTime());
            }
            return vo;
        }).toList();

        return PageVO.of(page, records);
    }

    /**
     * ⑨ 详情：节点按 happen_time ASC, id ASC 全量返回；起止时间取首尾节点，不再发聚合 SQL
     */
    @Override
    public EventDetailVO detail(Long id) {
        EventsInfo event = getOwnedEvent(id);
        List<TimelineNode> nodes = listSortedNodes(id);
        return buildDetailVO(event, nodes);
    }

    /**
     * ⑩ 修改：选填语义（null 字段跳过），返回最新详情
     */
    @Override
    public EventDetailVO update(Long id, EventUpdateDTO dto) {
        EventsInfo event = getOwnedEvent(id);
        if (dto.getType() != null) {
            checkType(dto.getType());
            event.setType(dto.getType());
        }
        if (StringUtils.hasText(dto.getName())) {
            event.setName(dto.getName());
        }
        updateById(event);
        return detail(id);
    }

    /**
     * ⑪ 删除：逻辑删除事件并级联逻辑删除全部节点（@TableLogic 自动转 UPDATE）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwnedEvent(id);
        removeById(id);
        timelineNodeMapper.delete(
                new LambdaQueryWrapper<TimelineNode>().eq(TimelineNode::getEventId, id));
    }

    /**
     * 归属校验：不存在或不属于当前用户一律 404（不暴露他人资源存在性）
     */
    @Override
    public EventsInfo getOwnedEvent(Long eventId) {
        Long userId = UserContext.require();
        EventsInfo event = getById(eventId);
        if (event == null || !event.getUserId().equals(userId)) {
            throw new BizException(404, "事件不存在");
        }
        return event;
    }

    // ---------- 私有方法 ----------

    /**
     * 节点按 happen_time ASC, id ASC 排序查询（id 兜底保证同刻节点顺序稳定）
     */
    private List<TimelineNode> listSortedNodes(Long eventId) {
        return timelineNodeMapper.selectList(new LambdaQueryWrapper<TimelineNode>()
                .eq(TimelineNode::getEventId, eventId)
                .orderByAsc(TimelineNode::getHappenTime)
                .orderByAsc(TimelineNode::getId));
    }

    private EventDetailVO buildDetailVO(EventsInfo event, List<TimelineNode> nodes) {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setName(event.getName());
        vo.setType(event.getType());
        vo.setCreateTime(event.getCreateTime());
        vo.setNodes(nodes.stream().map(this::toNodeVO).toList());
        if (!nodes.isEmpty()) {
            vo.setStartTime(nodes.get(0).getHappenTime());
            vo.setEndTime(nodes.get(nodes.size() - 1).getHappenTime());
        }
        return vo;
    }

    private NodeVO toNodeVO(TimelineNode node) {
        NodeVO vo = new NodeVO();
        vo.setId(node.getId());
        vo.setEventId(node.getEventId());
        vo.setHappenTime(node.getHappenTime());
        vo.setContent(node.getContent());
        return vo;
    }

    private void checkType(Integer type) {
        if (!EventTypeConstant.isValid(type)) {
            throw new BizException(400, "主线类型不合法（10生活/20学业/30事业/40情感）");
        }
    }
}
/*
两个逻辑方面可能存在差异的地方
一个分页查询的时候，返回的是查询到的事件以及他们所包含的时间点，但是返回的数量确实计算事件的数量，容易算成时间点的总数量
二是添加事件的时候，默认必须存在至少一个时间节点，并且只能存在一个节点
 */