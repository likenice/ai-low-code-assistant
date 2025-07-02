package com.seeyon.ai.ocrprocess.form.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seeyon.ai.ocrprocess.form.AssociationEntityDto;
import com.seeyon.ai.ocrprocess.form.EntityDto;
import com.seeyon.ai.ocrprocess.form.SelectCtpEnumDto;
import lombok.Data;

import java.util.List;

@Data
public class UdcFormGenerate {
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
    @JsonProperty("ocrJson")
    private String ocrJson;
    @JsonProperty("associationEntityInfo")
    private List<AssociationEntityDto> associationEntityInfo;
    @JsonProperty("selectCtpEnumInfo")
    private List<SelectCtpEnumDto> selectCtpEnumInfo;
    @JsonProperty("formType")
    // normal udc 应用 form 审批应用
    private String formType = "normal";
    @JsonProperty("appName")
    private String appName ="";

}
