package com.tva.startrail.constant;

/**
 * 主线类型常量（技术文档 §5.4.1）
 * 10=生活 / 20=学业 / 30=事业 / 40=情感
 */
public final class EventTypeConstant {

    public static final int LIFE = 10;
    public static final int STUDY = 20;
    public static final int CAREER = 30;
    public static final int EMOTION = 40;

    private EventTypeConstant() {
    }

    public static boolean isValid(Integer type) {
        return type != null
                && (type == LIFE || type == STUDY || type == CAREER || type == EMOTION);
    }
}
