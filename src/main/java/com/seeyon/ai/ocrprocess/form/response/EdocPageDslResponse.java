package com.seeyon.ai.ocrprocess.form.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.seeyon.ai.ocrprocess.form.EdocGroupsDslDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonPropertyOrder({"titleName", "group"})
public class EdocPageDslResponse {
    private String titleName;
    private String resize;
    private List<EdocGroupsDslDto> groups = new ArrayList<>();


}
