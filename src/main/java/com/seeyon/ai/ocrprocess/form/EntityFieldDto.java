package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.Map;

@Data
public class EntityFieldDto {
    private Map<String,AttributeDto> fieldName;
}
