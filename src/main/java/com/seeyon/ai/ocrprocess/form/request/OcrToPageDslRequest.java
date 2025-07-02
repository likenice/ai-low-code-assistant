package com.seeyon.ai.ocrprocess.form.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrToPageDslRequest {
    @NonNull
    private String ocrJson;
    @NonNull
    private List<DataStandardResponse> entityInfo;
}
