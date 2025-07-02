package com.seeyon.ai.schematransformer.dto;

public enum OcrGroupType {

    //-------------------------------udc表单------------------------------
    /**
     * 页面属性
     */
    PAGE_PROPS("pageProps"),

    /**
     * 分组
     */
    COLLAPSE("collapse"),

    /**
     * 重复表
     */
    DATA_GRID("dataGrid"),

    /**
     * 页签
     */
    TABS("tabs"),

    //-------------------------------公文------------------------------


    /**
     * 表格
     */
    GRID("grid"),

    /**
     * 容器
     */
    CONTAINER("container"),

    /**
     * 网格cell
     */
    GRID_CELL("gridCell");

    private String value;
    OcrGroupType(String value) {
        this.value = value;

    }
    public String getValue() {
        return value;
    }
}
