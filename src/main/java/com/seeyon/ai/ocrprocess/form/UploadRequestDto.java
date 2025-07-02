package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

@Data
public class UploadRequestDto {

    private String fileName;
    private String appName;
    private String path;
    private String apiKey;
}
