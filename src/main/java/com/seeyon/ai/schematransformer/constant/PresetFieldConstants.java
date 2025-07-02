package com.seeyon.ai.schematransformer.constant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PresetFieldConstants {
    public static final Set<String> PRESET_NAME_SET = new HashSet<>();
    public static final Map<String, String> PRESET_FIELD_MAP = new HashMap<>();

    static {
        // 初始化预置字段映射
        PRESET_FIELD_MAP.put("id", "ID");
        PRESET_FIELD_MAP.put("name", "名称");
        PRESET_FIELD_MAP.put("createTime", "创建时间");
        PRESET_FIELD_MAP.put("updateTime", "更新时间");
        PRESET_FIELD_MAP.put("creator", "创建人");
        PRESET_FIELD_MAP.put("updator", "更新人");

        // 初始化预置名称集合
        PRESET_NAME_SET.addAll(PRESET_FIELD_MAP.values());
    }
} 