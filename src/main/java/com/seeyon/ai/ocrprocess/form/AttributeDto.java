package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.UUID;

@Data
public class AttributeDto {
    // 字段信息
    private String caption;
    private String ocrRelationId = UUID.randomUUID().toString();
    private String groupOcrRelationId;
    // 字段类型
    private String dataType="STRING";
    // 字段编码
    private String name = "";
    // 是否为空
    private boolean notNull = false;
    // 默认值
    private String defaultValue = "";
    // 是否唯一
    private boolean uniqueness = false;
    // 最小长度
    private Integer minLength = 0;
    // 最大长度
    private Integer maxLength = 0;
    // 最小值
    private String min = "";
    // 最大值
    private String max = "";
    //小数位数
    private Integer decimalDigits = 2;
    // 字段宽度
    @JsonIgnore
    private Integer itemWidth;
    @JsonIgnore
    private Integer y;
    @JsonIgnore
    private Integer y2;
    private Integer columns;
    @JsonIgnore
    private Boolean enumArr = false;
    @JsonIgnore
    private String value;
    // 是否多选
    private boolean multiSelect = false;
    private String relationType = "Many2OneAssociation";
    private String relationApp = "";
    private String relationEntity = "";
    private String relationCode = "";
    private String relationEntityCategory = "";
    private String relationStarter = "";
    private String relationEntityName = "";
    @JsonIgnore
    private boolean believable = true;
    private String information;
    private Integer level = 0;
    private boolean systemField = false;
}