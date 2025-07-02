package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaTransformerBase {


    /**
     * 递归查找标题节点Id
     * @param node 要处理的节点
     * @return 标题节点id
     */
    public static String getTitleNode(JsonNode node) {
        if (node == null) return null;

        // 检查当前节点是否是目标节点
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText())) {
            if (node.has("name")) {
                String name = node.get("name").asText();
                if ("表头".equals(name) || "文单标题".equals(name) || "表单标题".equals(name)) {
                    // 找到已经设置为表头的节点，返回其id
                    return node.has("id") ? node.get("id").asText() : null;
                }
            }
        }

        // 递归处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                String result = getTitleNode(child);
                if (result != null) {
                    return result;
                }
            }
        }

        // 处理所有字段，以防有嵌套的节点
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String result = getTitleNode(entry.getValue());
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

//    /**
//     * 递归查找最大fontSize的节点
//     * @param node 要处理的节点
//     * @param maxFontSizeRef 存储最大fontSize的引用
//     * @param hasTitleNodeRef 存储是否找到标题节点的引用
//     */
//    private static void findMaxFontSizeNode(JsonNode node, java.util.concurrent.atomic.AtomicInteger maxFontSizeRef, java.util.concurrent.atomic.AtomicReference<String> hasTitleNodeRef) {
//        if (node == null) return;
//
//        // 检查当前节点是否是目标节点
//        if (node.has("type") && "label".equals(node.get("type").asText())){
//            if(node.has("settings") && node.get("settings").has("textFontSize")) {
//                int fontSize = node.get("settings").get("textFontSize").asInt();
//                if(fontSize > maxFontSizeRef.get()){
//                    maxFontSizeRef.set(fontSize);
//                    hasTitleNodeRef.set(node.get("id").asText());
//                }
//            }
//        }
//
//        // 递归处理子节点
//        if (node.has("children") && node.get("children").isArray()) {
//            for (JsonNode child : node.get("children")) {
//                findMaxFontSizeNode(child, maxFontSizeRef, hasTitleNodeRef);
//            }
//        }
//
//
//        // 处理所有字段，以防有嵌套的节点
//        if (node.isObject()) {
//            node.fields().forEachRemaining(entry -> {
//                findMaxFontSizeNode(entry.getValue(), maxFontSizeRef, hasTitleNodeRef);
//            });
//        }
//    }

    /**
     * 递归查找,并设置"表头"
     *
     * if(不存在表头){
     * 	查找最大fontSize的label, 设置为表头.
     * }
     *
     * if(表格开始 && 第一行是独立单元格,里面只有一个label组件 && fontSize>14){
     * 	将第一行从原有网格中拆分出来.
     * }
     *
     * @param node 要处理的节点
     */
    public static void initTitleNodeName(JsonNode node) {
        // 查找最大fontSize的label作为标题
        String titleNodeId = getTitleNode(node);

        if(StringUtils.isBlank(titleNodeId)) {
            titleNodeId = getTitleNodeId(node, 0, "");
        }
        if (titleNodeId != null && !"".equals(titleNodeId)) {
            // 将找到的最大fontSize的节点设置为表头
            boolean updated = updateTitleNodeName(node, titleNodeId, "表头");
            
            // 确保设置成功
            if (!updated) {
                // 如果设置失败，尝试直接查找并设置
                findAndSetTitleNode(node);
            }
        } else {
            // getTitleNodeId返回空字符串（可能是找到了多个具有相同最大字体大小的节点）
            // 使用findAndSetTitleNode方法尝试设置标题
            findAndSetTitleNode(node);
        }

        // 处理网格拆分
        splitTitleGrid(node, null);
    }
    
    /**
     * 直接查找并设置最大fontSize的节点为表头
     * @param node 要处理的节点
     */
    private static void findAndSetTitleNode(JsonNode node) {
        if (node == null) return;
        
        // 使用AtomicInteger来存储最大fontSize
        final java.util.concurrent.atomic.AtomicInteger maxFontSizeRef = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicReference<String> titleNodeIdRef = new java.util.concurrent.atomic.AtomicReference<>("");
        // 添加计数器来跟踪具有最大fontSize的节点数量
        final java.util.concurrent.atomic.AtomicInteger maxFontSizeCountRef = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // 递归查找最大fontSize的节点
        findMaxFontSizeNode(node, maxFontSizeRef, titleNodeIdRef, maxFontSizeCountRef);
        
        // 如果找到了唯一的最大fontSize的节点，设置为表头
        String titleNodeId = titleNodeIdRef.get();
        if (titleNodeId != null && !"".equals(titleNodeId) && maxFontSizeCountRef.get() == 1) {
            updateTitleNodeName(node, titleNodeId, "表头");
        }
    }

    /**
     * 递归查找最大fontSize的节点（增强版，可以追踪多个相同最大值的节点）
     * @param node 要处理的节点
     * @param maxFontSizeRef 存储最大fontSize的引用
     * @param titleNodeIdRef 存储标题节点ID的引用
     * @param maxFontSizeCountRef 存储具有最大fontSize的节点数量的引用
     */
    private static void findMaxFontSizeNode(JsonNode node, 
                                         java.util.concurrent.atomic.AtomicInteger maxFontSizeRef, 
                                         java.util.concurrent.atomic.AtomicReference<String> titleNodeIdRef,
                                         java.util.concurrent.atomic.AtomicInteger maxFontSizeCountRef) {
        if (node == null) return;


        // 检查当前节点是否是目标节点
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText())){
            if(node.has("settings") && node.get("settings").has("textFontSize")) {
                JsonNode fontSizeNode = node.get("settings").get("textFontSize");
                int fontSize = 0;
                if (fontSizeNode == null) {
                    fontSize =  14;
                } else if(fontSizeNode.has("simple")){
                    String fontSizText = fontSizeNode.get("simple").asText();
                    if(StringUtils.isNotBlank(fontSizText) && fontSizText.length()>5){
                        String replaceFontSizText = fontSizText.replace("font-", "");
                        //如果replaceFontSizText是数字
                        if(StringUtils.isNumeric(replaceFontSizText)){
                            fontSize = 10 + (Integer.parseInt(replaceFontSizText)*2);

                        }
                    }
                }
                if(fontSizeNode.isNumber()){
                    fontSize = fontSizeNode.asInt();
                }
                if(fontSize > maxFontSizeRef.get()){
                    // 找到更大的字体大小，重置计数器为1
                    maxFontSizeRef.set(fontSize);
                    titleNodeIdRef.set(node.get("id").asText());
                    maxFontSizeCountRef.set(1);
                } else if (fontSize == maxFontSizeRef.get() && fontSize > 0) {
                    // 找到相同字体大小的另一个节点，增加计数器
                    maxFontSizeCountRef.incrementAndGet();
                }
            }
        }

        // 递归处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                findMaxFontSizeNode(child, maxFontSizeRef, titleNodeIdRef, maxFontSizeCountRef);
            }
        }

        // 处理所有字段，以防有嵌套的节点
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                findMaxFontSizeNode(entry.getValue(), maxFontSizeRef, titleNodeIdRef, maxFontSizeCountRef);
            });
        }
    }
    
    /**
     * 为兼容现有调用重载的方法
     */
    private static void findMaxFontSizeNode(JsonNode node, java.util.concurrent.atomic.AtomicInteger maxFontSizeRef, java.util.concurrent.atomic.AtomicReference<String> titleNodeIdRef) {
        // 调用增强版方法，使用默认的计数器初始值0
        findMaxFontSizeNode(node, maxFontSizeRef, titleNodeIdRef, new java.util.concurrent.atomic.AtomicInteger(0));
    }

    /**
     * 拆分标题行
     */
    private static void splitTitleGrid(JsonNode node, JsonNode parentNode) {
        if (node == null) return;

        if (node.has("type") && "grid".equals(node.get("type").asText())) {
            JsonNode children = node.get("children");
            if (children != null && children.isArray() && children.size() > 0) {
                JsonNode firstCell = children.get(0);

                // 检查是否是合并的单元格且包含label
                if (firstCell.has("settings") && 
                    firstCell.get("settings").has("isMergeAllColumns") && 
                    firstCell.get("settings").get("isMergeAllColumns").asBoolean()) {
//                    ((ObjectNode)firstCell).put("name","表头");



                        // 创建新的网格结构
                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode newGrid = mapper.createObjectNode();
                        newGrid.put("id", node.get("id").asText());
                        newGrid.put("type", "grid");
                        
                        // 复制并修改原网格设置
                        ObjectNode settings = mapper.createObjectNode();
                        if (node.has("settings")) {
                            JsonNode originalSettings = node.get("settings");

                            // 复制 gridTemplateColumns
                            if (originalSettings.has("gridTemplateColumns")) {
                                settings.set("gridTemplateColumns", originalSettings.get("gridTemplateColumns").deepCopy());
                            }
                            // 作废  columns 和 rows  (gridTemplateColumns 和gridTemplateRows 数量替代)
//                            if (originalSettings.has("rows")) {
//                                settings.put("rows", originalSettings.get("rows").asInt() - 1);
//                            }
//                            if (originalSettings.has("columns")) {
//                                settings.put("columns", originalSettings.get("columns").asInt());
//                            }
                            // 设置 gridTemplateRows - 去掉第一行
                            if (originalSettings.has("gridTemplateRows")) {
                                ArrayNode originalRows = (ArrayNode) originalSettings.get("gridTemplateRows");
                                ArrayNode newRows = settings.putArray("gridTemplateRows");
                                for (int i = 1; i < originalRows.size(); i++) {
                                    newRows.add(originalRows.get(i));
                                }
                            }
                        }
                        newGrid.set("settings", settings);
                        
                        // 设置sourceSchema
                        if (node.has("sourceSchema")) {
                            newGrid.set("sourceSchema", node.get("sourceSchema"));
                        }
                        
                        // 创建标题行网格
                        ObjectNode titleGrid = mapper.createObjectNode();
                        titleGrid.put("id", node.get("id").asText());
                        titleGrid.put("type", "grid");
                        
                        // 为标题网格创建设置
                        ObjectNode titleSettings = mapper.createObjectNode();
                        if (node.has("settings")) {
                            JsonNode originalSettings = node.get("settings");
                            // 复制 gridTemplateColumns
                            if (originalSettings.has("gridTemplateColumns")) {
                                titleSettings.set("gridTemplateColumns", originalSettings.get("gridTemplateColumns").deepCopy());
                            }

                            // 设置 gridTemplateRows - 只保留第一行
                            if (originalSettings.has("gridTemplateRows")) {
                                ArrayNode titleRows = titleSettings.putArray("gridTemplateRows");
                                titleRows.add(originalSettings.get("gridTemplateRows").get(0));
                            }
                        }
                        titleGrid.set("settings", titleSettings);
                        
                        if (node.has("sourceSchema")) {
                            titleGrid.set("sourceSchema", node.get("sourceSchema"));
                        }
                        
                        // 移动标题单元格到标题网格
                        ArrayNode titleChildren = mapper.createArrayNode();
                        // 创建内容网格
                         ArrayNode contentChildren = mapper.createArrayNode();
                        //将一个grid 拆分成2个.

                        for (JsonNode cell : children) {
                            if(cell.get("settings").has("styles") &&  cell.get("settings").get("styles").has("gridRow")){
                                String text = cell.get("settings").get("styles").get("gridRow").asText();
                                if ("1/2".equals(text)) {
                                    if(cell.has("children") && cell.get("children").size()>0){
                                        ObjectNode titleObjectNode = (ObjectNode)cell.get("children").get(0);
                                        if (titleObjectNode != null) {
                                            titleObjectNode.put("name","表头");
                                        }

                                    }
                                    titleChildren.add(cell);
                                } else {
                                    contentChildren.add(cell);
                                }
                            }
                        }


                        titleGrid.set("children", titleChildren);


                        newGrid.set("children", contentChildren);
                        
                        // 替换原网格节点
                        if (parentNode != null && parentNode.has("children")) {
                            ArrayNode parentChildren = (ArrayNode) parentNode.get("children");
                            int index = findNodeIndex(parentChildren, node);
                            if (index >= 0) {
                                parentChildren.remove(index);
                                parentChildren.insert(index, titleGrid);
                                parentChildren.insert(index + 1, newGrid);
                            }
                        }

                }
            }
        }

        // 递归处理子节点 - 修复ConcurrentModificationException
        if (node.has("children") && node.get("children").isArray()) {
            // 创建一个临时列表存储子节点，避免在遍历过程中修改集合
            List<JsonNode> childrenList = new ArrayList<>();
            node.get("children").forEach(childrenList::add);
            
            // 使用临时列表进行遍历
            for (JsonNode child : childrenList) {
                splitTitleGrid(child, node);
            }
        }
    }

    /**
     * 查找第一个label节点
     */
    private static JsonNode findFirstLabelNode(JsonNode node) {
        if (node == null) return null;
        
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText())) {
            return node;
        }
        
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                JsonNode label = findFirstLabelNode(child);
                if (label != null) {
                    return label;
                }
            }
        }
        
        return null;
    }

    /**
     * 查找节点在数组中的索引
     */
    private static int findNodeIndex(ArrayNode array, JsonNode target) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 递归更新节点中的标题内容
     * @param node 要处理的节点
     * @param titleNodeId 要更新的节点ID
     * @param titleName 要设置的标题内容
     * @return 是否找到并更新了目标节点
     */
    public static boolean updateTitleNodeName(JsonNode node, String titleNodeId, String titleName) {
        if (node == null) return false;

        // 检查当前节点是否是目标节点
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText())
                && node.has("id") && titleNodeId.equals(node.get("id").asText())) {

            // 找到目标节点，更新name
            ((ObjectNode) node).put("name", titleName);
            return true;
        }

        // 递归处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                if (updateTitleNodeName(child, titleNodeId, titleName)) {
                    return true;
                }
            }
        }

        // 处理所有字段，以防有嵌套的节点
        if (node.isObject()) {
            final java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean(false);
            node.fields().forEachRemaining(entry -> {
                if (updateTitleNodeName(entry.getValue(), titleNodeId, titleName)) {
                    found.set(true);
                }
            });
            if (found.get()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 递归查找标题节点Id
     * @param node 要处理的节点
     * @return 标题节点id
     */
    public static String getTitleNodeId(JsonNode node, int textFontSize, String titleNodeId) {
        if (node == null) return "";

        // 使用AtomicInteger来存储fontSize，使其可以在lambda中修改
        final java.util.concurrent.atomic.AtomicInteger fontSizeRef = new java.util.concurrent.atomic.AtomicInteger(textFontSize);
        final java.util.concurrent.atomic.AtomicReference<String> titleNodeIdRef = new java.util.concurrent.atomic.AtomicReference<>(titleNodeId);
        // 添加计数器来跟踪具有最大fontSize的节点数量
        final java.util.concurrent.atomic.AtomicInteger maxFontSizeCountRef = new java.util.concurrent.atomic.AtomicInteger(textFontSize > 0 ? 1 : 0);

        // 检查当前节点是否是目标节点
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText())){
            String nodeId = node.get("id").asText();
            // 只考虑textFontSize最大的节点，不再考虑name="表头"的情况
            if(node.has("settings") && node.get("settings").has("textFontSize")) {
                int tempTextFontSize = node.get("settings").get("textFontSize").asInt();
                if(tempTextFontSize > fontSizeRef.get()){
                    // 找到更大的字体大小，重置计数器为1
                    fontSizeRef.set(tempTextFontSize);
                    titleNodeIdRef.set(nodeId);
                    maxFontSizeCountRef.set(1);
                } else if(tempTextFontSize == fontSizeRef.get() && tempTextFontSize > 0){
                    // 找到相同字体大小的另一个节点，增加计数器
                    maxFontSizeCountRef.incrementAndGet();
                }
            }
        }

        // 递归处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                String result = getTitleNodeId(child, fontSizeRef.get(), titleNodeIdRef.get());
                if (!result.isEmpty()) {
                    // 检查子递归是否找到了唯一的最大字体大小
                    if (maxFontSizeCountRef.get() > 1) {
                        return ""; // 如果有多个相同的最大字体大小，返回空字符串
                    }
                    return result;
                }
            }
        }

        // 处理所有字段，以防有嵌套的节点
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String result = getTitleNodeId(entry.getValue(), fontSizeRef.get(), titleNodeIdRef.get());
                if (!result.isEmpty()) {
                    titleNodeIdRef.set(result);
                }
            });
        }

        // 检查是否找到了唯一的最大字体大小
        if (maxFontSizeCountRef.get() > 1) {
            return ""; // 如果有多个相同的最大字体大小，返回空字符串
        }
        
        return titleNodeIdRef.get();
    }


    /**
     * 递归更新节点中的标题内容
     * @param node 要处理的节点
     * @param titleName 要设置的标题内容
     * @return 是否找到并更新了目标节点
     */
    public static Boolean updateTitleNode(JsonNode node, String titleName) {
        if (node == null) {
            return false;
        }
        
        // 检查当前节点是否是目标节点
        if (node.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(node.get("type").asText()) 
            && node.has("name") ) {
            String name = node.get("name").asText();

            if( "表头".equals(name) || "文单标题".equals(name) || "表单标题".equals(name)) {
                JsonNode settings = node.get("settings");
                if (settings == null) {
                    ObjectMapper mapper = new ObjectMapper();
                    settings = mapper.createObjectNode();
                    ((ObjectNode) node).set("settings", settings);
                }
                // 找到目标节点，更新content
                ((ObjectNode) settings).put("content", titleName);
                return true;
            }
        }

        // 递归处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                if (updateTitleNode(child, titleName)) {
                    return true;
                }
            }
        }

        // 处理所有字段，以防有嵌套的节点
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (updateTitleNode(entry.getValue(), titleName)) {
                    return true;
                }
            }
        }

        return false;
    }

     /**
     * 递归查找第一个form节点, 设置dataSource->entityName 
     */
    public static void updateFormNode(JsonNode currNode, String entityName) {
        if (currNode.has("type") && "form".equals(currNode.get("type").asText())) {
            JsonNode dataSource = currNode.get("dataSource");
            if(dataSource == null){
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode dataSourceNew = mapper.createObjectNode();
                dataSourceNew.put("entityName", entityName);
                ((ObjectNode)currNode).set("dataSource",dataSourceNew);
            }else {
                ((ObjectNode) dataSource).put("entityName", entityName);
            }
            return;
        }

        if (currNode.has("children") && currNode.get("children").isArray()) {
            for (JsonNode child : currNode.get("children")) {
                updateFormNode(child, entityName);
            }
        }

    }



    


}
