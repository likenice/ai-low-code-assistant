package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RootEnumDtos {
    private String id;
    private String fullName;
    private String name;
    private String code;
    private String appId;
    private String appName;
    private List<Map<String,Object>> children;
}
