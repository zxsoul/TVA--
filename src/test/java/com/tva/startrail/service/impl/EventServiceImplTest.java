package com.tva.startrail.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.tva.startrail.common.BizException;
import com.tva.startrail.common.UserContext;
import com.tva.startrail.constant.EventTypeConstant;
import com.tva.startrail.constant.VisibilityConstant;
import com.tva.startrail.dto.EventCreateDTO;
import com.tva.startrail.dto.EventQueryDTO;
import com.tva.startrail.dto.EventUpdateDTO;
import com.tva.startrail.dto.NodeItemDTO;
import com.tva.startrail.entity.EventsInfo;
import com.tva.startrail.entity.TimelineNode;
import com.tva.startrail.mapper.EventsInfoMapper;
import com.tva.startrail.mapper.TimelineNodeMapper;
import com.tva.startrail.vo.EventDetailVO;
import com.tva.startrail.vo.EventListItemVO;
import com.tva.startrail.vo.NodeAggVO;
import com.tva.startrail.vo.NodeVO;
import com.tva.startrail.vo.PageVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EventServiceImpl 全分支单元测试（PRD F2 / 里程碑 M2）
 * 覆盖：创建多节点同事务批量写入、日期排序与 id 兜底、分页聚合两查询法、
 * 归属校验 404（不暴露他人资源存在性）、选填修改语义、级联逻辑删除
 */
