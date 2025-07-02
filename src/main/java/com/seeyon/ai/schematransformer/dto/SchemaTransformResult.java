package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SchemaTransformResult {
    private NodePosition nodePosition;
    private JsonNode layoutNode;
}