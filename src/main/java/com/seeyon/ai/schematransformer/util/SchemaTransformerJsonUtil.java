package com.seeyon.ai.schematransformer.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaTransformerJsonUtil {
    
    /**
     * 递归遍历JsonNode，收集所有type和第一个对应的sourceSchema.id
     * @param node 输入的JsonNode
     * @return Map<String,String> key为type，value为sourceSchema.id
     */
    public static Map<String, String> collectTypeAndSourceSchemaId(JsonNode node) {
        Map<String, String> result = new HashMap<>();
        collectTypeAndSourceSchemaIdRecursive(node, result);
        return result;
    }

    private static void collectTypeAndSourceSchemaIdRecursive(JsonNode node, Map<String, String> result) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 处理当前节点
        if (node.has("type") && node.has("sourceSchema")) {
            String type = node.get("type").asText();
            String sourceSchemaId = node.get("sourceSchema").get("id").asText();
            
            // 只保存每个type的第一个sourceSchema.id
            if (!result.containsKey(type)) {
                result.put(type, sourceSchemaId);
            }
        }

        // 递归处理children节点
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectTypeAndSourceSchemaIdRecursive(child, result);
            }
        }
    }




    /**
     * 判断layoutSchema中是否存在tabs, 并返回tabs与layoutSchema的关系
     * 处理逻辑:递归遍历layoutSchema->groups->children
     *
     * @param layoutSchema
     * 示例:
    {
    "titleName": "",
    "groups": [
    {
    "type": "tabs",
    "children": [
    {
    "type": "collapse"
    }
    ]
    }
    ]
    }
     * @return  none: 没有tabs,
     *          all: groups下只有一个tabs, 且tabs包含所有group,
     *          part: 存在tabs, 且tabs未包含所有group
     */
    public static String getLayoutAndTabsRelation(JsonNode layoutSchema){
        // 如果layoutSchema为空或不包含groups，则返回none
        if (layoutSchema == null || !layoutSchema.has("groups") || !layoutSchema.get("groups").isArray()
                || layoutSchema.get("groups").size() == 0) {
            return "none";
        }

        ArrayNode groups = (ArrayNode) layoutSchema.get("groups");
        int totalGroups = groups.size();

        // 检查是否只有一个tabs节点，且包含所有group
        if (totalGroups == 1) {
            JsonNode singleGroup = groups.get(0);
            if (singleGroup.has("type") && "tabs".equals(singleGroup.get("type").asText())) {
                return "all";
            }
        }

        // 检查是否存在tabs节点
        for (JsonNode group : groups) {
            if (group.has("type") && "tabs".equals(group.get("type").asText())) {

                return "part";
            }
        }

        return "none";
    }

    
    /**
     * 根据OCR schema和DSL模板schema生成tabs数组节点
     * @param ocrSchema OCR识别结果schema
     * @param dslTemplateSchema DSL模板schema
     * @return 包含tabs节点的List<JsonNode>
     */
    public static List<JsonNode> getTabsArrayNodeByTemplateAndOcr(JsonNode ocrSchema, JsonNode dslTemplateSchema) {
        if (ocrSchema == null || dslTemplateSchema == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        List<JsonNode> resultList = new ArrayList<>();

        // 从OCR schema中获取tabs设置信息
        if (ocrSchema.has("groups") && ocrSchema.get("groups").isArray()) {
            ArrayNode groups = (ArrayNode) ocrSchema.get("groups");
            for (JsonNode group : groups) {
                if (group.has("type") && "tabs".equals(group.get("type").asText())) {
                    // 从模板中获取tabs的sourceSchema信息
                    List<JsonNode> templateTabsNodes = getTabsNode(dslTemplateSchema);
                    if (!templateTabsNodes.isEmpty()) {
                        // 创建新的tabs节点
                        ObjectNode tabsNode = objectMapper.createObjectNode();
                        tabsNode.put("type", "tabs");
                        
                        // 设置sourceSchema
                        ObjectNode sourceSchema = objectMapper.createObjectNode();
                        sourceSchema.put("id", templateTabsNodes.get(0).get("sourceSchema").get("id").asText());
                        tabsNode.set("sourceSchema", sourceSchema);
                        
                        // 复制settings
                        if (group.has("settings")) {
                            tabsNode.set("settings", group.get("settings").deepCopy());
                        }
                        
                        // 添加空的children数组
                        tabsNode.set("children", objectMapper.createArrayNode());
                        
                        resultList.add(tabsNode);
                    }
                }
            }
        }

        return resultList;
    }

    /**
     * 删除tabs节点, 并保存tabs下面children节点
     * 示例:
     *  {
     "titleName": "",
     "groups": [
     {
     "id": "1",
     "type": "tabs",
     "settings": {
        "tabsSetting": [{
            "name": "页签1"
         },{
            "name": "页签2"
         }]
     },
     "components": [
     {
     "id": "2",
     "type": "collapse"
     }
     ]
     }
     ]
     }
     调整为:
     {
     "titleName": "",
     "groups": [
     {
     "id": "2",
     "settings": {
        "belongTabs":"页签1/页签2"
     }
     "type": "collapse"
     }
     ]
     }
     *
     *
     * @param layoutSchema
     * @return
     */
    public static ObjectNode removeTabsNodeInOcrSchema(JsonNode layoutSchema) {
        if (layoutSchema == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        ObjectNode result = layoutSchema.deepCopy();

        if (!layoutSchema.has("groups") || !layoutSchema.get("groups").isArray()) {
            return result;
        }

        ArrayNode newGroups = objectMapper.createArrayNode();
        JsonNode groups = layoutSchema.get("groups");

        for (JsonNode group : groups) {
            if (group.has("type") && "tabs".equals(group.get("type").asText()) && group.has("components")) {
                // 获取 tabsSetting 信息
                StringBuilder tabNames = new StringBuilder();
                if (group.has("settings") && group.get("settings").has("tabsSetting")) {
                    JsonNode tabsSettings = group.get("settings").get("tabsSetting");
                    for (int i = 0; i < tabsSettings.size(); i++) {
                        if (i > 0) {
                            tabNames.append("/");
                        }
                        tabNames.append(tabsSettings.get(i).get("name").asText());
                    }
                }

                // 处理 components 节点
                JsonNode children = group.get("components");
                for (JsonNode child : children) {
                    ObjectNode modifiedChild = child.deepCopy();
                    
                    // 添加 belongTabs 设置
                    ObjectNode settings;
                    if (modifiedChild.has("settings")) {
                        settings = (ObjectNode) modifiedChild.get("settings");
                    } else {
                        settings = objectMapper.createObjectNode();
                        modifiedChild.set("settings", settings);
                    }
                    settings.put("belongTabs", tabNames.toString());
                    
                    newGroups.add(modifiedChild);
                }
            } else {
                newGroups.add(group.deepCopy());
            }
        }

        ((ObjectNode)result).set("groups", newGroups);
        return result;
    }

    /**
     * 将sourceNodes中belongTabs属性匹配的节点添加到tabsNode的children中
     * 
     * @param sourceNodes 源节点数组，包含belongTabs属性的节点
     * @param tabsNodeList tabs模板节点列表
     */
    public static void addTabs(ArrayNode sourceNodes, List<JsonNode> tabsNodeList) {
        if (sourceNodes == null || tabsNodeList == null || tabsNodeList.isEmpty()) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // 创建一个临时列表存储所有匹配的节点
        Map<String, List<JsonNode>> matchingNodesMap = new HashMap<>();
        
        // 遍历所有tabs模板，初始化matchingNodesMap
        for (JsonNode tabsNode : tabsNodeList) {
            if (tabsNode.has("settings") && tabsNode.get("settings").has("tabsSetting")) {
                String targetTabNames = getTabNames(tabsNode.get("settings").get("tabsSetting"));
                if (!targetTabNames.isEmpty()) {
                    matchingNodesMap.put(targetTabNames, new ArrayList<>());
                }
            }
        }

        // 收集所有匹配的节点并从原数组中移除
        Iterator<JsonNode> iterator = sourceNodes.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (node.has("settings") && node.get("settings").has("belongTabs")) {
                String belongTabs = node.get("settings").get("belongTabs").asText();
                if (matchingNodesMap.containsKey(belongTabs)) {
                    matchingNodesMap.get(belongTabs).add(node.deepCopy());
                    iterator.remove();
                }
            }
        }

        // 将匹配的节点添加到对应的tabs节点中，并将tabs节点添加到sourceNodes
        for (JsonNode tabsNode : tabsNodeList) {
            if (tabsNode.has("settings") && tabsNode.get("settings").has("tabsSetting")) {
                String targetTabNames = getTabNames(tabsNode.get("settings").get("tabsSetting"));
                List<JsonNode> matchingNodes = matchingNodesMap.get(targetTabNames);
                
                if (matchingNodes != null && !matchingNodes.isEmpty()) {
                    ObjectNode newTabsNode = tabsNode.deepCopy();
                    ArrayNode children = objectMapper.createArrayNode();
                    matchingNodes.forEach(children::add);
                    ((ObjectNode) newTabsNode).set("children", children);
                    sourceNodes.add(newTabsNode);
                }
            }
        }
    }

    private static String getTabNames(JsonNode tabsSettings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tabsSettings.size(); i++) {
            if (i > 0) {
                sb.append("/");
            }
            sb.append(tabsSettings.get(i).get("name").asText());
        }
        return sb.toString();
    }

    /**
     * 将包含tabs的schema转换为不包含tabs的schema
     * @param node 输入的JsonNode
     * @return 转换后的JsonNode
     */
    public static JsonNode removeTabs(JsonNode node) {
        if (node == null) {
            return null;
        }

        // 处理数组节点
        if (node.isArray()) {
            ArrayNode newArray = JsonNodeFactory.instance.arrayNode();
            for (JsonNode element : node) {
                JsonNode processedElement = removeTabs(element);
                if (processedElement != null) {
                    newArray.add(processedElement);
                }
            }
            return newArray;
        }

        // 处理对象节点
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            
            // 如果是tabs节点，处理其children节点
            if ("tabs".equals(objNode.path("type").asText())) {
                // 获取tabsSetting中的name信息
                StringBuilder tabNames = new StringBuilder();
                if (objNode.has("settings") && objNode.get("settings").has("tabsSetting")) {
                    JsonNode tabsSettings = objNode.get("settings").get("tabsSetting");
                    for (int i = 0; i < tabsSettings.size(); i++) {
                        if (i > 0) {
                            tabNames.append("/");
                        }
                        tabNames.append(tabsSettings.get(i).get("name").asText());
                    }
                }

                if (objNode.has("children") && objNode.get("children").isArray()) {
                    ArrayNode children = (ArrayNode) objNode.get("children");
                    if (children.size() == 1) {
                        // 如果只有一个子节点，处理该节点并添加belongTabs
                        JsonNode processedChild = removeTabs(children.get(0));
                        if (processedChild != null && processedChild.isObject()) {
                            ObjectNode childObj = (ObjectNode) processedChild;
                            ObjectNode settings;
                            if (childObj.has("settings")) {
                                settings = (ObjectNode) childObj.get("settings");
                            } else {
                                settings = JsonNodeFactory.instance.objectNode();
                                childObj.set("settings", settings);
                            }
                            settings.put("belongTabs", tabNames.toString());
                            return childObj;
                        }
                    }
                    
                    // 如果有多个子节点，处理所有节点并添加belongTabs
                    ArrayNode newChildren = JsonNodeFactory.instance.arrayNode();
                    for (JsonNode child : children) {
                        JsonNode processedChild = removeTabs(child);
                        if (processedChild != null && processedChild.isObject()) {
                            ObjectNode childObj = (ObjectNode) processedChild;
                            ObjectNode settings;
                            if (childObj.has("settings")) {
                                settings = (ObjectNode) childObj.get("settings");
                            } else {
                                settings = JsonNodeFactory.instance.objectNode();
                                childObj.set("settings", settings);
                            }
                            settings.put("belongTabs", tabNames.toString());
                            newChildren.add(childObj);
                        }
                    }
                    return newChildren;
                }
                return null;
            }

            // 处理所有字段
            ObjectNode newObj = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode processedValue = removeTabs(entry.getValue());
                if (processedValue != null) {
                    newObj.set(entry.getKey(), processedValue);
                }
            }
            return newObj;
        }

        // 对于其他类型的节点（如文本、数字等），直接返回
        return node;
    }

    /**
     * 获取tabs节点
     * @param node 输入的JsonNode
     * @return node中的type为tabs的节点集合
     */
    public static List<JsonNode> getTabsNode(JsonNode node) {
        List<JsonNode> tabsNodes = new ArrayList<>();
        findTabsNodes(node, tabsNodes);
        return tabsNodes;
    }

    private static void findTabsNodes(JsonNode node, List<JsonNode> tabsNodes) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            // 检查当前节点是否为tabs类型
            if (node.has("type") && "tabs".equals(node.get("type").asText())) {
                tabsNodes.add(node);
            }

            // 递归处理所有字段
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                findTabsNodes(entry.getValue(), tabsNodes);
            }
        } else if (node.isArray()) {
            // 递归处理数组中的每个元素
            for (JsonNode element : node) {
                findTabsNodes(element, tabsNodes);
            }
        }
    }

    /**
     * 递归遍历所有节点, 只删除根节点和children数组下节点的id属性
     * 示例输入:
     * {
     *   "children": [{                         // children数组下的节点
     *     "children": [{                       // children数组下的节点
     *       "dataSource": { "id": "cfb3-3" },  // 保留
     *       "name": "",
     *       "id": "reference_1"                 // 删除
     *     }],
     *     "dataSource": { "id": "" },          // 保留
     *     "name": "",
     *     "id": "group_1"                      // 删除
     *   }],
     *   "dataSource": { "entityName": "表名称" },
     *   "name": "重复表名称",
     *   "id": "dataGrid_1"                     // 删除（根节点）
     * }
     * 
     * @param node 输入的JsonNode，节点间通过children属性建立父子关系
     */
    public static void removeAllNodeId(JsonNode node) {
        if (node == null) {
            return;
        }

        // 处理对象节点
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            // 删除当前节点的id属性
            objNode.remove("id");
            
            // 递归处理所有字段
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                // 只处理children字段
                if ("children".equals(entry.getKey()) && entry.getValue().isArray()) {
                    ArrayNode children = (ArrayNode) entry.getValue();
                    for (JsonNode child : children) {
                        removeAllNodeId(child);
                    }
                }
            }
        }
        // 处理数组节点（可能是children数组）
        else if (node.isArray()) {
            for (JsonNode element : node) {
                removeAllNodeId(element);
            }
        }
    }

    /**
     * 修复groupNode 中缺失的单元格.
     * @param groupNode
     */
    public static void fixComponents(ObjectNode groupNode) {
        if (groupNode == null || !groupNode.has("type") ) {

            return;
        }

        boolean isGrid = "grid".equals(groupNode.get("type").asText()) || "collapse".equals(groupNode.get("type").asText());
        if(!isGrid){
            return;
        }

        // 获取网格设置
        JsonNode settings = groupNode.get("settings");
        if (settings == null || !settings.has("gridTemplateColumns") || !settings.has("gridTemplateRows")) {
            return;
        }

        ArrayNode columns = (ArrayNode) settings.get("gridTemplateColumns");
        ArrayNode rows = (ArrayNode) settings.get("gridTemplateRows");
        int rowCount = rows.size();
        int colCount = columns.size();

        // 创建一个二维数组来跟踪已占用的单元格
        boolean[][] occupiedCells = new boolean[rowCount + 1][colCount + 1];

        // 标记已有组件占用的单元格
        ArrayNode components = (ArrayNode) groupNode.get("components");
        if (components != null) {
            for (JsonNode component : components) {
                JsonNode cellColRow = component.get("cellColRow");
                if (cellColRow != null) {
                    int rowIndex = cellColRow.get("rowIndex").asInt();
                    int colIndex = cellColRow.get("colIndex").asInt();
                    int flexColSize = cellColRow.get("flexColSize").asInt();
                    int flexRowSize = cellColRow.get("flexRowSize").asInt();
                    
                    // 标记所有被合并单元格占用的位置
                    for (int r = 0; r < flexRowSize; r++) {
                        for (int c = 0; c < flexColSize; c++) {
                            if (rowIndex + r <= rowCount && colIndex + c <= colCount) {
                                occupiedCells[rowIndex + r][colIndex + c] = true;
                            }
                        }
                    }
                }
            }
        }

        List<Integer> deleteRows = new ArrayList<>();
        List<Integer> deleteColumns = new ArrayList<>();
        
        //如果整行都为空,则删除该行
        for (int row = 1; row <= rowCount; row++) {
            boolean isEmptyRow = true;
            for (int col = 1; col <= colCount; col++) {
                if (occupiedCells[row][col]) {
                    isEmptyRow = false;
                    break;
                }
            }
            if (isEmptyRow) {
                // 删除该行
                rows.remove(row);
                deleteRows.add(row);
            }
        }

        //如果整列都为空,则删除该列
        for (int col = 1; col <= colCount; col++) {
            boolean isEmptyCol = true;
            for (int row = 1; row <= rowCount; row++) {
                if (occupiedCells[row][col]) {
                    isEmptyCol = false; 
                    break;
                }
            }
            if (isEmptyCol) {
                // 删除该列
                columns.remove(col);    
                deleteColumns.add(col);
            }
        }


        // 检查并添加缺失的单元格
        ObjectMapper objectMapper = new ObjectMapper();
        for (int row = 1; row <= rowCount; row++) {
            for (int col = 1; col <= colCount; col++) {
                if (!occupiedCells[row][col]) {
                    if(deleteRows.contains(row) || deleteColumns.contains(col)) {
                        continue;
                    }
                    // 创建新的gridCell组件
                    ObjectNode gridCell = objectMapper.createObjectNode();
                    gridCell.put("type", "gridCell");
                    gridCell.putObject("settings");
                    // 创建cellColRow
                    ObjectNode cellColRow = objectMapper.createObjectNode();
                    cellColRow.put("rowIndex", row);
                    cellColRow.put("flexRowSize", 1);
                    cellColRow.put("colIndex", col);
                    cellColRow.put("flexColSize", 1);
                    
                    gridCell.set("cellColRow", cellColRow);
                    
                    // 添加到components数组
                    components.add(gridCell);
                }
            }
        }

        //遍历components ->component 和 deleteRows -> deleteRow 和 deleteColumns -> deleteColumn.
        //如果component[rowIndex] > deleteRow ,则修改component[rowIndex] = component[rowIndex] - 1
        //如果component[colIndex] > deleteColumn ,则修改component[colIndex] = component[colIndex] - 1
        //如果component[rowIndex] > deleteRow 且 component[colIndex] > deleteColumn ,则修改component[rowIndex] = component[rowIndex] - 1 和 component[colIndex] = component[colIndex] - 1


        for(JsonNode component : components) {
            int deleteRowNum = 1;
            int deleteColumnNum = 1;
            for (Integer deleteRow : deleteRows) {
                if(component.has("cellColRow")) {
                    
                    ObjectNode cellColRow = (ObjectNode) component.get("cellColRow");
                    int rowIndex = cellColRow.get("rowIndex").asInt();
                    if(rowIndex > deleteRow) {

                        cellColRow.put("rowIndex", rowIndex - deleteRowNum);
                        deleteRowNum++;
                    }
                }
            };

            for (Integer deleteColumn : deleteColumns) {
                if(component.has("cellColRow")) {
                    
                    ObjectNode cellColRow = (ObjectNode) component.get("cellColRow");
                    int colIndex = cellColRow.get("colIndex").asInt();
                    if(colIndex > deleteColumn) {

                        cellColRow.put("colIndex", colIndex - deleteColumnNum);
                        deleteColumnNum++;
                    }
                   
                }
            };

            
        }
        
        
    }


}
