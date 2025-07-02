package com.seeyon.ai.ocrprocess.enums;

public enum UdcStyleTypeEnum {
    COLLAPSE("collapse", "分组"),
    INPUT("input", "文本"),
    GRID("grid", "网格"),
    DATAGRID("dataGrid", "重复表录入"),
    DATAGRIDVIEW("dataGridView", "重复表查询"),
    REFERENCE("reference", "参照"),
    INPUTNUMBER("inputNumber", "数字"),
    CURRENCY("currency", "货币"),
    DATEPICKER("datePicker", "日期"),
    TIMEPICKER("timePicker", "时间"),
    SWITCH("switch", "开关"),
    TREESELECT("treeSelect", "下拉选择"),
    TEXTAREA("textArea", "多行文本"),
    UIBUSINESSCONTENT("uiBusinessContent", "正文"),
    ATTACHMENT("attachment", "附件"),
    UIBUSINESSEDOCMARK("uiBusinessEdocMark", "文号组件"),
    UIBUSSINESSEDOCOPINIONBOX("uiBusinessEdocOpinionBox", "意见组件"),
    UIBUSINESSSELECTPEOPLE("uiBusinessSelectPeople", "选人规则"),
    CONTAINER("container", "容器");

    private String type;
    private String name;

    private UdcStyleTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }
}
