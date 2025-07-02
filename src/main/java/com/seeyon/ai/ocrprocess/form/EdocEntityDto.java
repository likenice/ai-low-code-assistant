package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

@Data
public class EdocEntityDto {
    // 字段名称
    private String caption;
    // 字段编码
    private String fullName;
    // 关联应用
    private String relationApp;
    // 关联实体
    private String relationEntity;
    // 字段类型
    private String dataType;

}