package com.seeyon.ai.ocrprocess.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.seeyon.ai.ocrprocess.form.AiEnumDto;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = EnumDeserializer.class)
public interface AiBaseEnum {

    @JsonValue
    Integer getCode();
    String getName();

    public static <T extends AiBaseEnum> List<AiEnumDto> toList(Class<T> type) {
        T[] enums= type.getEnumConstants();
        List<AiEnumDto> aiEnumDtoList = new ArrayList<>();
        for (T t : enums) {
            aiEnumDtoList.add(new AiEnumDto(t.getCode(), t.getName()));
        }
        return aiEnumDtoList;
    }

    public static <T extends AiBaseEnum> T findByCode(Integer code,Class<T> type) {
        T[] enums= type.getEnumConstants();
        for (T t : enums) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        return null;
    }
}
