package com.seeyon.ai.schematransformer.enums;

public enum LayoutTypeEnum {
    ONE_CELL_UP_DOWN("oneCellUpDown"),//上下布局(在同一个单元格中)
    TWO_CELL_UP_DOWN("twoCellUpDown"),//上下布局(在同一个单元格中)
    SIMPLE("simple"),
    COMPLEX("complex");

    private final String value;

    LayoutTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 