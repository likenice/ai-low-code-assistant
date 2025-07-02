package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.List;

@Data
public class AttributeGroupDto {
    private String appId;
    private List<AttributeDto> attributeDtoList;
    private String caption;
    private Long entityId;
    private String id;
    private String guid;
}
