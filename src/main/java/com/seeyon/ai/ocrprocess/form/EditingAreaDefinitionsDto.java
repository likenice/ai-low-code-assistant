package com.seeyon.ai.ocrprocess.form;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

// 富文本信息DTO
@Data
public class EditingAreaDefinitionsDto {
    @JsonProperty("keywords")
    private List<String> keywords;
    @JsonProperty("type")
    private String type;
    @JsonProperty("order")
    private Integer order;
    @JsonProperty("weight")
    private Double weight;
    @JsonProperty("match_method")
    private String matchMethod ;
}
