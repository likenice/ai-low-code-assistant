package com.seeyon.ai.schematransformer.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.constant.ComponentTypeContants;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class SchemaTransformerUtil {
    

    public static String generateShortUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidStr = Base64.getUrlEncoder().withoutPadding().encodeToString(uuid.toString().getBytes(StandardCharsets.UTF_8));
        // 只取前部分作为短UUID
        return uuidStr.substring(0, Math.min(uuidStr.length(), 11)); // 根据需要调整长度
    }


    /**
     * 递归遍历dslSchema,返回所有type节点信息
     */
    public static List<NodePosition> getAllTypeNodeInfo(JsonNode dslSchema, String type) {
        // 创建NodePosition实例
        List<NodePosition> nodePositionList = new ArrayList<>();

        // 递归遍历并处理节点
        recursionNodePositionList(dslSchema,null, type, nodePositionList,  -1);

        return nodePositionList;
    }


    /**
     * 递归遍历节点并处理
     * @param node 当前节点
     * @param type 要查找的类型
     * @param nodePositionList 节点位置信息
     * @param parentNode 父节点
     * @param currentIndex 当前节点在父节点children中的索引
     */
    private static void recursionNodePositionList(JsonNode node, JsonNode parentNode,String type, List<NodePosition> nodePositionList,
                                                       int currentIndex) {
        if (node == null || !node.isObject()) {
            return;
        }
        NodePosition nodePosition = new NodePosition();

        // 检查当前节点类型
        if (node.has("type")) {
            String currType = node.get("type").asText();
            // 如果是目标type节点且还未找到过
            if (currType.equals(type)) {

//                    if (parentNode != null) {
                        if(parentNode != null && parentNode.has("id")) {
                            nodePosition.setParentId(parentNode.get("id").asText());
                        }else{
                            nodePosition.setParentId(null);
                        }
                        nodePosition.setIndex(currentIndex);
                        if(node != null && node.has("id")) {
                            nodePosition.setId(node.get("id").asText());
                        }
                        nodePosition.setObjectNode((ObjectNode) node);

                        nodePositionList.add(nodePosition);
//                    }

            }
        }

        // 处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                JsonNode child = children.get(i);
                recursionNodePositionList(child, node, type, nodePositionList, i);
            }
        }

    }


    /**
     * 获取dataGridId同级别节点
     */
    public static void getNodePositionListById(JsonNode node, JsonNode parentNode,String dataGridId,List<NodePosition> currIdParentNodeList) {
        if (node == null || !node.isObject()) {
            return;
        }


        // 检查当前节点类型
        if (node.has("id") && parentNode != null) {
            String currId = node.get("id").asText();

            if (currId.equals(dataGridId)) {
                ArrayNode parentJsonNodeChild = (ArrayNode) parentNode.get("children");
                for (int i = 0; i < parentJsonNodeChild.size(); i++) {
                    NodePosition nodePosition = new NodePosition();
                    JsonNode matchNode = parentJsonNodeChild.get(i);
                    if (matchNode.get("id") != null && !matchNode.get("id").asText().equals(currId)) {
                        nodePosition.setId(currId);
                        nodePosition.setIndex(i);
                        nodePosition.setParentId(parentNode.get("id").asText());
                        nodePosition.setObjectNode((ObjectNode)matchNode);
                        currIdParentNodeList.add(nodePosition);
                    }
                }

            }
        }

        // 处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                JsonNode child = children.get(i);
                getNodePositionListById(child, node, dataGridId, currIdParentNodeList);
            }
        }

    }


    

    /**
     * 删除指定父节点下特定索引位置的节点
     * @param node 当前节点
     * @return 处理后的JsonNode
     */
    public static void deleteNodePosition(ObjectNode node, NodePosition nodePosition) {
        if (node == null || !node.isObject()) {
            return;
        }

        String targetParentId = nodePosition.getParentId();
        int targetIndex = nodePosition.getIndex();

        ObjectNode result = node;

        // 检查当前节点是否是目标父节点
        if (result.has("id") && result.get("id").asText().equals(targetParentId)) {
            if (result.has("children") && result.get("children").isArray()) {
                ArrayNode children = (ArrayNode) result.get("children");
                if (targetIndex >= 0 && targetIndex < children.size()) {
                    children.remove(targetIndex);
                }
            }
        }

        // 递归处理children节点
        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            for (JsonNode child : children) {
                if (child.isObject()) {
                    deleteNodePosition((ObjectNode) child, nodePosition);
                }
            }
        }

        return;
    }


    /**
     * 删除指定父节点下特定id的节点
     * @param node 当前节点
     * @return 处理后的JsonNode
     */
    public static void deleteNodeById(ObjectNode node, String deleteId) {
        if (node == null || !node.isObject()) {
            return;
        }

        ObjectNode result = node;

        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            // 使用倒序遍历来安全删除元素
            for (int i = children.size() - 1; i >= 0; i--) {
                JsonNode child = children.get(i);
                if (child.has("id") && child.get("id").asText().equals(deleteId)) {
                    children.remove(i);
                }
            }
        }

        // 递归处理children节点
        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            for (JsonNode child : children) {
                if (child.isObject()) {
                    deleteNodeById((ObjectNode) child, deleteId);
                }
            }
        }
    }


    /**
     * 删除指定父节点下特定name的节点
     * @param node 当前节点
     * @return 处理后的JsonNode
     */
    public static void deleteNodeByName(ObjectNode node, String deleteName) {
        if (node == null || !node.isObject()) {
            return;
        }

        ObjectNode result = node;

        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            // 使用倒序遍历来安全删除元素
            for (int i = children.size() - 1; i >= 0; i--) {
                JsonNode child = children.get(i);
                if (child.has("name") && child.get("name").asText().equals(deleteName)) {
                    children.remove(i);
                }
            }
        }

        // 递归处理children节点
        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            for (JsonNode child : children) {
                if (child.isObject()) {
                    deleteNodeByName((ObjectNode) child, deleteName);
                }
            }
        }
    }



    /**
     * 删除指定父节点下特定类型的节点
     * @param node 当前节点
     * @return 处理后的JsonNode
     */
    public static void deleteNodeByType(ObjectNode node, String deleteType) {
        if (node == null || !node.isObject()) {
            return;
        }

        ObjectNode result = node;

        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            // 使用倒序遍历来安全删除元素
            for (int i = children.size() - 1; i >= 0; i--) {
                JsonNode child = children.get(i);
                if (child.has("type") && child.get("type").asText().equals(deleteType)) {
                    children.remove(i);
                }
            }
        }

        // 递归处理children节点
        if (result.has("children") && result.get("children").isArray()) {
            ArrayNode children = (ArrayNode) result.get("children");
            for (JsonNode child : children) {
                if (child.isObject()) {
                    deleteNodeByType((ObjectNode) child, deleteType);
                }
            }
        }
    }

    /**
     * 递归遍历dslSchema,找到nodePosition节点位置,并删除.
     * @param dslSchema 要处理的schema
     * @param type 要查找的类型
     * @param deleteScope 是否删除所有type节点  null: 不删除, deleteCurr: 删除当前, deleteAll: 删除全部
     * @return 第一个type节点的位置信息
     */
    public static NodePosition getFirstTypeNodeInfo(ObjectNode dslSchema, String type,String deleteScope) {
        // 创建NodePosition实例
        NodePosition nodePosition = new NodePosition();
        
        // 递归遍历并处理节点
        traverseAndProcess(dslSchema, type, nodePosition, null, -1, deleteScope);
        
        return nodePosition;
    }


    /**
     * 递归遍历节点并处理
     * @param node 当前节点
     * @param type 要查找的类型
     * @param nodePosition 节点位置信息
     * @param parentNode 父节点
     * @param currentIndex 当前节点在父节点children中的索引
     */
    public static void traverseAndProcess(JsonNode node, String type, NodePosition nodePosition,
            JsonNode parentNode, int currentIndex, String deleteScope) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 检查当前节点类型
        if (node.has("type")) {
            String currType = node.get("type").asText();
            // 如果是目标type节点且还未找到过
            if (currType.equals(type)) {
                //将第一次找到的type节点位置信息保存到nodePosition
                if(!nodePosition.isFound()) {
                    nodePosition.setFound(true);
                    if (parentNode != null) {
                        nodePosition.setParentId(parentNode.get("id").asText());
                        nodePosition.setIndex(currentIndex);
                    }
                }

                if (deleteScope != null && "deleteAll".equals(deleteScope) && parentNode != null) {
                    //从父节点的children中删除当前节点
                    ArrayNode parentChildren = (ArrayNode) parentNode.get("children");
                    parentChildren.remove(currentIndex);
                    // if("deleteCurr".equals(deleteScope)){
                    //     return; // 删除后直接返回，不再继续遍历
                    // }
                    // 由于删除了当前节点，需要调整后续遍历的索引
                    traverseAndProcess(parentNode, type, nodePosition, parentNode.deepCopy(), currentIndex, deleteScope);
                    return;
                }
            }
        }

        // 处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                JsonNode child = children.get(i);
                traverseAndProcess(child, type, nodePosition, node, i, deleteScope);
            }
        }
    }

    
    /**
     * 在所有指定的collapse位置插入layoutList
     * @param dslSchema 原始schema
     * @param nodePosition 节点位置信息
     * @param layoutList 要插入的布局列表
     * @return 处理后的schema
     */
    public static JsonNode insertLayoutListAtCollapsePosition(JsonNode dslSchema, NodePosition nodePosition, JsonNode layoutList) {
        if (dslSchema == null || nodePosition == null ) {
            throw new RuntimeException("dslSchema or nodePosition is null!");
        }

        if(nodePosition.getParentId() == null){
            return layoutList;
        }

        ObjectNode result = dslSchema.deepCopy();
        
        // 递归处理所有节点
        processNodeForInsertion(result, nodePosition.getParentId(), nodePosition.getIndex(), layoutList);
        
        return result;
    }

    /**
     * 递归处理节点，在匹配的位置插入layoutList
     * @param node 当前节点
     * @param targetParentId 目标父节点ID
     * @param targetIndex 目标索引位置
     * @param layoutList 要插入的布局列表
     */
    private static void processNodeForInsertion(ObjectNode node, String targetParentId, int targetIndex, JsonNode layoutList) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 检查当前节点是否是目标父节点
        if (node.has("id") && node.get("id").asText().equals(targetParentId)) {
            if (node.has("children") && node.get("children").isArray()) {
                ArrayNode children = (ArrayNode) node.get("children");
                // 在指定位置插入layoutList中的所有节点
                if(layoutList.isArray()) {
                    for (int i = 0; i < layoutList.size(); i++) {
                        children.insert(targetIndex + i, layoutList.get(i).deepCopy());
                    }
                }else{
                    children.insert(targetIndex , layoutList.deepCopy());
                }
            }
        }

        // 递归处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                if (child instanceof ObjectNode) {
                    processNodeForInsertion((ObjectNode)child, targetParentId, targetIndex, layoutList);
                }
            }
        }
    }

    
    /**
     * 递归检查节点中是否存在指定类型的节点
     * @param node 要检查的节点
     * @param type 要查找的类型
     * @return 是否存在该类型节点
     */
    public static boolean checkNodeTypeExists(JsonNode node, String type) {
        if (node == null) {
            return false;
        }
        
        // 检查当前节点类型
        if (node.has("type") && type.equalsIgnoreCase(node.get("type").asText())) {
            return true;
        }
        
        // 递归检查子节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                if (checkNodeTypeExists(child, type)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 处理gridTemplateColumns，将每一列按1:2比例拆分
     * @param originalColumns 原始的列宽数组
     * @return 拆分后的列宽数组
     */
    public static ArrayNode splitGridTemplateColumns(JsonNode originalColumns) {
        ArrayNode newColumns = JsonNodeFactory.instance.arrayNode();
        
        if (originalColumns != null && originalColumns.isArray()) {
            for (JsonNode column : originalColumns) {
                double originalWidth = column.asDouble();
                
                // 按1:2比例拆分每一列
                double firstPartWidth = (originalWidth / 3) * 2;  // 1/3
                double secondPartWidth = (originalWidth * 2 / 3) * 2;  // 2/3
                
                // 添加拆分后的两列宽度
                newColumns.add(formatDouble(firstPartWidth));
                newColumns.add(formatDouble(secondPartWidth));
            }
        }
        
        return newColumns;
    }

    /**
     * 格式化double值，保留6位小数
     * @param value 需要格式化的值
     * @return 格式化后的值
     */
    public static double formatDouble(double value) {
        // 保留6位小数
        return Math.round(value * 1000000.0) / 1000000.0;
    }

    /**
     * 过滤掉包含关系的List<NodePosition> nodePositionList, 保留最内层的节点
     * 判断依据NodePosition的id是否在另一个NodePosition的objectNode中. 
     * 如果存在,则删除当前NodePosition
     * 
     * @param nodePositionList 需要过滤的节点位置列表
     */
    public static void filterNestedNodePosition(List<NodePosition> nodePositionList) {
        if (nodePositionList == null || nodePositionList.isEmpty()) {
            return;
        }

        // 使用迭代器来安全删除元素
        Iterator<NodePosition> iterator = nodePositionList.iterator();
        while (iterator.hasNext()) {
            NodePosition current = iterator.next();
            
            // 检查当前节点是否包含其他节点
            boolean isContainingOthers = false;
            for (NodePosition other : nodePositionList) {
                if (current != other && isNodeContainedIn(other.getId(), current.getObjectNode())) {
                    isContainingOthers = true;
                    break;
                }
            }
            
            // 如果当前节点包含其他节点，则将其删除（保留最内层节点）
            if (isContainingOthers) {
                iterator.remove();
            }
        }
    }

    /**
     * 检查指定id的节点是否包含在给定的JsonNode中
     * 
     * @param id 要检查的节点id
     * @param node 要搜索的JsonNode
     * @return 如果包含返回true，否则返回false
     */
    private static boolean isNodeContainedIn(String id, JsonNode node) {
        if (node == null) {
            return false;
        }

        // 检查当前节点
        if (node.has("id") && id.equals(node.get("id").asText())) {
            return true;
        }

        // 递归检查children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                if (isNodeContainedIn(id, child)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static NodePosition getMostFrequentNodePosition(JsonNode jsonNode){
        List<NodePosition> gridNodePositionList = SchemaTransformerUtil.getAllTypeNodeInfo(jsonNode, "grid");
        List<NodePosition> containerNodePositionList = SchemaTransformerUtil.getAllTypeNodeInfo(jsonNode, "container");
        List<NodePosition> allList = new ArrayList<>();
        allList.addAll(gridNodePositionList);
        allList.addAll(containerNodePositionList);
        Set<String> entityOptionTypes = SchemaTransformerUtil.getEntityOptionTypes();
        //网格下的容器一般是包含组件
        entityOptionTypes.add("container");
        return SchemaTransformerUtil.getMostFrequentGridOrContainerNodeType(allList, entityOptionTypes);

    }

    /**
     * 查找出现nodePositionList 中 , 包含entityOptionTypes最多的NodePosition
     * @param nodePositionList 节点位置列表(必须是grid或container)
     * @param entityOptionTypes 允许的节点类型集合
     * @return 出现频率最高的节点位置信息
     */
    public static NodePosition getMostFrequentGridOrContainerNodeType(List<NodePosition> nodePositionList, Set<String> entityOptionTypes) {
        if (nodePositionList == null || nodePositionList.isEmpty()) {
            return null;
        }
        //过滤掉重复的外部节点
//        filterNestedNodePosition(nodePositionList);

        // 用于统计每个NodePosition出现的次数
        Map<NodePosition, Integer> frequencyMap = new HashMap<>();

        // 遍历每个NodePosition
        for (NodePosition position : nodePositionList) {
            JsonNode node = position.getObjectNode();
            if (node == null) {
                continue;
            }

            // 统计当前节点中各类型节点的数量
            if(node.has("type") && "grid".equals(node.get("type").asText())){
                countGridContentTypes(node, entityOptionTypes, position, frequencyMap);
            };
            if(node.has("type") && "container".equals(node.get("type").asText())){
                countContainerContentTypes(node, entityOptionTypes, position, frequencyMap);
            };

        }

        // 找出出现次数最多的NodePosition
        // 过滤掉frequencyMap 值为0
        return frequencyMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    
    /**
     * 查找出现nodePositionList 中 , 包含entityOptionTypes最多的NodePosition
     * @param nodePositionList 节点位置列表
     * @param entityOptionTypes 允许的节点类型集合
     * @return 出现频率最高的节点位置信息
     */
    public static NodePosition getMostFrequentNodeType(List<NodePosition> nodePositionList, Set<String> entityOptionTypes) {
        if (nodePositionList == null || nodePositionList.isEmpty()) {
            return null;
        }
        //过滤掉重复的外部节点
//        filterNestedNodePosition(nodePositionList);

        // 用于统计每个NodePosition出现的次数
        Map<NodePosition, Integer> frequencyMap = new HashMap<>();
        
        // 遍历每个NodePosition
        for (NodePosition position : nodePositionList) {
            JsonNode node = position.getObjectNode();
            if (node == null) {
                continue;
            }
            
            // 统计当前节点中各类型节点的数量
            countNodeTypes(node, entityOptionTypes, position, frequencyMap);
        }
        
        // 找出出现次数最多的NodePosition
        // 过滤掉frequencyMap 值为0
        return frequencyMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 递归统计节点类型数量
     * @param node 当前节点
     * @param entityOptionTypes 需要排除的类型
     * @param position 节点位置信息
     * @param frequencyMap 频率统计Map
     */
    private static void countNodeTypes(JsonNode node, Set<String> entityOptionTypes, NodePosition position, Map<NodePosition, Integer> frequencyMap) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 检查当前节点类型
        if (node.has("type")) {
            String type = node.get("type").asText();
            if (entityOptionTypes.contains(type)) {
                // 更新频率统计
                frequencyMap.merge(position, 1, Integer::sum);
            }
        }

        // 递归处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                countNodeTypes(child, entityOptionTypes, position, frequencyMap);
            }
        }
    }


    /**
     * 网格节点容纳的组件数量
     * @param node 当前节点
     * @param entityOptionTypes 需要排除的类型
     * @param position 节点位置信息
     * @param frequencyMap 频率统计Map
     */
    private static void countGridContentTypes(JsonNode node, Set<String> entityOptionTypes, NodePosition position, Map<NodePosition, Integer> frequencyMap) {
        if (node == null || !node.isObject() || !node.has("children")) {
            return;
        }
        if(!"grid".equalsIgnoreCase(node.get("type").asText())){
            throw new RuntimeException("countGridContentTypes 的入参 node 必须是 grid类型");
        }

        //node是grid组件
        node.get("children").forEach(child -> {
                    if (child.has("type") && "gridCell".equalsIgnoreCase(child.get("type").asText())) {
                        if (child.has("children") && child.get("children").isArray()) {
                            ArrayNode tempArrayNode = (ArrayNode)child.get("children");
                            for (JsonNode jsonNode : tempArrayNode) {
                                // 检查当前节点类型
                                if (jsonNode.has("type")) {
                                    String tempType = jsonNode.get("type").asText();
                                    if (entityOptionTypes.contains(tempType)) {
                                        // 更新频率统计
                                        frequencyMap.merge(position, 1, Integer::sum);
                                    }
                                }
                            }

                        }
                    }
                });


    }

    /**
     * 网格节点容纳的组件数量
     * @param node 当前节点
     * @param entityOptionTypes 需要排除的类型
     * @param position 节点位置信息
     * @param frequencyMap 频率统计Map
     */
    private static void countContainerContentTypes(JsonNode node, Set<String> entityOptionTypes, NodePosition position, Map<NodePosition, Integer> frequencyMap) {
        if (node == null || !node.isObject() || !node.has("children")) {
            return;
        }
        if(!"container".equalsIgnoreCase(node.get("type").asText())){
            throw new RuntimeException("countGridContentTypes 的入参 node 必须是 container类型");
        }

        //node是grid组件
        node.get("children").forEach(child -> {

//            if (child.has("children") && child.get("children").isArray()) {
//                ArrayNode tempArrayNode = (ArrayNode)child.get("children");
//                for (JsonNode jsonNode : tempArrayNode) {
                    // 检查当前节点类型
                    if (child.has("type")) {
                        String tempType = child.get("type").asText();
                        if (entityOptionTypes.contains(tempType)) {
                            // 更新频率统计
                            frequencyMap.merge(position, 1, Integer::sum);
                        }
                    }
//                }
//
//            }

        });


    }



    /**
     * 检查节点及其子节点是否包含指定类型的节点
     * @param objectNode 要检查的节点
     * @param checkType 要检查的类型
     * @return 如果找到指定类型的节点返回true，否则返回false
     */
    public static boolean isContentTypeNode(ObjectNode objectNode, String checkType) {
        if (objectNode == null || checkType == null) {
            return false;
        }
        
        // 检查当前节点的type
        String currType = "";
        if (objectNode.has("type")) {
            currType = objectNode.get("type").asText();
            if (checkType.equals(currType)) {
                return true;
            }
        }

        // 递归检查children节点
        if (objectNode.has("children") && objectNode.get("children").isArray()) {
            ArrayNode children = (ArrayNode) objectNode.get("children");
            for (JsonNode child : children) {
                if (child instanceof ObjectNode && isContentTypeNode((ObjectNode) child, checkType)) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * 递归查找第一个指定类型的节点
     * @param node 要搜索的节点
     * @param type 要查找的节点类型
     * @return 找到的第一个指定类型节点，如果未找到返回null
     */
    public static JsonNode getFirstNodeByType(JsonNode node, String type) {
        if (node == null) {
            return null;
        }

        // 检查当前节点是否为目标类型
        if (node.has("type") && type.equals(node.get("type").asText())) {
            return node;
        }

        // 递归检查子节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                JsonNode result = getFirstNodeByType(child, type);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 获取所有实体可操作组件. 不包括:label,grid,container等
     * @return
     */
    public static Set<String>  getEntityOptionTypes() {
        return  new HashSet<>(Arrays.asList(ComponentTypeContants.ALL_COMPONENT_TYPE.split(",")));
    }



    /**
     * 获取"dataGrid"节点评级中包含最多实体组件的节点.
     * @param jsonNode
     * @return
     */
    public static NodePosition getDataGridBrotherNode(JsonNode jsonNode){
        List<NodePosition> dataGridNodePositionList = getAllTypeNodeInfo(jsonNode, "dataGrid");
        //第一个dataGrid
        if(dataGridNodePositionList.size() > 0){
            NodePosition nodePosition = dataGridNodePositionList.get(0);
            String dataGridId = nodePosition.getId();
            List<NodePosition> currIdParentNodeList = new ArrayList<>();
            //获取dataGridId 同级别的 node , 放入currIdParentNodeList
            getNodePositionListById(jsonNode,null,dataGridId,currIdParentNodeList);

            if(!CollectionUtils.isEmpty(currIdParentNodeList)){
                //找到包含最多实体组件的NodePosition
                NodePosition mostFrequentNodeType = getMostFrequentNodeType(currIdParentNodeList, SchemaTransformerUtil.getEntityOptionTypes());
                return mostFrequentNodeType;
            }else{
                return null;
            }
        }
        return null;
    }


    /**
     * 获取"dataGrid"节点评级中包含最多实体组件的节点. 且不能是datagrid
     * @param jsonNode
     * @return
     */
    public static NodePosition getNodeHasMostComponent(JsonNode jsonNode,String type){
        JsonNode jsonNodeTemp = jsonNode.deepCopy();
        //排除datagrid影响.
        SchemaTransformerUtil.deleteNodeByType((ObjectNode) jsonNodeTemp, "dataGrid");
        List<NodePosition> gridNodePositionList = getAllTypeNodeInfo(jsonNodeTemp, type);
        if(!CollectionUtils.isEmpty(gridNodePositionList)){
            //找到包含最多实体组件的NodePosition
            NodePosition mostFrequentNodeType = getMostFrequentNodeType(gridNodePositionList, SchemaTransformerUtil.getEntityOptionTypes());
            return mostFrequentNodeType;
        }else{
            return null;
        }

    }


    /**
     * 根据id获取nodePosition
     * @param jsonNode 要搜索的JSON节点树
     * @param id 要查找的节点id
     * @return 找到的NodePosition，如果未找到返回null
     */
    public static NodePosition getNodePositionById(JsonNode jsonNode, String id){
        if (jsonNode == null || id == null) {
            return null;
        }
        
        NodePosition result = new NodePosition();
        getNodePositionByIdCore(jsonNode, null, -1, id, result);
        
        return result;
    }

    /**
     * 递归查找指定id的节点位置
     * @param node 当前节点
     * @param parentNode 父节点
     * @param currentIndex 当前节点在父节点children中的索引
     * @param targetId 目标id
     * @param result 存储结果的NodePosition对象
     */
    private static void getNodePositionByIdCore(JsonNode node, JsonNode parentNode, int currentIndex,
            String targetId, NodePosition result) {
        if (node == null || !node.isObject() ) {
            return;
        }

        // 检查当前节点是否匹配目标id
        if (node.has("id") && targetId.equals(node.get("id").asText())) {
            result.setFound(true);
            result.setObjectNode(node.deepCopy());
            if (parentNode != null) {
                result.setParentId(parentNode.get("id").asText());
                result.setIndex(currentIndex);
            }
            result.setId(targetId);
            return;
        }

        // 递归检查children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                getNodePositionByIdCore(children.get(i), node, i, targetId, result);
            }
        }
    }



    /**
     * 遍历jsonNode 节点, 找到第一个name 为name的节点, 并返回该节点. 
     * @param jsonNode 要搜索的JSON节点树
     * @param name 要查找的节点name
     * @return 找到的节点，如果未找到返回null
     */
    public static JsonNode getFirstJsonNodeByName(JsonNode jsonNode, String name){
        if (jsonNode == null || name == null) {
            return null;
        }
        
        // 检查当前节点是否匹配目标name
        if (jsonNode.has("name") && name.equals(jsonNode.get("name").asText())) {
            return jsonNode;
        }
        
        // 递归检查children节点
        if (jsonNode.has("children") && jsonNode.get("children").isArray()) {
            ArrayNode children = (ArrayNode) jsonNode.get("children");
            for (JsonNode child : children) {
                JsonNode result = getFirstJsonNodeByName(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }


    /**
     * 根据id获取nodePosition
     * @param jsonNode 要搜索的JSON节点树
     * @param name 要查找的节点name
     * @return 找到的NodePosition，如果未找到返回null
     */
    public static NodePosition getFirstNodePositionByName(JsonNode jsonNode, String name){
        if (jsonNode == null || name == null) {
            return null;
        }

        NodePosition result = new NodePosition();
        getNodePositionByNameCore(jsonNode, null, -1, name, result);

        return result;
    }




    /**
     * 递归查找指定id的节点位置
     * @param node 当前节点
     * @param parentNode 父节点
     * @param currentIndex 当前节点在父节点children中的索引
     * @param targetName 目标name
     * @param result 存储结果的NodePosition对象
     */
    private static void getNodePositionByNameCore(JsonNode node, JsonNode parentNode, int currentIndex,
                                                String targetName, NodePosition result) {
        if (node == null || !node.isObject() ) {
            return;
        }
        String targetId = node.get("id").asText();

        // 检查当前节点是否匹配目标id
        if (node.has("name") && targetName.equals(node.get("name").asText())) {
            result.setFound(true);
            result.setObjectNode(node.deepCopy());
            if (parentNode != null) {
                result.setParentId(parentNode.get("id").asText());
                result.setIndex(currentIndex);
            }
            result.setId(targetId);
            return;
        }

        // 递归检查children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                getNodePositionByIdCore(children.get(i), node, i, targetName, result);
            }
        }
    }

    /**
     * 递归ObjectNode node 和children下节点，将所有节点的id属性 重新生成(使用:generateShortUUID())
     *
     * @param node
     */
    public static void reChangeNodeId(ObjectNode node){
        if (node == null) {
            return;
        }
        node.put("id", generateShortUUID());
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (JsonNode child : children) {
                reChangeNodeId((ObjectNode) child);
            }
        }

    }


    /**
     * 这个方法时用来对比node是否数据相等.
     * 因此,需要将node转换为字符串, 并去除换行符和空格, 然后进行比较.且所有节点(含子节点)的属性名称按照从小到大排序, 顺序需要一致.
     *
     * @param node 需要标准化的JsonNode
     * @return 标准化后的JSON字符串
     * @throws Exception 如果JSON处理过程中发生错误
     */
    public static String normalizeJson(JsonNode node) throws Exception {
        if (node == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();

        if (node.isObject()) {
            // 获取所有字段名并排序
            List<String> fieldNames = new ArrayList<>();
            node.fieldNames().forEachRemaining(fieldNames::add);
            Collections.sort(fieldNames);

            // 创建一个新的有序的ObjectNode
            ObjectNode normalizedNode = objectMapper.createObjectNode();
            
            // 按照排序后的字段名重新构建对象
            for (String fieldName : fieldNames) {
                JsonNode value = node.get(fieldName);
                if (value.isObject() || value.isArray()) {
                    // 递归处理嵌套的对象和数组
                    normalizedNode.set(fieldName, objectMapper.readTree(normalizeJson(value)));
                } else if (value.isNumber()) {
                    // 保持数值类型
                    if (value.isInt()) {
                        normalizedNode.put(fieldName, value.asInt());
                    } else if (value.isLong()) {
                        normalizedNode.put(fieldName, value.asLong());
                    } else if (value.isDouble()) {
                        normalizedNode.put(fieldName, value.asDouble());
                    } else {
                        // 将通用 Number 类型转换为 double
                        normalizedNode.put(fieldName, value.numberValue().doubleValue());
                    }
                } else {
                    normalizedNode.set(fieldName, value);
                }
            }
            return objectMapper.writeValueAsString(normalizedNode);
        } else if (node.isArray()) {
            ArrayNode normalizedArray = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                if (element.isObject() || element.isArray()) {
                    normalizedArray.add(objectMapper.readTree(normalizeJson(element)));
                } else if (element.isNumber()) {
                    // 保持数组中数值的类型
                    if (element.isInt()) {
                        normalizedArray.add(element.asInt());
                    } else if (element.isLong()) {
                        normalizedArray.add(element.asLong());
                    } else if (element.isDouble()) {
                        normalizedArray.add(element.asDouble());
                    } else {
                        // 将通用 Number 类型转换为 double
                        normalizedArray.add(element.numberValue().doubleValue());
                    }
                } else {
                    normalizedArray.add(element);
                }
            }
            return objectMapper.writeValueAsString(normalizedArray);
        } else {
            // 对于基本类型，直接返回字符串表示
            return objectMapper.writeValueAsString(node);
        }
    }

    /**
     * 创建并插入一个container节点，内部包含一个label显示标题内容
     * @param node 要插入container的节点
     * @param titleName 标题内容
     */
    public static void insertContainerNode(ObjectNode node, String titleName) {
        if (node == null || titleName == null) {
            return;
        }
        
        // 创建Container节点
        ObjectNode containerNode = JsonNodeFactory.instance.objectNode();
        containerNode.put("id", generateShortUUID());
        containerNode.put("type", "container");

        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        settingsNode.put("alignItems", "center");
        settingsNode.put("justifyContent","center");
        containerNode.set("settings", settingsNode);
        // // 创建sourceSchema
        // ObjectNode sourceSchema = JsonNodeFactory.instance.objectNode();
        // sourceSchema.put("id", "udcContainer_" + generateShortUUID().substring(0, 4));
        // containerNode.set("sourceSchema", sourceSchema);
        
        // 创建children数组
        ArrayNode children = JsonNodeFactory.instance.arrayNode();
        
        // 创建label节点
        ObjectNode labelNode = JsonNodeFactory.instance.objectNode();
        labelNode.put("id", generateShortUUID());
        labelNode.put("type", "label");
        labelNode.put("name", "表头");
        
        // 创建label设置
        ObjectNode labelSettings = JsonNodeFactory.instance.objectNode();
        labelSettings.put("content", titleName);
        labelSettings.put("textFontSize", 20); // 设置较大字体
        labelSettings.put("alignment", "center"); // 居中对齐
        labelNode.set("settings", labelSettings);
        
        // 添加label到container的children
        children.add(labelNode);
        containerNode.set("children", children);
        
        // 添加container到根节点的第一个位置
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode nodeChildren = (ArrayNode) node.get("children");
            nodeChildren.insert(0, containerNode);
        } else {
            ArrayNode nodeChildren = JsonNodeFactory.instance.arrayNode();
            nodeChildren.add(containerNode);
            node.set("children", nodeChildren);
        }
    }

    /**
     * 拷贝属性
     */
    public static void insertNodeProps(JsonNode sourceNode, ObjectNode targetNode, List<String> copyProps) {
        if (sourceNode == null) {
            return;
        }

        for (String copyProp : copyProps) {
            if (sourceNode.has(copyProp)) {
                JsonNode copyPropNode = sourceNode.get(copyProp);
                if (copyPropNode != null) {


                    JsonNode copyPropDeepCopy = copyPropNode.deepCopy();
                    //修复gridTemplateRows
                    if("gridTemplateRows".equalsIgnoreCase(copyProp) && sourceNode.has("rows") ){
                        ArrayNode gridTemplateRowsNode = (ArrayNode)sourceNode.get("gridTemplateRows");
                        //修复 settings.rows 和 gridTemplateColumns 数量不一致问题

                        JsonNode rowsNode = sourceNode.get("rows");
                        if(rowsNode != null && gridTemplateRowsNode != null){
                            int lostNum =  rowsNode.asInt() - gridTemplateRowsNode.size();
                            for (int i = 0; i < lostNum; i++) {
                                ((ArrayNode)copyPropDeepCopy).add(28);
                            }
                        }

                    }
                    targetNode.set(copyProp, copyPropDeepCopy);
                }
            }
        }
    }

    /**
     * 拷贝复杂属性
     * @param sourceNode
     * @param targetNode
     * @param copyProps
     */
    public static void insertComplexNodeProps(JsonNode sourceNode, ObjectNode targetNode, List<String> copyProps) {
        if (sourceNode == null) {
            return;
        }

        for (String copyProp : copyProps) {
            if (sourceNode.has(copyProp)) {
                JsonNode copyPropNode = sourceNode.get(copyProp);
                if (copyPropNode != null) {
                    JsonNode typeNode = copyPropNode.get("type");
                    JsonNode simpleNode = copyPropNode.get("simple");
                    if(simpleNode != null && typeNode != null && "Simple".equals(typeNode.asText())){
                        targetNode.set(copyProp, simpleNode.deepCopy());
                    }
                    
                }

                
            }
        }
    }

    public static boolean checkType(JsonNode node,String type){

        if (type != null && node.has("type")) {
            String nodeType = node.get("type").asText();
            if(type.equalsIgnoreCase(nodeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断类型是否为 label 或 fieldtitle（忽略大小写）
     */
    public static boolean isLabelOrFieldTitle(String type) {
        return "label".equalsIgnoreCase(type) || "fieldtitle".equalsIgnoreCase(type);
    }

}
