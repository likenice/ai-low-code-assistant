package com.seeyon.ai.ocrprocess.form.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.seeyon.ai.ocrprocess.form.EntityDto;
import lombok.Data;

import java.util.List;

@Data
public class IdentifyRequest {
    // 文件路径
    @JsonProperty("path")
    private String path;
    // 当前应用实体信息
    @JsonProperty("entityInfo")
    private List<EntityDto> entityInfo;
    // AiIdentifyTypeEnum
    @JsonProperty("type")
    private Integer type = 0;
    @JsonProperty("id")
    private Long id ;
}
