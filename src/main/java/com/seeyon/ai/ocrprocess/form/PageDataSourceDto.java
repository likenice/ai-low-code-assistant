package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

@Data
public class PageDataSourceDto {
    private String dataField;


    public static PageDataSourceDto convert(String dataField) {
        PageDataSourceDto pageDataSourceDto = new PageDataSourceDto();
        pageDataSourceDto.setDataField(dataField);
        return pageDataSourceDto;
    }
}
