package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GongwenOcrBatchImportResult {

    private boolean isAllSuccess = true;
    private List<GongwenOcrBatchResult> message;


}


