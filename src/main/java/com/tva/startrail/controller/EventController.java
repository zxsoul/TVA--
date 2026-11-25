package com.tva.startrail.controller;

import com.tva.startrail.common.Result;
import com.tva.startrail.dto.EventCreateDTO;
import com.tva.startrail.dto.EventQueryDTO;
import com.tva.startrail.dto.EventUpdateDTO;
import com.tva.startrail.service.EventService;
import com.tva.startrail.vo.EventDetailVO;
import com.tva.startrail.vo.EventListItemVO;
import com.tva.startrail.vo.PageVO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事件模块（技术文档 §5.2）
 */
@Tag(name = "事件模块")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "创建事件（内嵌首个节点，同一事务）")
    @PostMapping
    public Result<EventDetailVO> create(@RequestBody @Valid EventCreateDTO dto) {
        return Result.ok(eventService.create(dto));
    }

    @Operation(summary = "我的事件分页列表（可按主线筛选）")
    @GetMapping
    public Result<PageVO<EventListItemVO>> page(@Valid EventQueryDTO query) {
        return Result.ok(eventService.page(query));
    }

    @Operation(summary = "事件详情（含按时间排序的节点序列）")
    @GetMapping("/{id}")
    public Result<EventDetailVO> detail(@PathVariable Long id) {
        return Result.ok(eventService.detail(id));
    }

    @Operation(summary = "修改事件（字段均选填）")
    @PutMapping("/{id}")
    public Result<EventDetailVO> update(@PathVariable Long id, @RequestBody @Valid EventUpdateDTO dto) {
        return Result.ok(eventService.update(id, dto));
    }

    @Operation(summary = "删除事件（逻辑删除，级联节点）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return Result.ok();
    }
}
