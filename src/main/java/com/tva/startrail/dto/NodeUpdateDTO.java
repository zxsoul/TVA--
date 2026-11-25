package com.tva.startrail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修改时间节点请求（技术文档 §5.3⑭）：字段均选填
 */
@Data
@Schema(description = "修改时间节点请求")
public class NodeUpdateDTO {

    @Schema(description = "发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime happenTime;

    @Schema(description = "节点内容")
    @Size(max = 512, message = "节点内容不能超过 512 字")
    private String content;
}
