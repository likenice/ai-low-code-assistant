package com.seeyon.ai.ocrprocess.form.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.seeyon.ai.ocrprocess.form.AssociationEntityDto;
import com.seeyon.ai.ocrprocess.form.SelectCtpEnumDto;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRequest {
    @NonNull
    @JsonProperty("ocrJson")
    private String ocrJson;
    @NonNull
    @JsonProperty("path")
    private String path;
    @NonNull
    @JsonProperty("associationEntityInfo")
    private List<AssociationEntityDto> associationEntityInfo;
    @NonNull
    @JsonProperty("selectCtpEnumInfo")
    private List<SelectCtpEnumDto> selectCtpEnumInfo;
    @NonNull
    @JsonProperty("formType")
    // normal udc 应用 form 审批应用
    private String formType = "normal";
    @NonNull
    @JsonProperty("type")
    //0 图片 1 excel
    private Integer type = 0;
    private String appName;
}
