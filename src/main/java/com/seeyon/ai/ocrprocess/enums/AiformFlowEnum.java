package com.seeyon.ai.ocrprocess.enums;

public enum AiformFlowEnum {
    ocrStart(1, "正在识别文本内容...."),
    specialItem(2, "特殊字符校验...."),
    editingAreaDetector(3, "解析复杂文本格式...."),
    tableStructureGenerate(4, "构建表单数据结构...."),
    entityDslCreate(5, "智能识别字段类型...."),
    pageDslTransfer(6, "页面Dsl转换...."),
    ocrServerError(1001, "ocr服务执行失败...."),
    ocrFieldNull(1002, "ocr未识别出字段信息...."),
    entityTransferError(1003, "实体生成失败...."),
    ocrDslTransferError(1004, "ocrDsl转换失败...."),
    ocrProcessError(1000, "ocr后处理失败....");

    private Integer code;
    private String caption;

    private AiformFlowEnum(Integer code, String caption) {
        this.code = code;
        this.caption = caption;
    }

    public Integer code() {
        return this.code;
    }

    public String getCaption() {
        return this.caption;
    }
}
