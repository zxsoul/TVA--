package com.tva.startrail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tva.startrail.common.BizException;
import com.tva.startrail.common.GlobalExceptionHandler;
import com.tva.startrail.dto.NodeCreateDTO;
import com.tva.startrail.dto.NodeUpdateDTO;
import com.tva.startrail.service.NodeService;
import com.tva.startrail.vo.NodeVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * NodeController MockMvc 测试（standalone + 全局异常处理器）
 * 覆盖：事件维度 /api/events/{eventId}/nodes 与节点维度 /api/nodes/{id} 两组路径，
 * 参数校验边界与归属链 404 透出
 */
@DisplayName("时间节点控制器测试")
@ExtendWith(MockitoExtension.class)
class NodeControllerTest {

    @Mock
    NodeService nodeService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NodeController(nodeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private NodeVO nodeVO(Long id, Long eventId, String content) {
        NodeVO vo = new NodeVO();
        vo.setId(id);
        vo.setEventId(eventId);
        vo.setHappenTime(LocalDateTime.of(2020, 1, 1, 8, 0));
        vo.setContent(content);
        return vo;
    }

    // ---------------- add ----------------

    @Test
    @DisplayName("添加节点——成功返回节点 VO")
    void add_Success() throws Exception {
        when(nodeService.add(eq(100L), any(NodeCreateDTO.class)))
                .thenReturn(nodeVO(5L, 100L, "转正"));

        String body = """
                {"happenTime":"2020-01-01 08:00:00","content":"转正"}
                """;

        mockMvc.perform(post("/api/events/100/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.eventId").value(100))
                .andExpect(jsonPath("$.data.content").value("转正"));

        verify(nodeService).add(eq(100L), argThat(d ->
                "转正".equals(d.getContent())
                        && d.getHappenTime().equals(LocalDateTime.of(2020, 1, 1, 8, 0))));
    }

    @Test
    @DisplayName("添加节点——缺发生时间触发参数校验 400")
    void add_MissingHappenTime_400() throws Exception {
        mockMvc.perform(post("/api/events/100/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"转正\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("happenTime 发生时间不能为空"));
        verify(nodeService, never()).add(any(), any());
    }

    @Test
    @DisplayName("添加节点——内容为空白触发参数校验 400")
    void add_BlankContent_400() throws Exception {
        mockMvc.perform(post("/api/events/100/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"happenTime\":\"2020-01-01 08:00:00\",\"content\":\" \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("content 节点内容不能为空"));
        verify(nodeService, never()).add(any(), any());
    }

    @Test
    @DisplayName("添加节点——内容超 512 字触发参数校验 400")
    void add_ContentTooLong_400() throws Exception {
        NodeCreateDTO dto = new NodeCreateDTO();
        dto.setHappenTime(LocalDateTime.of(2020, 1, 1, 8, 0));
        dto.setContent("长".repeat(513));

        mockMvc.perform(post("/api/events/100/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("content 节点内容不能超过 512 字"));
        verify(nodeService, never()).add(any(), any());
    }

    @Test
    @DisplayName("添加节点——事件不存在或不属于当前用户透出 404")
    void add_EventNotFound_404() throws Exception {
        when(nodeService.add(eq(999L), any(NodeCreateDTO.class)))
                .thenThrow(new BizException(404, "事件不存在"));

        mockMvc.perform(post("/api/events/999/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"happenTime\":\"2020-01-01 08:00:00\",\"content\":\"转正\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("事件不存在"));
    }

    // ---------------- list ----------------

    @Test
    @DisplayName("节点列表——成功返回按时间正序的节点数组")
    void list_Success() throws Exception {
        when(nodeService.listByEvent(100L)).thenReturn(List.of(
                nodeVO(1L, 100L, "起点"),
                nodeVO(2L, 100L, "终点")));

        mockMvc.perform(get("/api/events/100/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("起点"))
                .andExpect(jsonPath("$.data[1].content").value("终点"));
    }

    @Test
    @DisplayName("节点列表——事件不存在或不属于当前用户透出 404")
    void list_EventNotFound_404() throws Exception {
        when(nodeService.listByEvent(999L)).thenThrow(new BizException(404, "事件不存在"));

        mockMvc.perform(get("/api/events/999/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("事件不存在"));
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("修改节点——成功返回更新后的节点 VO")
    void update_Success() throws Exception {
        when(nodeService.update(eq(5L), any(NodeUpdateDTO.class)))
                .thenReturn(nodeVO(5L, 100L, "新内容"));

        mockMvc.perform(put("/api/nodes/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"新内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.content").value("新内容"));

        verify(nodeService).update(eq(5L), argThat((NodeUpdateDTO d) ->
                "新内容".equals(d.getContent()) && d.getHappenTime() == null));
    }

    @Test
    @DisplayName("修改节点——内容超 512 字触发参数校验 400")
    void update_ContentTooLong_400() throws Exception {
        NodeUpdateDTO dto = new NodeUpdateDTO();
        dto.setContent("长".repeat(513));

        mockMvc.perform(put("/api/nodes/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("content 节点内容不能超过 512 字"));
        verify(nodeService, never()).update(any(Long.class), any(NodeUpdateDTO.class));
    }

    @Test
    @DisplayName("修改节点——节点不存在透出 404")
    void update_NodeNotFound_404() throws Exception {
        when(nodeService.update(eq(999L), any(NodeUpdateDTO.class)))
                .thenThrow(new BizException(404, "节点不存在"));

        mockMvc.perform(put("/api/nodes/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"新内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("节点不存在"));
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("删除节点——成功返回")
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/nodes/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(nodeService).delete(5L);
    }

    @Test
    @DisplayName("删除节点——节点不存在透出 404")
    void delete_NodeNotFound_404() throws Exception {
        doThrow(new BizException(404, "节点不存在")).when(nodeService).delete(999L);

        mockMvc.perform(delete("/api/nodes/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("节点不存在"));
    }
}
