package com.seeyon.ai.ocrprocess.form.response;


import com.seeyon.ai.ocrprocess.form.EntityDto;
import com.seeyon.ai.ocrprocess.form.FieldDto;
import lombok.Data;

import java.util.List;

@Data
public class DataStandardResponse {

    private EntityDto createEntity;
    private List<FieldDto> operateEntity;



}
