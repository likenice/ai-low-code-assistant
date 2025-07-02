package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FieldDto {
    // 分组信息
    private String ocrRelationId = UUID.randomUUID().toString();
    private String caption;
    // 列数
    private Integer columns;
    // 容器宽度
    @JsonIgnore
    private Integer contWidth;
    // 字段信息
    private List<AttributeDto> attributeDtoList;

}