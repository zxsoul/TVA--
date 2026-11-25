package com.tva.startrail.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tva.startrail.common.BizException;
import com.tva.startrail.dto.NodeCreateDTO;
import com.tva.startrail.dto.NodeUpdateDTO;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.mapper.TimelineNodeMapper;
import com.tva.startrail.service.EventService;
import com.tva.startrail.vo.NodeVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NodeServiceImpl 全分支单元测试（PRD F2 / 里程碑 M2）
 * 覆盖：添加节点（不限时间边界）、列表排序透传、选填修改语义、
 * 删除节点，以及归属链校验（节点 → 事件 → 当前用户，任一环节不通过即 404）
 */
@DisplayName("时间节点业务单元测试")
@ExtendWith(MockitoExtension.class)
class NodeServiceImplTest {

    private static final Long EVENT_ID = 100L;
    private static final Long NODE_ID = 5L;

    @Mock
    TimelineNodeMapper timelineNodeMapper;
    @Mock
    EventService eventService;
    @InjectMocks
    NodeServiceImpl nodeService;

    @BeforeAll
    static void initMybatisPlus() {
        // removeById 的逻辑删除 + 更新填充分支依赖 TableInfo 元数据，
        // 纯 Mockito 环境无 SqlSessionFactory，手动初始化一次表元数据
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), TimelineNode.class);
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是泛型字段（M extends BaseMapper<T>），
        // Mockito 字段注入无法按类型变量匹配，需显式塞入
        ReflectionTestUtils.setField(nodeService, "baseMapper", timelineNodeMapper);
    }

    // ---------------- 测试数据构造 ----------------

    private TimelineNode node(Long id, Long eventId, String happenTime, String content) {
        TimelineNode n = new TimelineNode();
        n.setId(id);
        n.setEventId(eventId);
        n.setHappenTime(LocalDateTime.parse(happenTime.replace(' ', 'T')));
        n.setContent(content);
        return n;
    }

    private NodeCreateDTO createDTO(String happenTime, String content) {
        NodeCreateDTO dto = new NodeCreateDTO();
        dto.setHappenTime(LocalDateTime.parse(happenTime.replace(' ', 'T')));
        dto.setContent(content);
        return dto;
    }

    // ---------------- add ----------------

    @Test
    @DisplayName("添加节点——成功落库并返回 VO，时间早于/晚于现有节点均允许（PRD F2 规则 1）")
    void add_Success() {
        NodeCreateDTO dto = createDTO("2018-01-01 08:00:00", "更早的节点");
        when(timelineNodeMapper.insert(any(TimelineNode.class))).thenAnswer(inv -> {
            TimelineNode n = inv.getArgument(0);
            n.setId(NODE_ID);
            return 1;
        });

        NodeVO vo = nodeService.add(EVENT_ID, dto);

        // 归属链先校验：节点 → 所属事件 → 当前用户
        verify(eventService).getOwnedEvent(EVENT_ID);
        assertEquals(NODE_ID, vo.getId());
        assertEquals(EVENT_ID, vo.getEventId());
        assertEquals(LocalDateTime.of(2018, 1, 1, 8, 0), vo.getHappenTime());
        assertEquals("更早的节点", vo.getContent());
    }

    @Test
    @DisplayName("添加节点——事件不存在或属于他人抛 404，不落库")
    void add_EventNotOwned_404() {
        when(eventService.getOwnedEvent(EVENT_ID))
                .thenThrow(new BizException(404, "事件不存在"));

        NodeCreateDTO dto = createDTO("2018-01-01 08:00:00", "节点");
        BizException e = assertThrows(BizException.class, () -> nodeService.add(EVENT_ID, dto));
        assertEquals(404, e.getCode());
        assertEquals("事件不存在", e.getMessage());
        verify(timelineNodeMapper, never()).insert(any());
    }

    // ---------------- listByEvent ----------------

    @Test
    @DisplayName("节点列表——按 happen_time ASC, id ASC 查询结果透传为 VO")
    void listByEvent_Success() {
        when(timelineNodeMapper.selectList(any())).thenReturn(List.of(
                node(1L, EVENT_ID, "2019-09-01 08:00:00", "起点"),
                node(2L, EVENT_ID, "2020-07-01 09:00:00", "中途"),
                node(3L, EVENT_ID, "2020-07-01 09:00:00", "同刻")));

        List<NodeVO> list = nodeService.listByEvent(EVENT_ID);

        verify(eventService).getOwnedEvent(EVENT_ID);
        assertEquals(3, list.size());
        // 排序由 SQL 保证，服务层按序透传（含同刻 id 兜底顺序）
        assertEquals(List.of(1L, 2L, 3L), list.stream().map(NodeVO::getId).toList());
        assertEquals("起点", list.get(0).getContent());
    }

    @Test
    @DisplayName("节点列表——事件不存在或属于他人抛 404，不查节点")
    void listByEvent_EventNotOwned_404() {
        when(eventService.getOwnedEvent(EVENT_ID))
                .thenThrow(new BizException(404, "事件不存在"));

        BizException e = assertThrows(BizException.class, () -> nodeService.listByEvent(EVENT_ID));
        assertEquals(404, e.getCode());
        verify(timelineNodeMapper, never()).selectList(any());
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("修改节点——只传内容，发生时间保持不动（选填语义）")
    void update_ContentOnly_PartialSemantics() {
        TimelineNode existing = node(NODE_ID, EVENT_ID, "2020-01-01 00:00:00", "旧内容");
        when(timelineNodeMapper.selectById(NODE_ID)).thenReturn(existing);

        NodeUpdateDTO dto = new NodeUpdateDTO();
        dto.setContent("新内容");

        NodeVO vo = nodeService.update(NODE_ID, dto);

        verify(eventService).getOwnedEvent(EVENT_ID);
        verify(timelineNodeMapper).updateById(existing);
        assertEquals("新内容", vo.getContent());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), vo.getHappenTime());
    }

    @Test
    @DisplayName("修改节点——只传发生时间，内容保持不动")
    void update_HappenTimeOnly_PartialSemantics() {
        TimelineNode existing = node(NODE_ID, EVENT_ID, "2020-01-01 00:00:00", "内容");
        when(timelineNodeMapper.selectById(NODE_ID)).thenReturn(existing);

        NodeUpdateDTO dto = new NodeUpdateDTO();
        dto.setHappenTime(LocalDateTime.of(2019, 5, 20, 12, 30));

        NodeVO vo = nodeService.update(NODE_ID, dto);

        verify(timelineNodeMapper).updateById(existing);
        assertEquals(LocalDateTime.of(2019, 5, 20, 12, 30), vo.getHappenTime());
        assertEquals("内容", vo.getContent());
    }

    @Test
    @DisplayName("修改节点——节点不存在抛 404，不校验事件也不更新")
    void update_NodeNotFound_404() {
        when(timelineNodeMapper.selectById(999L)).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> nodeService.update(999L, new NodeUpdateDTO()));
        assertEquals(404, e.getCode());
        assertEquals("节点不存在", e.getMessage());
        verify(eventService, never()).getOwnedEvent(any());
        verify(timelineNodeMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("修改节点——节点属于他人事件抛 404，不更新")
    void update_EventNotOwned_404() {
        TimelineNode existing = node(NODE_ID, EVENT_ID, "2020-01-01 00:00:00", "内容");
        when(timelineNodeMapper.selectById(NODE_ID)).thenReturn(existing);
        when(eventService.getOwnedEvent(EVENT_ID))
                .thenThrow(new BizException(404, "事件不存在"));

        BizException e = assertThrows(BizException.class,
                () -> nodeService.update(NODE_ID, new NodeUpdateDTO()));
        assertEquals(404, e.getCode());
        verify(timelineNodeMapper, never()).updateById(any());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("删除节点——成功逻辑删除（本期不校验最后一个节点）")
    void delete_Success() {
        TimelineNode existing = node(NODE_ID, EVENT_ID, "2020-01-01 00:00:00", "内容");
        when(timelineNodeMapper.selectById(NODE_ID)).thenReturn(existing);

        nodeService.delete(NODE_ID);

        verify(eventService).getOwnedEvent(EVENT_ID);
        // 逻辑删除：MP 检测到 @TableLogic + updateTime 填充后，
        // 构造携带主键的实体走 deleteById(实体)（生产环境由拦截器转 UPDATE）
        org.mockito.ArgumentCaptor<TimelineNode> captor =
                org.mockito.ArgumentCaptor.forClass(TimelineNode.class);
        verify(timelineNodeMapper).deleteById(captor.capture());
        assertEquals(NODE_ID, captor.getValue().getId());
    }

    @Test
    @DisplayName("删除节点——节点不存在抛 404，不删除")
    void delete_NodeNotFound_404() {
        when(timelineNodeMapper.selectById(999L)).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> nodeService.delete(999L));
        assertEquals(404, e.getCode());
        assertEquals("节点不存在", e.getMessage());
        verify(eventService, never()).getOwnedEvent(any());
        verify(timelineNodeMapper, never()).deleteById(org.mockito.ArgumentMatchers.<TimelineNode>any());
    }

    @Test
    @DisplayName("删除节点——节点属于他人事件抛 404，不删除")
    void delete_EventNotOwned_404() {
        TimelineNode existing = node(NODE_ID, EVENT_ID, "2020-01-01 00:00:00", "内容");
        when(timelineNodeMapper.selectById(NODE_ID)).thenReturn(existing);
        when(eventService.getOwnedEvent(EVENT_ID))
                .thenThrow(new BizException(404, "事件不存在"));

        BizException e = assertThrows(BizException.class, () -> nodeService.delete(NODE_ID));
        assertEquals(404, e.getCode());
        verify(timelineNodeMapper, never()).deleteById(org.mockito.ArgumentMatchers.<TimelineNode>any());
    }
}
