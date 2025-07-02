package com.seeyon.ai.schematransformer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaTransformerTabs {

    public static JsonNode convertGroupTabsStructure(JsonNode sourceGroup, JsonNode templateNode) {
        if(templateNode == null){
            return convertGroupDataGridStructureInDataGrid(sourceGroup, templateNode);
        } else {
            String type= "";
            if (templateNode.has("type")) {
                type = templateNode.get("type").asText();
                if("collapse".equalsIgnoreCase(type)){

                    return convertGroupDataGridStructureInCollapse(sourceGroup, templateNode);
                }else if("dataGrid".equalsIgnoreCase(type)){
                    return convertGroupDataGridStructureInDataGrid(sourceGroup, templateNode);
                }else{
                    throw new RuntimeException("type must be collapse or dataGrid");
                }
            }else{
                throw new RuntimeException("templateNode must have type");
            }


        }

    }

    /**
     * 将sourceGroup结构套用templateNode模版.
     * templateNode是一个dataGrid组件.
     *
     * @param sourceGroup
     * @param templateNode
     * @return
     */
    public static JsonNode convertGroupDataGridStructureInDataGrid(JsonNode sourceGroup, JsonNode templateNode) {

        if(templateNode == null){
            templateNode = new ObjectMapper().createObjectNode();
            ((ObjectNode)templateNode).put("id", SchemaTransformerUtil.generateShortUUID());
        }

        // 如果templateNode是 分组 ,还是dataGrid

        ObjectMapper objectMapper = new ObjectMapper();
        // 创建新的根节点，复制模板节点的基本结构
        ObjectNode result = objectMapper.createObjectNode();
        
        // 复制type和sourceSchema
        String type= "dataGrid";
        if (templateNode.has("type")) {
            type = templateNode.get("type").asText();
        }
        result.put("type", type);
        Map<String,String> collectTypeAndSourceMap = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(templateNode);



        if (templateNode.has("sourceSchema")) {
            result.set("sourceSchema", templateNode.get("sourceSchema").deepCopy());
        }

        if (templateNode.has("id")) {
            result.put("id", templateNode.get("id").asText());
        }
        
        // 合并settings
        ObjectNode settings = objectMapper.createObjectNode();
        if (sourceGroup.has("settings")) {
            settings.setAll((ObjectNode) sourceGroup.get("settings"));
        }
        result.set("settings", settings);
        
        // 使用sourceGroup的dataSource
        if (sourceGroup.has("dataSource")) {
            result.set("dataSource", sourceGroup.get("dataSource").deepCopy());
        }
        
        // 处理components到children的转换
        ArrayNode children = objectMapper.createArrayNode();
        if (sourceGroup.has("components")) {
            JsonNode components = sourceGroup.get("components");
            JsonNode templateChildren = templateNode != null ? templateNode.get("children") : null;
            
            // 获取模板中第一个input组件的sourceSchema
            String firstSourceSchemaId = "";
            if (templateChildren != null && templateChildren.isArray()) {
//                for (JsonNode child : templateChildren) {
                JsonNode child = templateChildren.get(0);
                if (child != null){
                    ObjectNode firstSourceSchema = (ObjectNode) child.get("sourceSchema");
                    if(firstSourceSchema != null)
                        firstSourceSchemaId = firstSourceSchema.get("id").asText();
                }
//                JsonNode componentType = child.get("type");
//
//                String componentSourceSchemaId = collectTypeAndSourceMap.get(componentType);
//                if(componentSourceSchemaId != null){
//                    firstSourceSchema.put("id", componentSourceSchemaId);
//                }
//                }
            }
            
            // 将components转换为children
            for (JsonNode component : components) {
                ObjectNode child = (ObjectNode) component.deepCopy();
                JsonNode componentType = child.get("type");
                String componentSourceSchemaId = collectTypeAndSourceMap.get(componentType.asText());
                ObjectNode sourceSchemaNode = objectMapper.createObjectNode();

                if (componentSourceSchemaId == null) {
                    componentSourceSchemaId = firstSourceSchemaId;
                }
                sourceSchemaNode.put("id", componentSourceSchemaId);
                child.set("sourceSchema",sourceSchemaNode);
                children.add(child);
            }
        }
        result.set("children", children);

        
        return result;
    }

    /**
     * templateNode是一个分组组件, 其中包含的dataGrid.
     *
     * @param sourceGroup
     * @param collapseNode
     * @return
     */
    public static JsonNode convertGroupDataGridStructureInCollapse(JsonNode sourceGroup, JsonNode collapseNode) {

        JsonNode collapseNodeNew = collapseNode.deepCopy();
        // 重置所有节点id
        SchemaTransformerUtil.reChangeNodeId((ObjectNode) collapseNodeNew);
        List<NodePosition> dataGridNodePositionList = SchemaTransformerUtil.getAllTypeNodeInfo(collapseNodeNew, "dataGrid");
        NodePosition nodePosition = dataGridNodePositionList.get(0);
        JsonNode templateNode = nodePosition.getObjectNode();


        ObjectMapper objectMapper = new ObjectMapper();
        // 创建新的根节点，复制模板节点的基本结构
        ObjectNode result = objectMapper.createObjectNode();

        // 复制type和sourceSchema
        String type= "";
        if (templateNode.has("type")) {
            type = templateNode.get("type").asText();
        }
        result.put("type", type);
        Map<String,String> collectTypeAndSourceMap = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(templateNode);



        if (templateNode.has("sourceSchema")) {
            result.set("sourceSchema", templateNode.get("sourceSchema").deepCopy());
        }

        if (templateNode.has("id")) {
            result.put("id", templateNode.get("id").asText());
        }

        // 合并settings
        String dataGridTitle = "";
        ObjectNode settings = objectMapper.createObjectNode();
        if (sourceGroup.has("settings")) {
            JsonNode settingsNode = sourceGroup.get("settings");
            settings.setAll((ObjectNode) settingsNode);
            if (settingsNode.has("title")) {
                dataGridTitle = settingsNode.get("title").asText();
                ((ObjectNode)collapseNodeNew.get("settings")).put("title", dataGridTitle);
            }
        }
        result.set("settings", settings);

        // 使用sourceGroup的dataSource
        if (sourceGroup.has("dataSource")) {
            result.set("dataSource", sourceGroup.get("dataSource").deepCopy());
        }

        // 处理components到children的转换
        ArrayNode children = objectMapper.createArrayNode();
        if (sourceGroup.has("components")) {
            JsonNode components = sourceGroup.get("components");
            JsonNode templateChildren = templateNode != null ? templateNode.get("children") : null;

            // 获取模板中第一个input组件的sourceSchema
            String firstSourceSchemaId = "";
            if (templateChildren != null && templateChildren.isArray()) {

                JsonNode child = templateChildren.get(0);
                if (child != null){
                    ObjectNode firstSourceSchema = (ObjectNode) child.get("sourceSchema");
                    if(firstSourceSchema != null)
                        firstSourceSchemaId = firstSourceSchema.get("id").asText();
                }

            }

            // 将components转换为children
            for (JsonNode component : components) {
                ObjectNode child = (ObjectNode) component.deepCopy();
                JsonNode componentType = child.get("type");
                String componentSourceSchemaId = collectTypeAndSourceMap.get(componentType.asText());
                ObjectNode sourceSchemaNode = objectMapper.createObjectNode();

                if (componentSourceSchemaId == null) {
                    componentSourceSchemaId = firstSourceSchemaId;
                }
                sourceSchemaNode.put("id", componentSourceSchemaId);
                child.set("sourceSchema",sourceSchemaNode);
                children.add(child);
            }
        }

//        ObjectMapper tempMapper = new ObjectMapper();
//        ArrayNode tempArrayNode = tempMapper.createArrayNode();
        result.set("children", children);
        ObjectMapper tempMapper2 = new ObjectMapper();
        ArrayNode tempArrayNode2 = tempMapper2.createArrayNode();
        tempArrayNode2.add(result);
        SchemaTransformerUtil.deleteNodeById((ObjectNode) collapseNodeNew,nodePosition.getId());
        JsonNode jsonNode = SchemaTransformerUtil.insertLayoutListAtCollapsePosition(collapseNodeNew, nodePosition, tempArrayNode2);
//        tempArrayNode.add(jsonNode);
//        result.set("children", tempArrayNode);


        return jsonNode;
    }
}
