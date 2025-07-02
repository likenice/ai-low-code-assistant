package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

@Data
public class RelationDto {
    private String relationEntity = "";
    private String relationEntityName = "";
    private String relationCode = "";
    private String relationApp = "";
    private String relationAppName = "";
    private String relationType = "Many2OneAssociation";
    private String relationEntityCategory ="";
    private String relationStarter ="";
}
