package com.tva.startrail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tva.startrail.common.BizException;
import com.tva.startrail.dto.NodeCreateDTO;
import com.tva.startrail.dto.NodeUpdateDTO;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.mapper.TimelineNodeMapper;
import com.tva.startrail.service.EventService;
import com.tva.startrail.service.NodeService;
import com.tva.startrail.vo.NodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 时间节点业务实现（技术文档 §5.3、§5.4）
 * 归属链：节点 → 所属事件 → 当前用户（经 EventService.getOwnedEvent 校验）
 */
@Service
@RequiredArgsConstructor
public class NodeServiceImpl extends ServiceImpl<TimelineNodeMapper, TimelineNode> implements NodeService {

    private final EventService eventService;

    /**
     * ⑫ 添加节点：不校验时间边界（早于/晚于现有节点均允许，PRD F2 规则 1）
     */
    @Override
    public NodeVO add(Long eventId, NodeCreateDTO dto) {
        eventService.getOwnedEvent(eventId);
        TimelineNode node = new TimelineNode();
        node.setEventId(eventId);
        node.setHappenTime(dto.getHappenTime());
        node.setContent(dto.getContent());
        save(node);
        return toVO(node);
    }

    /**
     * ⑬ 节点列表：happen_time ASC, id ASC
     */
    @Override
    public List<NodeVO> listByEvent(Long eventId) {
        eventService.getOwnedEvent(eventId);
        return list(new LambdaQueryWrapper<TimelineNode>()
                        .eq(TimelineNode::getEventId, eventId)
                        .orderByAsc(TimelineNode::getHappenTime)
                        .orderByAsc(TimelineNode::getId))
                .stream().map(this::toVO).toList();
    }

    /**
     * ⑭ 修改节点：选填语义（null 字段跳过）
     */
    @Override
    public NodeVO update(Long id, NodeUpdateDTO dto) {
        TimelineNode node = getOwnedNode(id);
        if (dto.getHappenTime() != null) {
            node.setHappenTime(dto.getHappenTime());
        }
        if (dto.getContent() != null) {
            node.setContent(dto.getContent());
        }
        updateById(node);
        return toVO(node);
    }

    /**
     * ⑮ 删除节点：本期不校验"最后一个节点"（约束 10003 见技术文档 §7.1）
     */
    @Override
    public void delete(Long id) {
        getOwnedNode(id);
        removeById(id);
    }

    // ---------- 私有方法 ----------

    /**
     * 取节点并校验归属链：节点不存在 → 404；所属事件不属于当前用户 → 404
     */
    private TimelineNode getOwnedNode(Long id) {
        TimelineNode node = getById(id);
        if (node == null) {
            throw new BizException(404, "节点不存在");
        }
        eventService.getOwnedEvent(node.getEventId());
        return node;
    }

    private NodeVO toVO(TimelineNode node) {
        NodeVO vo = new NodeVO();
        vo.setId(node.getId());
        vo.setEventId(node.getEventId());
        vo.setHappenTime(node.getHappenTime());
        vo.setContent(node.getContent());
        return vo;
    }
}
