package com.seeyon.ai.ocrprocess.form.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.seeyon.ai.ocrprocess.form.EdocEntityDto;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdocIdentifyRequest {
    @NonNull
    private String path;
    @NonNull
    private List<EdocEntityDto> entityInfo;
    private Integer type = 0;
    private Long id;
}
