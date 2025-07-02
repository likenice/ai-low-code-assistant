package com.seeyon.ai.schematransformer.model;

import java.util.Arrays;
import java.util.List;

/**
 * DSL转换常量
 */
public class DslTransformConstant {
    /**
     * 文档应用名称
     */
    public static final String DOC_APP_NAME = "edoc335172694483814428";

    public static final String DOC_FULL_NAME_PREFIX = "com.seeyon.edoc335172694483814428.domain.entity";

    /**
     * 预置字段映射
     */
    public static final String[] PRESET_FIELDS = {
        "title",
        "content",
        "createTime",
        "createUser",
        "modifyTime",
        "modifyUser"
    };
    
    /** UDC容器组件列表 */
    public static final List<String> UDC_CONTAINER_COMP = Arrays.asList(
        "form", "container", "dataGrid", "grid", "gridCell"
    );
    
    /** UDC前缀组件列表 */
    public static final List<String> UDC_PREFIX_COMP = Arrays.asList(
        "form", "container", "dataGrid", "grid", "gridCell", "reference"
    );
    
    private DslTransformConstant() {
        // 私有构造函数
    }
} 