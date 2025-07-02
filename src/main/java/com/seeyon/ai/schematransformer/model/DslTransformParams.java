package com.seeyon.ai.schematransformer.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DSL转换参数对象
 * 
 * @author AI Assistant
 */
@Data
@Setter
@Getter
public class DslTransformParams {
    /**
     * 原始DSL
     */
    JsonNode dsl;
    
//    /**
//     * 实体ID
//     */
//    private String entityId;
    
    /**
     * 应用信息
     */
    private JsonNode appInfo;
    
    /**
     * 模板Schema
     */
    private String tempSchema;
    
    /**
     * 表格数据
     */
    private ArrayNode tableData;
    
    /**
     * 是否为文档
     */
    private boolean isDoc;
    
    /**
     * 实体列表
     */
    private ArrayNode entityList;

    /**
     * 表格数据
     */
    private Map<String, String> referceFullNameMap;
} 