@DisplayName("事件业务单元测试")
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long EVENT_ID = 100L;

    @Mock
    EventsInfoMapper eventsInfoMapper;
    @Mock
    TimelineNodeMapper timelineNodeMapper;
    @InjectMocks
    EventServiceImpl eventService;

    @BeforeAll
    static void initMybatisPlus() {
        // removeById 的逻辑删除 + 更新填充分支依赖 TableInfo 元数据，
        // 纯 Mockito 环境无 SqlSessionFactory，手动初始化一次表元数据
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), EventsInfo.class);
    }

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是泛型字段（M extends BaseMapper<T>），
        // Mockito 字段注入无法按类型变量匹配，需显式塞入
        ReflectionTestUtils.setField(eventService, "baseMapper", eventsInfoMapper);
        UserContext.set(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ---------------- 测试数据构造 ----------------

    private EventsInfo event(Long id, Long userId, String name, Integer type) {
        EventsInfo e = new EventsInfo();
        e.setId(id);
        e.setUserId(userId);
        e.setName(name);
        e.setType(type);
        e.setVisibility(VisibilityConstant.PRIVATE);
        e.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        return e;
    }

    private TimelineNode node(Long id, Long eventId, String happenTime, String content) {
        TimelineNode n = new TimelineNode();
        n.setId(id);
        n.setEventId(eventId);
        n.setHappenTime(LocalDateTime.parse(happenTime.replace(' ', 'T')));
        n.setContent(content);
        return n;
    }

    private NodeItemDTO nodeItem(String happenTime, String content) {
        NodeItemDTO item = new NodeItemDTO();
        item.setHappenTime(LocalDateTime.parse(happenTime.replace(' ', 'T')));
        item.setContent(content);
        return item;
    }

    private NodeAggVO agg(Long eventId, Long count, String start, String end) {
        NodeAggVO vo = new NodeAggVO();
        vo.setEventId(eventId);
        vo.setNodeCount(count);
        vo.setStartTime(LocalDateTime.parse(start.replace(' ', 'T')));
        vo.setEndTime(LocalDateTime.parse(end.replace(' ', 'T')));
        return vo;
    }

    private EventCreateDTO createDTO(Integer type, NodeItemDTO... items) {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setName("第一份工作");
        dto.setType(type);
        dto.setNodes(List.of(items));
        return dto;
    }

    /** 桩住事件落库：模拟雪花 ID 回填与创建时间填充 */
    private void stubEventInsert() {
        when(eventsInfoMapper.insert(any(EventsInfo.class))).thenAnswer(inv -> {
            EventsInfo e = inv.getArgument(0);
            e.setId(EVENT_ID);
            e.setCreateTime(LocalDateTime.of(2026, 11, 25, 12, 0));
            return 1;
        });
    }

    /**
     * 桩住节点批量写入（静态 Db.saveBatch）：按提交顺序回填自增 ID，
     * 返回被写入的节点列表供后续断言
     */
    @SuppressWarnings("unchecked")
    private List<TimelineNode>[] stubNodeBatch(MockedStatic<Db> db) {
        List<TimelineNode>[] saved = new List[1];
        db.when(() -> Db.<TimelineNode>saveBatch(anyList())).thenAnswer(inv -> {
            List<TimelineNode> nodes = inv.getArgument(0);
            long seq = 1000L;
            for (TimelineNode n : nodes) {
                n.setId(seq++);
            }
            saved[0] = nodes;
            return true;
        });
        return saved;
    }

    // ---------------- create ----------------

    @Test
    @DisplayName("创建事件——多节点乱序提交，批量写入后按 happen_time ASC, id ASC 返回")
    void create_MultiNodesUnsorted_SortedByHappenTimeThenId() {
        // 故意乱序 + 同刻节点：验证排序与 id 兜底（PRD F2 规则 2）
        EventCreateDTO dto = createDTO(EventTypeConstant.CAREER,
                nodeItem("2020-07-01 09:00:00", "入职"),
                nodeItem("2019-09-01 08:00:00", "拿到 offer"),
                nodeItem("2020-07-01 09:00:00", "同刻节点"));
        stubEventInsert();

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            List<TimelineNode>[] saved = stubNodeBatch(db);

            EventDetailVO vo = eventService.create(dto);

            // 事件落库：归属当前用户、默认私密
            ArgumentCaptor<EventsInfo> captor = ArgumentCaptor.forClass(EventsInfo.class);
            verify(eventsInfoMapper).insert(captor.capture());
            assertEquals(USER_ID, captor.getValue().getUserId());
            assertEquals("第一份工作", captor.getValue().getName());
            assertEquals(EventTypeConstant.CAREER, captor.getValue().getType());
            assertEquals(VisibilityConstant.PRIVATE, captor.getValue().getVisibility());

            // 节点同事务批量写入一次，且全部挂上事件 ID
            db.verify(() -> Db.saveBatch(anyList()));
            assertEquals(3, saved[0].size());
            saved[0].forEach(n -> assertEquals(EVENT_ID, n.getEventId()));

            // 返回序列：2019 最早；两个 2020 同刻节点按 id 兜底（先提交的排前）
            assertEquals(List.of(1001L, 1000L, 1002L),
                    vo.getNodes().stream().map(NodeVO::getId).toList());
            assertEquals("拿到 offer", vo.getNodes().get(0).getContent());
            // 起止时间取排序后首尾节点
            assertEquals(LocalDateTime.of(2019, 9, 1, 8, 0), vo.getStartTime());
            assertEquals(LocalDateTime.of(2020, 7, 1, 9, 0), vo.getEndTime());
        }
    }

    @Test
    @DisplayName("创建事件——仅一个节点（V2.5 下限），起点即终点")
    void create_SingleNode_StartEqualsEnd() {
        EventCreateDTO dto = createDTO(EventTypeConstant.LIFE,
                nodeItem("2000-01-01 00:00:00", "出生"));
        stubEventInsert();

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            stubNodeBatch(db);

            EventDetailVO vo = eventService.create(dto);

            assertEquals(1, vo.getNodes().size());
            assertEquals(vo.getStartTime(), vo.getEndTime());
            assertEquals(LocalDateTime.of(2000, 1, 1, 0, 0), vo.getStartTime());
        }
    }

    @Test
    @DisplayName("创建事件——主线类型非法抛 400，不落库")
    void create_InvalidType_400() {
        EventCreateDTO dto = createDTO(99, nodeItem("2020-01-01 00:00:00", "节点"));

        BizException e = assertThrows(BizException.class, () -> eventService.create(dto));
        assertEquals(400, e.getCode());
        assertEquals("主线类型不合法（10生活/20学业/30事业/40情感）", e.getMessage());
        verify(eventsInfoMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建事件——未登录抛 401，不落库")
    void create_NotLogin_401() {
        UserContext.clear();
        EventCreateDTO dto = createDTO(EventTypeConstant.LIFE,
                nodeItem("2020-01-01 00:00:00", "节点"));

        BizException e = assertThrows(BizException.class, () -> eventService.create(dto));
        assertEquals(401, e.getCode());
        assertEquals("未登录或 Token 已失效", e.getMessage());
        verify(eventsInfoMapper, never()).insert(any());
    }

    // ---------------- page ----------------

    @Test
    @DisplayName("分页列表——两查询法：MP 分页查事件，第二查询按当前页事件 ID 聚合节点")
    void page_Success_WithNodeAggregation() {
        EventQueryDTO query = new EventQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setType(EventTypeConstant.LIFE);

        EventsInfo e1 = event(100L, USER_ID, "事件A", EventTypeConstant.LIFE);
        EventsInfo e2 = event(101L, USER_ID, "事件B", EventTypeConstant.LIFE);
        when(eventsInfoMapper.selectPage(org.mockito.ArgumentMatchers.<Page<EventsInfo>>any(),
                org.mockito.ArgumentMatchers.<Wrapper<EventsInfo>>any())).thenAnswer(inv -> {
            Page<EventsInfo> p = inv.getArgument(0);
            p.setTotal(2);
            p.setRecords(List.of(e1, e2));
            return p;
        });
        // 第二查询只返回 e1 的聚合（e2 模拟节点已全部逻辑删除的残影）
        when(timelineNodeMapper.aggByEventIds(List.of(100L, 101L)))
                .thenReturn(List.of(agg(100L, 3L, "2018-01-01 00:00:00", "2020-01-01 00:00:00")));

        PageVO<EventListItemVO> vo = eventService.page(query);

        // 分页元数据：total 计事件数，而非节点数（PRD M2 分页聚合点）
        assertEquals(2L, vo.getTotal());
        assertEquals(1L, vo.getPages());
        assertEquals(1L, vo.getCurrent());
        assertEquals(10L, vo.getSize());
        assertEquals(2, vo.getRecords().size());

        EventListItemVO r1 = vo.getRecords().get(0);
        assertEquals(100L, r1.getId());
        assertEquals(3L, r1.getNodeCount());
        assertEquals(LocalDateTime.of(2018, 1, 1, 0, 0), r1.getStartTime());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), r1.getEndTime());

        // 聚合缺失兜底：nodeCount=0，起止时间为 null，不抛异常
        EventListItemVO r2 = vo.getRecords().get(1);
        assertEquals(0L, r2.getNodeCount());
        assertNull(r2.getStartTime());
        assertNull(r2.getEndTime());

        // 聚合只针对当前页事件 ID 批量发起（一次 IN 查询，无 N+1）
        verify(timelineNodeMapper).aggByEventIds(List.of(100L, 101L));
    }

    @Test
    @DisplayName("分页列表——当前页无事件，跳过节点聚合查询")
    void page_EmptyPage_SkipAggregation() {
        EventQueryDTO query = new EventQueryDTO();
        when(eventsInfoMapper.selectPage(org.mockito.ArgumentMatchers.<Page<EventsInfo>>any(),
                org.mockito.ArgumentMatchers.<Wrapper<EventsInfo>>any())).thenAnswer(inv -> {
            Page<EventsInfo> p = inv.getArgument(0);
            p.setTotal(0);
            p.setRecords(List.of());
            return p;
        });

        PageVO<EventListItemVO> vo = eventService.page(query);

        assertEquals(0L, vo.getTotal());
        assertEquals(0, vo.getRecords().size());
        verify(timelineNodeMapper, never()).aggByEventIds(anyList());
    }

    @Test
    @DisplayName("分页列表——筛选主线类型非法抛 400，不发起查询")
    void page_InvalidTypeFilter_400() {
        EventQueryDTO query = new EventQueryDTO();
        query.setType(99);

        BizException e = assertThrows(BizException.class, () -> eventService.page(query));
        assertEquals(400, e.getCode());
        assertEquals("主线类型不合法（10生活/20学业/30事业/40情感）", e.getMessage());
        verify(eventsInfoMapper, never()).selectPage(any(), any());
    }

    // ---------------- detail ----------------

    @Test
    @DisplayName("事件详情——节点按时间正序全量返回，起止时间取首尾节点")
    void detail_Success_StartEndFromFirstLastNode() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, USER_ID, "第一份工作", EventTypeConstant.CAREER));
        when(timelineNodeMapper.selectList(any())).thenReturn(List.of(
                node(1L, EVENT_ID, "2019-09-01 08:00:00", "拿到 offer"),
                node(2L, EVENT_ID, "2020-07-01 09:00:00", "入职"),
                node(3L, EVENT_ID, "2021-03-01 10:00:00", "转正")));

        EventDetailVO vo = eventService.detail(EVENT_ID);

        assertEquals(EVENT_ID, vo.getId());
        assertEquals("第一份工作", vo.getName());
        assertEquals(3, vo.getNodes().size());
        assertEquals(LocalDateTime.of(2019, 9, 1, 8, 0), vo.getStartTime());
        assertEquals(LocalDateTime.of(2021, 3, 1, 10, 0), vo.getEndTime());
        // 排序交给 SQL（happen_time ASC, id ASC），服务层不再二次排序，按序透传
        assertEquals(List.of(1L, 2L, 3L), vo.getNodes().stream().map(NodeVO::getId).toList());
    }

    @Test
    @DisplayName("事件详情——事件不存在抛 404")
    void detail_NotFound_404() {
        when(eventsInfoMapper.selectById(999L)).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> eventService.detail(999L));
        assertEquals(404, e.getCode());
        assertEquals("事件不存在", e.getMessage());
        verify(timelineNodeMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("事件详情——他人事件同样 404（不暴露资源存在性）")
    void detail_NotOwner_404_NoExistenceLeak() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, OTHER_USER_ID, "别人的事", EventTypeConstant.LIFE));

        BizException e = assertThrows(BizException.class, () -> eventService.detail(EVENT_ID));
        assertEquals(404, e.getCode());
        assertEquals("事件不存在", e.getMessage());
        verify(timelineNodeMapper, never()).selectList(any());
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("修改事件——只传名称，主线保持不动（选填语义），返回最新详情")
    void update_NameOnly_PartialSemantics() {
        EventsInfo existing = event(EVENT_ID, USER_ID, "旧名", EventTypeConstant.LIFE);
        when(eventsInfoMapper.selectById(EVENT_ID)).thenReturn(existing);
        when(timelineNodeMapper.selectList(any())).thenReturn(List.of());

        EventUpdateDTO dto = new EventUpdateDTO();
        dto.setName("新名");
        // type 传 null → 跳过

        EventDetailVO vo = eventService.update(EVENT_ID, dto);

        ArgumentCaptor<EventsInfo> captor = ArgumentCaptor.forClass(EventsInfo.class);
        verify(eventsInfoMapper).updateById(captor.capture());
        assertEquals("新名", captor.getValue().getName());
        assertEquals(EventTypeConstant.LIFE, captor.getValue().getType());
        assertEquals("新名", vo.getName());
    }

    @Test
    @DisplayName("修改事件——只传主线，名称保持不动；空白名称视为未传")
    void update_TypeOnlyAndBlankName_PartialSemantics() {
        EventsInfo existing = event(EVENT_ID, USER_ID, "旧名", EventTypeConstant.LIFE);
        when(eventsInfoMapper.selectById(EVENT_ID)).thenReturn(existing);
        when(timelineNodeMapper.selectList(any())).thenReturn(List.of());

        EventUpdateDTO dto = new EventUpdateDTO();
        dto.setType(EventTypeConstant.CAREER);
        dto.setName("   "); // hasText=false → 跳过

        eventService.update(EVENT_ID, dto);

        ArgumentCaptor<EventsInfo> captor = ArgumentCaptor.forClass(EventsInfo.class);
        verify(eventsInfoMapper).updateById(captor.capture());
        assertEquals(EventTypeConstant.CAREER, captor.getValue().getType());
        assertEquals("旧名", captor.getValue().getName());
    }

    @Test
    @DisplayName("修改事件——主线类型非法抛 400，不更新")
    void update_InvalidType_400() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, USER_ID, "事件A", EventTypeConstant.LIFE));

        EventUpdateDTO dto = new EventUpdateDTO();
        dto.setType(99);

        BizException e = assertThrows(BizException.class, () -> eventService.update(EVENT_ID, dto));
        assertEquals(400, e.getCode());
        verify(eventsInfoMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("修改事件——他人事件抛 404，不更新")
    void update_NotOwner_404() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, OTHER_USER_ID, "别人的事", EventTypeConstant.LIFE));

        BizException e = assertThrows(BizException.class,
                () -> eventService.update(EVENT_ID, new EventUpdateDTO()));
        assertEquals(404, e.getCode());
        verify(eventsInfoMapper, never()).updateById(any());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("删除事件——逻辑删除事件并级联逻辑删除全部节点")
    void delete_Success_CascadeNodes() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, USER_ID, "事件A", EventTypeConstant.LIFE));

        eventService.delete(EVENT_ID);

        // 事件逻辑删除：MP 检测到 @TableLogic + updateTime 填充后，
        // 构造携带主键的实体走 deleteById(实体)（生产环境由拦截器转 UPDATE）
        ArgumentCaptor<EventsInfo> captor = ArgumentCaptor.forClass(EventsInfo.class);
        verify(eventsInfoMapper).deleteById(captor.capture());
        assertEquals(EVENT_ID, captor.getValue().getId());
        // 级联：按 event_id 删除该事件下全部节点
        verify(timelineNodeMapper).delete(any());
    }

    @Test
    @DisplayName("删除事件——他人事件抛 404，事件与节点均不动")
    void delete_NotOwner_404_NoCascade() {
        when(eventsInfoMapper.selectById(EVENT_ID))
                .thenReturn(event(EVENT_ID, OTHER_USER_ID, "别人的事", EventTypeConstant.LIFE));

        BizException e = assertThrows(BizException.class, () -> eventService.delete(EVENT_ID));
        assertEquals(404, e.getCode());
        assertEquals("事件不存在", e.getMessage());
        verify(eventsInfoMapper, never()).deleteById(org.mockito.ArgumentMatchers.<EventsInfo>any());
        verify(timelineNodeMapper, never()).delete(any());
    }
}
