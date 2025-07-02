package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 节点位置信息类
 */
@Data
@Setter @Getter
public class MultiLevelTableCell {
    private int rowIndex;
    private int flexRowSize;
    private int colIndex;
    private int flexColSize;
    private ObjectNode objectNode;
} 