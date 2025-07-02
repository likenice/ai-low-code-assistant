package com.seeyon.ai.ocrprocess.enums;

public enum UdcDataTypeEnum {
    STRING("STRING", "文本"),
    INTEGER("INTEGER", "整数"),
    BIGINTEGER("BIGINTEGER", "长整数"),
    MULTILINESTRING("MULTILINESTRING", "多行文本"),
    DECIMAL("DECIMAL", "小数"),
    CURRENCY("CURRENCY", "货币"),
    DATE("DATE", "日期"),
    TIME("TIME", "时间"),
    DATETIME("DATETIME", "日期时间"),
    BOOLEAN("BOOLEAN", "布尔"),
    ATTACHMENT("ATTACHMENT", "附件"),
    ENUM("ENUM", "选项集"),
    CTPENUM("CTPENUM", "枚举"),
    ENTITY("ENTITY", "实体"),
    CONTENT("CONTENT", "正文");

    private String code;
    private String caption;

    private UdcDataTypeEnum(String code, String caption) {
        this.code = code;
        this.caption = caption;
    }

    public String code() {
        return this.code;
    }

    public String getCaption() {
        return this.caption;
    }
}
