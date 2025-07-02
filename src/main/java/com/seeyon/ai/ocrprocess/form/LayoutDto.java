package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LayoutDto {
    private Map<String,Object> entity;
    private List<Map<String,Object>> group;
    private List<Map<String,Object>> sublist;
}
