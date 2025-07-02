package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GongwenOcrBatchImport {

    private String tid;
    private String type;
    private boolean isUpdate = true;
    private List<String> titleNames;
    private List<JsonNode> ocrSchemas;

}
