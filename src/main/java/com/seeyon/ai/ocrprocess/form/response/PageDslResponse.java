package com.seeyon.ai.ocrprocess.form.response;

import com.seeyon.ai.ocrprocess.form.PageGroupDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageDslResponse {
    private String titleName;
    private List<PageGroupDto> groups = new ArrayList<>();

}
