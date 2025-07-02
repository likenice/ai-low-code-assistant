package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.dto.MultiLevelTableCell;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SchemaTransformerDataGrid {

    public static JsonNode convertGroupDataGridStructure(JsonNode sourceGroup, JsonNode templateNode) {
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
     * 将sourceGroup结构套用templateNode模版.(支持多级表头)
     * templateNode是一个dataGrid组件.
     *
     * @param sourceGroup
     * @param templateNode
     * @return
     */
    public static JsonNode convertGroupDataGridStructureInDataGridNew(JsonNode sourceGroup, JsonNode templateNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode result = objectMapper.createObjectNode();

        // 1. 复制基本属性
        result.put("name", sourceGroup.has("name") ? sourceGroup.get("name").asText() : "");
        result.put("type", sourceGroup.has("type") ? sourceGroup.get("type").asText() : "");

        // 2. 复制dataSource
        if (sourceGroup.has("dataSource")) {
            result.set("dataSource", sourceGroup.get("dataSource").deepCopy());
        }

        // 3. 设置sourceSchema
        if (templateNode != null && templateNode.has("sourceSchema")) {
            result.set("sourceSchema", templateNode.get("sourceSchema").deepCopy());
        }

        // 4. 处理settings
        ObjectNode settings = objectMapper.createObjectNode();
//        ObjectNode settingsNode = null;
        if (sourceGroup.has("settings")) {

            settings.setAll((ObjectNode) sourceGroup.get("settings"));
        }


        // 如果有多级表头,添加enableColumnGroups设置
        if (hasMultiLevelHeaders(sourceGroup)) {
            settings.put("enableColumnGroups", true);
        }
        result.set("settings", settings);
        
        // 5. 处理components到children的转换
        ArrayNode children = objectMapper.createArrayNode();
        if (sourceGroup.has("components")) {
            
            Map<String, String> typeToSourceSchemaMap = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(templateNode);

            // 获取模版中第一个组件的type作为默认type
            String defaultType = "";
            JsonNode childrenNodes = templateNode.get("children");
            if (childrenNodes.size() > 0) {
                defaultType = childrenNodes.get(0).get("type").asText();
            }
            
            // 构建单元格位置映射
            Map<String, MultiLevelTableCell> cellMap = buildCellMap(sourceGroup);

            int initColNum = 1;
            int initRowNum = 1;

            JsonNode componentNodes = sourceGroup.get("components");
            for (JsonNode componentNode : componentNodes) {
                // 递归构建层级结构，传入defaultType
                JsonNode topLevelNodes = buildHierarchy(cellMap, typeToSourceSchemaMap, initRowNum, initColNum, defaultType,sourceGroup);

                int anInt = componentNode.get("cellColRow").get("flexColSize").asInt();
                initColNum = initColNum +anInt;
                if(topLevelNodes != null) {
                    children.add(topLevelNodes);
                }
            }

//
        }
        
        result.set("children", children);
        return result;
    }

    private static boolean hasMultiLevelHeaders(JsonNode sourceGroup) {
        if (!sourceGroup.has("components")) {
            return false;
        }
        
        JsonNode components = sourceGroup.get("components");
        Set<Integer> rowIndices = new HashSet<>();
        for (JsonNode component : components) {
            if (component.has("cellColRow")) {
                rowIndices.add(component.get("cellColRow").get("rowIndex").asInt());
            }
        }
        return rowIndices.size() > 1;
    }


    private static Map<String, MultiLevelTableCell> buildCellMap(JsonNode ocrNode) {

        AtomicInteger rowNum = new AtomicInteger();
        AtomicInteger colNum = new AtomicInteger();
        ocrNode.get("settings").get("gridTemplateRows").forEach(row -> rowNum.getAndIncrement());
        ocrNode.get("settings").get("gridTemplateColumns").forEach(row -> colNum.getAndIncrement());
        ArrayNode components = (ArrayNode) ocrNode.get("components");
        Map<String, MultiLevelTableCell> cellMap = new HashMap<>();
        for (JsonNode component : components) {
            MultiLevelTableCell multiLevelTableCell = new MultiLevelTableCell();
            JsonNode cellColRow = component.get("cellColRow");

            int rowIndex = cellColRow.get("rowIndex").asInt();
            int colIndex = cellColRow.get("colIndex").asInt();
            multiLevelTableCell.setRowIndex(cellColRow.get("rowIndex").asInt());
            multiLevelTableCell.setColIndex(cellColRow.get("colIndex").asInt());
            multiLevelTableCell.setFlexRowSize(cellColRow.get("flexRowSize").asInt());
            multiLevelTableCell.setFlexColSize(cellColRow.get("flexColSize").asInt());
            multiLevelTableCell.setObjectNode((ObjectNode) component);
            String key = rowIndex + "," + colIndex;
            cellMap.put(key, multiLevelTableCell);
        }
        return cellMap;
    }

    private static JsonNode buildHierarchy(Map<String, MultiLevelTableCell> cellMap,
                                               Map<String, String> typeToSourceSchemaMap,
                                               int currentRow,
                                               int currentCol,
                                               String defaultType,JsonNode parentOcrNode) {

        // 处理
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode leafNode = objectMapper.createObjectNode();
        String currentKey = currentRow + "," + currentCol;
        
        if (!cellMap.containsKey(currentKey)) {
            return null;
        }
        


        MultiLevelTableCell multiLevelTableCell = cellMap.get(currentKey);
        int flexColSize1 = multiLevelTableCell.getFlexColSize();
        int flexRowSize1 = multiLevelTableCell.getFlexRowSize();
        int currentRowChild = currentRow + flexRowSize1;
//        ArrayNode childNodes = objectMapper.createArrayNode();

        ObjectNode objectNode = multiLevelTableCell.getObjectNode();

//        JsonNode componentsJsonNodes = parentOcrNode.get("components");
        if(objectNode != null ){
//            for (JsonNode componentsJsonNode : componentsJsonNodes) {
                String type = objectNode.get("type") == null ? "":objectNode.get("type").asText();
                leafNode.put("name", "");
                leafNode.put("type", type);  // 保持原始type

                // 设置dataSource
                if (objectNode.has("dataSource")) {
                    leafNode.set("dataSource", objectNode.get("dataSource").deepCopy());
                }

                // 设置sourceSchema
                ObjectNode sourceSchema = objectMapper.createObjectNode();
                String sourceSchemaId = typeToSourceSchemaMap.getOrDefault(type,
                        typeToSourceSchemaMap.get(defaultType));
                sourceSchema.put("id", sourceSchemaId);
                leafNode.set("sourceSchema", sourceSchema);

                // 设置settings
                ObjectNode settings = objectMapper.createObjectNode();
                if (objectNode.has("settings")) {
                    JsonNode cellSettings = objectNode.get("settings");
                    if (cellSettings.has("titleName")) {
                        settings.put("content", cellSettings.get("titleName").asText());
                    }
                    if (cellSettings.has("align")) {
                        settings.put("align", cellSettings.get("align").asText());
                    }
                }
                leafNode.set("settings", settings);


                ArrayNode childNodes = objectMapper.createArrayNode();
                for(int i=0 ; i<flexColSize1 ; i++){
                    int currentColChild = currentCol+i;
                    JsonNode childJsonNode = buildHierarchy(cellMap, typeToSourceSchemaMap, currentRowChild, currentColChild, defaultType,objectNode);
                    if(childJsonNode != null){
                        childNodes.add(childJsonNode);
                    }
                }
                if(childNodes !=null && childNodes.size() > 0) {
                    leafNode.put("children", childNodes);
                }
//            }
        }

        
        return leafNode;
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
//        if (sourceGroup.has("settings")) {
//            settings.setAll((ObjectNode) sourceGroup.get("settings"));
//        }
//

        if (sourceGroup.has("settings")) {
            JsonNode settingsNode = sourceGroup.get("settings");
            if(settingsNode.get("title")!= null){
                settings.set("title",settingsNode.get("title"));
            }else if(settingsNode.get("titleName")!= null){
                settings.set("title",settingsNode.get("titleName"));
            }
            if(settingsNode.get("belongTabs")!= null){
                settings.set("belongTabs",settingsNode.get("belongTabs"));
            }
            if(settingsNode.get("gridTemplateColumns")!= null){
                settings.set("gridTemplateColumns",settingsNode.get("gridTemplateColumns"));
            }
            if(settingsNode.get("gridTemplateRows")!= null){
                settings.set("gridTemplateRows",settingsNode.get("gridTemplateRows"));
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
            if(settingsNode.get("title")!= null){
                settings.set("title",settingsNode.get("title"));
            }else if(settingsNode.get("titleName")!= null){
                settings.set("title",settingsNode.get("titleName"));
            }
            if(settingsNode.get("belongTabs")!= null){
                settings.set("belongTabs",settingsNode.get("belongTabs"));
            }
            if(settingsNode.get("gridTemplateColumns")!= null){
                settings.set("gridTemplateColumns",settingsNode.get("gridTemplateColumns"));
            }
            if(settingsNode.get("gridTemplateRows")!= null){
                settings.set("gridTemplateRows",settingsNode.get("gridTemplateRows"));
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
