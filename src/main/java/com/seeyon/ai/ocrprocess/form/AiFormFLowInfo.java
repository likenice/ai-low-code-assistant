package com.seeyon.ai.ocrprocess.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFormFLowInfo {
    private Long id;
    private String handleStage;
    private Integer handleStageType;
    private String ocrContextPath;
    private String entityContextPath;
    private String pageContextPath;


}
