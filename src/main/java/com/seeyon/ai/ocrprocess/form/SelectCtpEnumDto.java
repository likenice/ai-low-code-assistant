package com.seeyon.ai.ocrprocess.form;


import lombok.Data;

import java.util.List;

@Data
public class SelectCtpEnumDto {
    private List<RootEnumDtos> rootEnumDtos;
    private ApplicationDto applicationInfo;
    private Object categoryAndItemDtos;
}
