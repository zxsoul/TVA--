package com.tva.startrail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.vo.NodeAggVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TimelineNodeMapper extends BaseMapper<TimelineNode> {

    /**
     * 按事件批量聚合节点（技术文档 §5.4.3-⑧）：COUNT / MIN / MAX
     * 用于事件分页列表的第二查询，命中 idx_event_time
     */
    @Select("<script>" +
            "SELECT event_id AS eventId, COUNT(*) AS nodeCount, " +
            "MIN(happen_time) AS startTime, MAX(happen_time) AS endTime " +
            "FROM timeline_node WHERE deleted = 0 AND event_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY event_id" +
            "</script>")
    List<NodeAggVO> aggByEventIds(@Param("ids") List<Long> ids);
}
