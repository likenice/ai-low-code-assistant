package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"rowIndex", "flexRowSize", "colIndex", "flexColSize"})
public class PageCellColRow {
    private Integer rowIndex;
    private Integer flexRowSize;
    private Integer colIndex;
    private Integer flexColSize;
}
