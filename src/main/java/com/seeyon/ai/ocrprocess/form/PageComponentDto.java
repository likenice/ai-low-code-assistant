package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class PageComponentDto {
    private PageSettingDto settings = new PageSettingDto();
    private String type;
    @JsonIgnore
    private Integer y;
    @JsonIgnore
    private Integer x1;
    @JsonIgnore
    private Integer x2;
    @JsonIgnore
    private Integer width;
    @JsonIgnore
    private Integer high;
    @JsonIgnore
    private String name;
    @JsonIgnore
    private Boolean gridProcess = false;
    private String ocrRelationId;
    private PageDataSourceDto dataSource;
    private PageCellColRow cellColRow;

}
