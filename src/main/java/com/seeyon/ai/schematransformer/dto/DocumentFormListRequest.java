package com.seeyon.ai.schematransformer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class DocumentFormListRequest {
    private List<String> filterPlanGuids;
    private SearchParams searchParams;
    private PageInfo pageInfo;

    @Setter
    @Getter
    public static class SearchParams {
        private Map<String, Object> searchParam;
        private String logicalOperator = "AND";
        private List<Object> sortSettings;
        private Map<String, Object> expressionValues;
        private List<Object> one2OneEntityRelationsV2;
    }

    @Setter
    @Getter
    public static class PageInfo {
        private int pageNumber = 1;
        private int pageSize = 1000;
        private int total = 0;
        private boolean needTotal = true;
    }
}
