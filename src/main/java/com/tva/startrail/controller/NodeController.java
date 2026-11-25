package com.tva.startrail.controller;

import com.tva.startrail.common.Result;
import com.tva.startrail.dto.NodeCreateDTO;
import com.tva.startrail.dto.NodeUpdateDTO;
import com.tva.startrail.service.NodeService;
import com.tva.startrail.vo.NodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 时间节点模块（技术文档 §5.3）
 * 路径分两段：事件维度 /api/events/{eventId}/nodes（⑫⑬），节点维度 /api/nodes/{id}（⑭⑮）
 */
@Tag(name = "时间节点模块")
@RestController
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @Operation(summary = "添加时间节点")
    @PostMapping("/api/events/{eventId}/nodes")
    public Result<NodeVO> add(@PathVariable Long eventId, @RequestBody @Valid NodeCreateDTO dto) {
        return Result.ok(nodeService.add(eventId, dto));
    }

    @Operation(summary = "节点列表（按 happenTime ASC, id ASC）")
    @GetMapping("/api/events/{eventId}/nodes")
    public Result<List<NodeVO>> list(@PathVariable Long eventId) {
        return Result.ok(nodeService.listByEvent(eventId));
    }

    @Operation(summary = "修改时间节点（字段均选填）")
    @PutMapping("/api/nodes/{id}")
    public Result<NodeVO> update(@PathVariable Long id, @RequestBody @Valid NodeUpdateDTO dto) {
        return Result.ok(nodeService.update(id, dto));
    }

    @Operation(summary = "删除时间节点（逻辑删除）")
    @DeleteMapping("/api/nodes/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        nodeService.delete(id);
        return Result.ok();
    }
}
