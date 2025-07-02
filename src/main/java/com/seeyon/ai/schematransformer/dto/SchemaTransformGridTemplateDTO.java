package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class SchemaTransformGridTemplateDTO {



    // 网格title Cell节点样式
    private JsonNode gridCellTitleNode; // 网格标题 节点默认样式
    private JsonNode gridCellComponentNode;  // 网格component 节点样式
    private JsonNode gridCellDescNode;    // 网格desc Cell节点样式


    // 网格title 节点样式
    private JsonNode titleNode;
    // 网格desc 节点样式
    private JsonNode descNode;

    // dataField 对应 component模版节点样式
    private java.util.Map<String,JsonNode> dataFieldTemplateMap = new java.util.HashMap<>();
    // type 对应 component模版节点样式
    private java.util.Map<String,JsonNode> typeTemplateMap = new java.util.HashMap<>();


    // 网格component 节点默认样式
    private JsonNode componentDefaultNode;

    // 容器默认样式
    private JsonNode containerDefaultNode;


    // 容器默认样式
    private ObjectNode labelDefaultNode;


    private NodePosition gridTemplatePosition = new NodePosition();

    public JsonNode getGridCellTitleNode() {
        JsonNode gridCellNode = typeTemplateMap.get("gridCell");
        if(gridCellNode != null && gridCellTitleNode == null){
            return gridCellNode;
        }
        return gridCellTitleNode;
    }

    public void setGridCellTitleNode(JsonNode gridCellTitleNode) {
        this.gridCellTitleNode = gridCellTitleNode;
    }

    public JsonNode getGridCellComponentNode() {
        JsonNode gridCellNode = typeTemplateMap.get("gridCell");
        if(gridCellNode != null && gridCellComponentNode == null){
            return gridCellNode;
        }
        return gridCellComponentNode;
    }

    public void setGridCellComponentNode(JsonNode gridCellComponentNode) {
        this.gridCellComponentNode = gridCellComponentNode;
    }

    public JsonNode getGridCellDescNode() {
        JsonNode gridCellNode = typeTemplateMap.get("gridCell");
        if(gridCellNode != null && gridCellDescNode == null){
            return gridCellNode;
        }
        return gridCellDescNode;
    }

    public void setGridCellDescNode(JsonNode gridCellDescNode) {
        this.gridCellDescNode = gridCellDescNode;
    }

    public JsonNode getLabelDefaultNode() {
        if(labelDefaultNode == null){
            labelDefaultNode = JsonNodeFactory.instance.objectNode();
            labelDefaultNode.put("type","label")   ;
            labelDefaultNode.putObject("settings");
        }

        return labelDefaultNode;
    }
}