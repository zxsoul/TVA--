package com.tva.startrail.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 通用分页结果（技术文档 §2.2）：由 MyBatis-Plus IPage 转换
 */
@Data
@Schema(description = "通用分页结果")
public class PageVO<T> {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页条数")
    private Long size;

    @Schema(description = "当前页记录")
    private List<T> records;

    public static <T> PageVO<T> of(IPage<?> page, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPages(page.getPages());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setRecords(records);
        return vo;
    }
}
