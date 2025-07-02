package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.Map;

@Data
public class DataJsonDto {

    private String tableName;

    private Map fieldsInfo;
}
