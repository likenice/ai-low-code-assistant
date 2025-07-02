package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@Data
@JsonPropertyOrder({"id", "settings", "type", "level", "information", "dataSource", "cellColRow", "referGroup", "groups"})
public class EdocComponentsDslDto {
    private Long id;
    private EdocSettingDslDto settings;
    private String type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private int level;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String information;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private EdocDataSourceDto dataSource;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PageCellColRow cellColRow;
    private EdocReferGroupDto referGroup;
    private List<EdocGroupsDslDto> groups;
    @JsonIgnore
    private int y;
}
