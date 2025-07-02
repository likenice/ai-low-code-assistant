package com.seeyon.ai.ocrprocess.enums;




import com.seeyon.ai.ocrprocess.form.AiEnumDto;

import java.util.List;

public enum AiIdentifyTypeEnum implements AiBaseEnum {
    Img(0,"img"),

    Excel(1,"excel");

    AiIdentifyTypeEnum(int code, String name) {
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

    public static AiIdentifyTypeEnum findByCode(Integer code) {
        return AiBaseEnum.findByCode(code, AiIdentifyTypeEnum.class);
    }
    public static List<AiEnumDto> toList() {
        return AiBaseEnum.toList(AiIdentifyTypeEnum.class);
    }
}
