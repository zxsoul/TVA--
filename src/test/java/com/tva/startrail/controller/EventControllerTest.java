package com.tva.startrail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tva.startrail.common.BizException;
import com.tva.startrail.common.GlobalExceptionHandler;
import com.tva.startrail.dto.EventCreateDTO;
import com.tva.startrail.dto.EventQueryDTO;
import com.tva.startrail.dto.EventUpdateDTO;
import com.tva.startrail.dto.NodeItemDTO;
import com.tva.startrail.service.EventService;
import com.tva.startrail.vo.EventDetailVO;
import com.tva.startrail.vo.EventListItemVO;
import com.tva.startrail.vo.NodeVO;
import com.tva.startrail.vo.PageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EventController MockMvc 测试（standalone + 全局异常处理器）
 * 重点覆盖 PRD V2.5 创建事件内嵌节点列表的参数校验边界：
 * 空列表 / 缺列表 / 超 50 个 / 节点字段缺失与超长，以及分页参数边界与归属 404 透出
 */
@DisplayName("事件控制器测试")
@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    EventService eventService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------- create ----------------

    @Test
    @DisplayName("创建事件——成功返回事件详情与节点序列")
    void create_Success() throws Exception {
        NodeVO node = new NodeVO();
        node.setId(1L);
        node.setEventId(100L);
        node.setHappenTime(LocalDateTime.of(2019, 9, 1, 8, 0));
        node.setContent("拿到 offer");
        EventDetailVO vo = new EventDetailVO();
        vo.setId(100L);
        vo.setName("第一份工作");
        vo.setType(30);
        vo.setNodes(List.of(node));
        when(eventService.create(any(EventCreateDTO.class))).thenReturn(vo);

        String body = """
                {"name":"第一份工作","type":30,
                 "nodes":[{"happenTime":"2019-09-01 08:00:00","content":"拿到 offer"}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.name").value("第一份工作"))
                .andExpect(jsonPath("$.data.type").value(30))
                .andExpect(jsonPath("$.data.nodes.length()").value(1))
                .andExpect(jsonPath("$.data.nodes[0].content").value("拿到 offer"));
    }

    @Test
    @DisplayName("创建事件——名称为空白触发参数校验 400")
    void create_BlankName_400() throws Exception {
        String body = """
                {"name":"   ","type":30,
                 "nodes":[{"happenTime":"2019-09-01 08:00:00","content":"拿到 offer"}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("name 事件名称不能为空"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——名称超 128 字触发参数校验 400")
    void create_NameTooLong_400() throws Exception {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setName("很".repeat(129));
        dto.setType(30);
        dto.setNodes(List.of(nodeItem("2019-09-01 08:00:00", "节点")));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("name 事件名称不能超过 128 字"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——缺主线类型触发参数校验 400")
    void create_MissingType_400() throws Exception {
        String body = """
                {"name":"第一份工作",
                 "nodes":[{"happenTime":"2019-09-01 08:00:00","content":"拿到 offer"}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("type 主线类型不能为空"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——节点列表为空触发参数校验 400（V2.5 至少一个节点）")
    void create_EmptyNodes_400() throws Exception {
        String body = """
                {"name":"第一份工作","type":30,"nodes":[]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes 至少需要一个时间节点"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——缺节点列表字段触发参数校验 400")
    void create_MissingNodes_400() throws Exception {
        String body = """
                {"name":"第一份工作","type":30}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes 至少需要一个时间节点"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——单次提交 51 个节点触发参数校验 400（V2.5 上限 50）")
    void create_Over50Nodes_400() throws Exception {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setName("第一份工作");
        dto.setType(30);
        dto.setNodes(IntStream.rangeClosed(1, 51)
                .mapToObj(i -> nodeItem("2019-09-01 08:00:00", "节点" + i))
                .collect(Collectors.toList()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes 单次创建节点不能超过 50 个"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——恰好 50 个节点通过校验（V2.5 上限边界）")
    void create_Exactly50Nodes_Pass() throws Exception {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(100L);
        vo.setName("第一份工作");
        vo.setType(30);
        vo.setNodes(List.of());
        when(eventService.create(any(EventCreateDTO.class))).thenReturn(vo);

        EventCreateDTO dto = new EventCreateDTO();
        dto.setName("第一份工作");
        dto.setType(30);
        dto.setNodes(IntStream.rangeClosed(1, 50)
                .mapToObj(i -> nodeItem("2019-09-01 08:00:00", "节点" + i))
                .collect(Collectors.toList()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(eventService).create(argThat(d -> d.getNodes().size() == 50));
    }

    @Test
    @DisplayName("创建事件——内嵌节点缺发生时间触发参数校验 400")
    void create_NodeMissingHappenTime_400() throws Exception {
        String body = """
                {"name":"第一份工作","type":30,"nodes":[{"content":"拿到 offer"}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes[0].happenTime 节点时间不能为空"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——内嵌节点内容为空白触发参数校验 400")
    void create_NodeBlankContent_400() throws Exception {
        String body = """
                {"name":"第一份工作","type":30,
                 "nodes":[{"happenTime":"2019-09-01 08:00:00","content":" "}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes[0].content 节点内容不能为空"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——内嵌节点内容超 512 字触发参数校验 400")
    void create_NodeContentTooLong_400() throws Exception {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setName("第一份工作");
        dto.setType(30);
        dto.setNodes(List.of(nodeItem("2019-09-01 08:00:00", "长".repeat(513))));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("nodes[0].content 节点内容不能超过 512 字"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("创建事件——主线类型非法由业务层拦截 400")
    void create_InvalidType_400_FromService() throws Exception {
        when(eventService.create(any(EventCreateDTO.class)))
                .thenThrow(new BizException(400, "主线类型不合法（10生活/20学业/30事业/40情感）"));

        String body = """
                {"name":"第一份工作","type":99,
                 "nodes":[{"happenTime":"2019-09-01 08:00:00","content":"拿到 offer"}]}
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("主线类型不合法（10生活/20学业/30事业/40情感）"));
    }

    // ---------------- page ----------------

    @Test
    @DisplayName("分页列表——按主线筛选成功返回分页结构")
    void page_Success_WithTypeFilter() throws Exception {
        EventListItemVO item = new EventListItemVO();
        item.setId(100L);
        item.setName("事件A");
        item.setType(10);
        item.setNodeCount(3L);
        PageVO<EventListItemVO> pageVO = new PageVO<>();
        pageVO.setTotal(1L);
        pageVO.setPages(1L);
        pageVO.setCurrent(1L);
        pageVO.setSize(10L);
        pageVO.setRecords(List.of(item));
        when(eventService.page(any(EventQueryDTO.class))).thenReturn(pageVO);

        mockMvc.perform(get("/api/events")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("type", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("事件A"))
                .andExpect(jsonPath("$.data.records[0].nodeCount").value(3));

        verify(eventService).page(argThat((EventQueryDTO q) ->
                q.getType() == 10 && q.getPageNum() == 1 && q.getPageSize() == 10));
    }

    @Test
    @DisplayName("分页列表——页码为 0 触发参数校验 400")
    void page_PageNumBelowOne_400() throws Exception {
        mockMvc.perform(get("/api/events").param("pageNum", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("pageNum 页码最小为 1"));
        verify(eventService, never()).page(any(EventQueryDTO.class));
    }

    @Test
    @DisplayName("分页列表——每页 101 条超上限触发参数校验 400")
    void page_PageSizeOver100_400() throws Exception {
        mockMvc.perform(get("/api/events").param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("pageSize 每页条数最大为 100"));
        verify(eventService, never()).page(any(EventQueryDTO.class));
    }

    // ---------------- detail / update / delete ----------------

    @Test
    @DisplayName("事件详情——成功返回")
    void detail_Success() throws Exception {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(100L);
        vo.setName("第一份工作");
        vo.setType(30);
        vo.setNodes(List.of());
        when(eventService.detail(100L)).thenReturn(vo);

        mockMvc.perform(get("/api/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.name").value("第一份工作"));
    }

    @Test
    @DisplayName("事件详情——不存在或不属于当前用户透出 404")
    void detail_NotFound_404() throws Exception {
        when(eventService.detail(999L)).thenThrow(new BizException(404, "事件不存在"));

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("事件不存在"));
    }

    @Test
    @DisplayName("修改事件——成功返回最新详情")
    void update_Success() throws Exception {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(100L);
        vo.setName("新名");
        vo.setType(30);
        vo.setNodes(List.of());
        when(eventService.update(any(Long.class), any(EventUpdateDTO.class))).thenReturn(vo);

        mockMvc.perform(put("/api/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("新名"));

        verify(eventService).update(argThat((Long id) -> id == 100L),
                argThat((EventUpdateDTO d) -> "新名".equals(d.getName()) && d.getType() == null));
    }

    @Test
    @DisplayName("修改事件——名称超 128 字触发参数校验 400")
    void update_NameTooLong_400() throws Exception {
        EventUpdateDTO dto = new EventUpdateDTO();
        dto.setName("很".repeat(129));

        mockMvc.perform(put("/api/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("name 事件名称不能超过 128 字"));
        verify(eventService, never()).update(any(Long.class), any(EventUpdateDTO.class));
    }

    @Test
    @DisplayName("删除事件——成功返回")
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(eventService).delete(100L);
    }

    @Test
    @DisplayName("删除事件——不存在或不属于当前用户透出 404")
    void delete_NotFound_404() throws Exception {
        org.mockito.Mockito.doThrow(new BizException(404, "事件不存在"))
                .when(eventService).delete(999L);

        mockMvc.perform(delete("/api/events/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("事件不存在"));
    }

    // ---------------- 辅助 ----------------

    private NodeItemDTO nodeItem(String happenTime, String content) {
        NodeItemDTO item = new NodeItemDTO();
        item.setHappenTime(LocalDateTime.parse(happenTime.replace(' ', 'T')));
        item.setContent(content);
        return item;
    }
}
