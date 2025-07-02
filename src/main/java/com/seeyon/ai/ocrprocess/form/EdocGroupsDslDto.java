package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonPropertyOrder({"name", "type", "settings","id","components"})
public class EdocGroupsDslDto {
    private String name;
    private String type;
    private EdocSettingDslDto settings;
    private Long id;
    private List<EdocComponentsDslDto> components=new ArrayList<>();
}
