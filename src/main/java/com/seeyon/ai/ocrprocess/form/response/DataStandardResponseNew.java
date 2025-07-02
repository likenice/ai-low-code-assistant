package com.seeyon.ai.ocrprocess.form.response;


import lombok.Data;

import java.util.List;

@Data
public class DataStandardResponseNew {
    private PageDslResponse pageDsl;
    private List<DataStandardResponse> dataStandards;

}
