package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

import java.util.List;

@Data
public class AssociationEntityDto {
    private String name;
    private String caption;
    private List<RelationDto> entityDtos;
}
