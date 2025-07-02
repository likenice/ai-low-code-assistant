package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

@Data
public class PageColumnsDto {
    private String align;
    private String componentType;
    private String dataFieldCaption;
    private Boolean dataI18n = false;
    private String dataIndex;
    private String dataType;
    private String widthValue;
}
