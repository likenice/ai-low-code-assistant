package com.seeyon.ai.ocrprocess.form;

//import com.alibaba.fastjson.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"groupId", "groupName", "groupType"})
public class EdocReferGroupDto {
    private Long groupId;
    private String groupName;
    private String groupType;
}
