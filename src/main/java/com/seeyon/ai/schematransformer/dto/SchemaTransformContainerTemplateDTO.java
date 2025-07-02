package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SchemaTransformContainerTemplateDTO {

    //网格 节点样式
    private JsonNode containerNode;
    // 网格component Cell默认节点样式
    private JsonNode containerComponentDefaultNode;
}