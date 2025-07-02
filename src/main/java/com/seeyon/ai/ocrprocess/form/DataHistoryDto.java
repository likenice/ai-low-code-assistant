package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

@Data
public class DataHistoryDto {
    private String creationTime ;
    private String assistantType;
    private String formName;
    private String status;
    private Integer elements;
    private Integer duration;
}
