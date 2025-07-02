package com.seeyon.ai.schematransformer.dto;

public enum TemplateTagType {

    /**
     * 文单标题
     */
    TITLE("文单标题"),

    /**
     * 主网格
     */
    MAIN_GRID("主网格"),

    /**
     * 删除
     */
    DELETE("删除");

    private String value;
    TemplateTagType(String value) {
        this.value = value;

    }
    public String getValue() {
        return value;
    }
}
