package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GongwenOcrImport {

    private String titleNames;
    private JsonNode ocrSchemas;

}
