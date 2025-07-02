package com.seeyon.ai.ocrprocess.enums;




import com.seeyon.ai.ocrprocess.form.AiEnumDto;

import java.util.List;

public enum AiFromPlatformEnum implements AiBaseEnum {

    AI_PLATFORM(0,"ai平台"),

    V5_PLATFORM(1,"v5平台"),

    V8_PLATFORM(2,"v8平台");

    AiFromPlatformEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    private Integer code;

    @Override
    public Integer getCode() {
        return code;
    }
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public static AiFromPlatformEnum findByCode(Integer code) {
        return AiBaseEnum.findByCode(code, AiFromPlatformEnum.class);
    }
    public static List<AiEnumDto> toList() {
        return AiBaseEnum.toList(AiFromPlatformEnum.class);
    }
}
