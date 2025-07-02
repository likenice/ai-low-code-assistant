package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

import java.util.List;
@Data
public class EdocTableDto {
    private List<EdocGroupDto> edocGroupDtos;
    //grid container
    private String type;
}
