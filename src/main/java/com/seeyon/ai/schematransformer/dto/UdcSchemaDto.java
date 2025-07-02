package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UdcSchemaDto {
    private JsonNode rootNode;

    private ArrayNode children;

    private Map<String,JsonNode> childByIdMap;

    private JsonNode sourceNode;
}
