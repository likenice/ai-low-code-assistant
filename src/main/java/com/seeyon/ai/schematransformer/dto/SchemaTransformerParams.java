package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;

@lombok.Data
public class SchemaTransformerParams {
    private String layoutSchema;
    private String template;
    private JsonNode layoutSchemaNode;
    private JsonNode templateNode;
    private Boolean isUseOcrLayout ;// true: 以ocr布局为主, false: 以模板布局为主

}
