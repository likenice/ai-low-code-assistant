package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonPropertyOrder({"titleName", "gridType", "gridTemplateColumns", "gridTemplateRows", "gap", "flexWrap", "columns", "flexDirection", "alignItems"
        , "titleDisplay", "validation", "align"})
public class EdocSettingDslDto {
    private String titleName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String gridType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String justifyContent;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> gridTemplateColumns;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Integer> gridTemplateRows;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String gap;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String flexWrap;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer columns;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String flexDirection;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alignItems;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String titleDisplay;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Boolean> validation;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String align;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer flexRowSize;
    private List<Integer> layout;
}
