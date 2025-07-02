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
public class NodePosition {
    private boolean found;
    private String parentId;
    private String id;
    private int index;
    private ObjectNode objectNode;
} 