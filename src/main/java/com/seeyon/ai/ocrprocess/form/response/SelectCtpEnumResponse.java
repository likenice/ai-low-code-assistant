package com.seeyon.ai.ocrprocess.form.response;


import com.seeyon.ai.ocrprocess.form.SelectCtpEnumDto;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SelectCtpEnumResponse {
    private int status;
    private String code;
    private String message;
    private Map<String, List<SelectCtpEnumDto>> data;
}
