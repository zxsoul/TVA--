package com.tva.startrail.constant;

/**
 * 事件可见性常量（技术文档 §5.4.1）
 * 10=公开 / 20=私密；本期固定私密，F6 互鉴上线时再开放修改入口
 */
public final class VisibilityConstant {

    public static final int PUBLIC = 10;
    public static final int PRIVATE = 20;

    private VisibilityConstant() {
    }
}
