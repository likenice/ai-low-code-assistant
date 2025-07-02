package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PageSettingDto {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Double> gridTemplateColumns = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Double> gridTemplateRows = new ArrayList<>();
    private String titleName = "";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String,Object> validation ;
}
