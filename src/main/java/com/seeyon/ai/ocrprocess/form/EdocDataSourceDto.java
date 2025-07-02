package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"dataField", "fullName", "caption", "relationApp", "RelationEntity", "dataType"})
public class EdocDataSourceDto {
    private String dataField;
    private String fullName;
    private String caption;
    private String relationApp;
    private String RelationEntity;
    private String dataType;
    @JsonIgnore
    private String componentType;
    @JsonIgnore
    private String type;
    @JsonIgnore
    private Integer level;
    @JsonIgnore
    private String information;


    public static EdocDataSourceDto convertToEdocEntity(EdocEntityDto edocEntityDto, String componentType, int level, String information) {
        EdocDataSourceDto edocDataSourceDto = new EdocDataSourceDto();
        edocDataSourceDto.setDataField(edocEntityDto.getFullName());
        edocDataSourceDto.setFullName(edocEntityDto.getFullName());
        edocDataSourceDto.setCaption(edocEntityDto.getCaption());
        edocDataSourceDto.setRelationApp(edocEntityDto.getRelationApp());
        edocDataSourceDto.setRelationEntity(edocEntityDto.getRelationEntity());
        edocDataSourceDto.setDataType(edocEntityDto.getDataType());
        edocDataSourceDto.setComponentType(componentType);
        edocDataSourceDto.setLevel(level);
        edocDataSourceDto.setInformation(information);
        return edocDataSourceDto;
    }
}
