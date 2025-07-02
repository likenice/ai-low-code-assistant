package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

@Data
public class EnumDto {
    // 枚举名称
    private String caption;
    // 枚举类型 SAME_LEVEL MULTI_LEVEL CASCADE
    private String enumType;
    // 枚举编码
    private String code;
    // 枚举描述
    private String description = "";
    // 枚举fullName
    private String fullName;



}