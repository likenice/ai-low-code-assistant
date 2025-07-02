package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageGroupDto {
    private String name = "";
    private String type = "";
    private Integer col = 0;
    private String ocrRelationId  = "";
    private PageSettingDto settings =new PageSettingDto();
    private List<PageComponentDto> components = new ArrayList<>();
}
