package com.seeyon.ai.ocrprocess.form.response;


import lombok.Data;

@Data
public class AiFormFlowResponse {
    private Integer handleStageType;
    private String handleStage;
    private String result="";
}
