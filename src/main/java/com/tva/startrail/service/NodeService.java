package com.tva.startrail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tva.startrail.dto.NodeCreateDTO;
import com.tva.startrail.dto.NodeUpdateDTO;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.vo.NodeVO;

import java.util.List;

/**
 * 时间节点业务（技术文档 §5.3、§5.4）
 */
public interface NodeService extends IService<TimelineNode> {

    /**
     * ⑫ 添加节点（不校验时间边界，事件跨度随数据自然扩展）
     */
    NodeVO add(Long eventId, NodeCreateDTO dto);

    /**
     * ⑬ 节点列表（happenTime ASC, id ASC）
     */
    List<NodeVO> listByEvent(Long eventId);

    /**
     * ⑭ 修改节点（字段均选填）
     */
    NodeVO update(Long id, NodeUpdateDTO dto);

    /**
     * ⑮ 删除节点（逻辑删除；本期不校验"最后一个节点"，见技术文档 §7.1）
     */
    void delete(Long id);
}
