package com.seeyon.ai.ocrprocess.form;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
//@Schema(description = "提示词服务调用参数DTO")
public class AiPromptSvcCallDto implements Serializable {
    private static final long serialVersionUID = 168165105620021208L;
//    @Schema(description = "提示词标识-提示词英文名称")
    private String promptCode;

//    @Schema(description = "用户输入")
    private String input;

//    @Schema(description = "输入的提示词变量与值map")
    private Map<String,Object> promptVarMap;

}