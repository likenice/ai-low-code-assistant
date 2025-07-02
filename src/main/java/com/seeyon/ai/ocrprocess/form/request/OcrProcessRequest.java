package com.seeyon.ai.ocrprocess.form.request;


import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.EdocEntityDto;
import lombok.Data;

import java.util.List;

@Data
public class OcrProcessRequest {
    private String tsr;
    private String ocr;
    private String resize;
    private AiFormFLowInfo aiFormFLowInfo ;
    private List<EdocEntityDto> edocEntityDtos ;
}
