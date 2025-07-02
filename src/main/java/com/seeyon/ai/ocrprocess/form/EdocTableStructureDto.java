package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EdocTableStructureDto {
    @JsonProperty("fn")
    private String fn;
    @JsonProperty("nLayout")
    private String nLayout;
    @JsonProperty("fv")
    private String fv;
    @JsonProperty("vLayout")
    private String vLayout;
    @JsonProperty("type")
    private String type;
}
