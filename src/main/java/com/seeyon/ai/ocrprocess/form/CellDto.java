package com.seeyon.ai.ocrprocess.form;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CellDto {
    private Long id;
    private List<Double> location;
    private List<Map<String,Object>> contents;
    private int row;
}
