package com.seeyon.ai.ocrprocess.form.response;


import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class IdentifyResponse {
    // csv文件路径
    private String ocrJson;
    // 相近实体信息
    private Map<String,List<String>> similarEntityInfo;
}
