package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class OcrJsonDto {
    // 坐标
    private List<String> position;
    // 名称
    private Object name;
    // 类型 table row group propertyKey propertyValue
    private String type;
    // 数据值
    private LinkedList<OcrJsonDto> value;
}
