package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EdocGroupDto {
    //     名称
    private String name = "";
    //     内容 只有在dataType 等于field的时候才会存在
    private String value;
    //     内容 只有在datatype 等于field的时候才会存在
    private List<Integer> valueLayout;
    //    tsr坐标
    private List<Integer> layout;
    //    类型 cell单元格 subGroup子容器
    private String type;
    //    类型 label component field字段
    private String dataType;
    //    关联id
    private Long relationId ;
    //    关联名称
    private String relationName ;
    //    subGroup 内容
    private List<EdocGroupDto> edocGroupDtos = new ArrayList<>();
    // 行信息 row,flexRow
    private String rowInfo;
    // 是否被tsr识别 识别为true
    private Boolean tsr = true;
}
