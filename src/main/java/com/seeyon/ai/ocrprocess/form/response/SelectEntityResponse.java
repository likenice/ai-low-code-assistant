package com.seeyon.ai.ocrprocess.form.response;


import com.seeyon.ai.ocrprocess.form.EntityDto;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SelectEntityResponse {
    private int status;
    private String code;
    private String message;
    private Map<String, List<EntityDto>> data;
}
