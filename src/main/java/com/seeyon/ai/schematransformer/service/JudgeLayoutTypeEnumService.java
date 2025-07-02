package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.constant.ComponentTypeContants;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 根据groupId判断布局类型的服务类
 * 
 * <p>该服务类用于根据指定的groupId判断DSL模板中的布局类型，支持以下四种布局类型：</p>
 * 
 * <h3>布局类型说明：</h3>
 * <ul>
 *   <li><strong>COMPLEX</strong> - 左右布局：title和component在同一行，title在左，component在右</li>
 *   <li><strong>TWO_CELL_UP_DOWN</strong> - 上下布局(不同单元格)：title和component在不同行，title在上，component在下</li>
 *   <li><strong>ONE_CELL_UP_DOWN</strong> - 上下布局(同一单元格)：title和component在同一单元格内，title在上，component在下</li>
 *   <li><strong>SIMPLE</strong> - 简单布局：只有一个component，没有对应的title</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取grid结构的DSL节点
 * JsonNode mainGridTemplateNode = getGridTemplateNode();
 * 
 * // 根据groupId判断布局类型
 * String groupId = "3759629784606892007";
 * LayoutTypeEnum layoutType = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(
 *     mainGridTemplateNode, groupId);
 * 
 * // 根据布局类型进行相应处理
 * switch (layoutType) {
 *     case COMPLEX:
 *         // 处理左右布局
 *         break;
 *     case TWO_CELL_UP_DOWN:
 *         // 处理上下布局(不同单元格)
 *         break;
 *     case ONE_CELL_UP_DOWN:
 *         // 处理上下布局(同一单元格)
 *         break;
 *     case SIMPLE:
 *         // 处理简单布局
 *         break;
 * }
 * }</pre>
 * 
 * <h3>DSL结构要求：</h3>
 * <p>输入的DSL节点必须符合以下结构：</p>
 * <pre>{@code
 * {
 *   "type": "grid",
 *   "children": [
 *     {
 *       "type": "gridCell",
 *       "settings": {
 *         "styles": {
 *           "gridColumn": "1/2",
 *           "gridRow": "2/3"
 *         }
 *       },
 *       "children": [
 *         {
 *           "type": "label",
 *           "referGroup": {
 *             "groupId": "3759629784606892007",
 *             "groupName": "文件标题",
 *             "groupType": "title"
 *           }
 *         }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 * 
 * @author AI Assistant
 * @since 1.0
 */
@Slf4j
public class JudgeLayoutTypeEnumService {

    /**
     * 获取第一个业务组件(绑定数据)
     * 
     * @param mainGridTemplateNode
     * @param groupId
     * @return
     */

     public static JsonNode getFirstBuzNode(JsonNode mainGridTemplateNode){
        String  mainGridTemplateNodeType = mainGridTemplateNode.get("type").asText();

        Set<String> allComponentTypeSet = new HashSet<>(Arrays.asList(ComponentTypeContants.ALL_COMPONENT_TYPE.split(",")));
        if("grid".equalsIgnoreCase(mainGridTemplateNodeType)){
            JsonNode gridChildrenNodes = mainGridTemplateNode.get("children");
    
            //TODO 将gridChildrenNodes进行排序, 保证规则是按照顺序组件 判断的.

            if(gridChildrenNodes != null && gridChildrenNodes.isArray()){
                for(JsonNode gridCellNode : gridChildrenNodes){

                    String cellComponentType = gridCellNode.get("type").asText();
                  
                    if("gridCell".equalsIgnoreCase(cellComponentType)){
                        JsonNode cellChildNode = gridCellNode.get("children");
                        if(cellChildNode != null && cellChildNode.isArray()){
                            for(JsonNode cellChildNodeItem : cellChildNode){
                                String cellChildNodeType = cellChildNodeItem.get("type").asText();
                                JsonNode dataSourceNode = cellChildNodeItem.get("dataSource");
                                if(allComponentTypeSet.contains(cellChildNodeType) && !"uiBusinessEdocOpinionBox".equalsIgnoreCase(cellChildNodeType) && dataSourceNode != null && dataSourceNode.has("dataField")){
                                    return cellChildNodeItem;
                                }
                            }
                        }
                    }


                } 
                
            }
        }
        return null;
     }

     /**
     * 获取第一个意见组件（递归遍历所有子节点）
     * 
     * @param node
     * @return
     */
    public static JsonNode getFirstOpinionBoxNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        // 判断当前节点
        if (node.has("type") && "uiBusinessEdocOpinionBox".equalsIgnoreCase(node.get("type").asText())) {
            return node;
        }
        // 递归遍历children
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                JsonNode result = getFirstOpinionBoxNode(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }


    /**
     * 获取第一个意见组件（递归遍历所有子节点）
     *
     * @param node
     * @return
     */
    public static JsonNode getFirstNodeById(JsonNode node,String id) {
        if (node == null) {
            return null;
        }
        // 判断当前节点
        if (node.has("type") && id.equals(node.get("id").asText())) {
            return node;
        }
        // 递归遍历children
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                JsonNode result = getFirstNodeById(child,id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }



    /**
     * 根据groupId判断布局类型
     * @param mainGridTemplateNode grid结构的dsl
     * @param groupId referGroup -> groupId
     * @return 布局类型枚举
     */
    public static LayoutTypeEnum getLayoutTypeEnumByGroupId(JsonNode mainGridTemplateNode, String groupId) {
        if (mainGridTemplateNode == null || groupId == null) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 检查是否为grid类型
        if (!mainGridTemplateNode.has("type") || !"grid".equals(mainGridTemplateNode.get("type").asText())) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 收集所有包含指定groupId的节点
        List<GroupNodeInfo> groupNodes = collectGroupNodes(mainGridTemplateNode, groupId);
        
        if (groupNodes.isEmpty()) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 判断布局类型
        if (isOneCellUpDownLayout(groupNodes)) {
            return LayoutTypeEnum.ONE_CELL_UP_DOWN;
        } else if (isTwoCellUpDownLayout(groupNodes)) {
            return LayoutTypeEnum.TWO_CELL_UP_DOWN;
        } else if (isComplexLayout(groupNodes)) {
            return LayoutTypeEnum.COMPLEX;
        } else {
            return LayoutTypeEnum.SIMPLE;
        }
    }

    /**
     * 根据id判断布局类型（不存在groupId时使用）
     * @param mainGridTemplateNode grid结构的dsl
     * @param id 节点的id
     * @return 布局类型枚举
     */
    public static LayoutTypeEnum getLayoutTypeEnumById(JsonNode mainGridTemplateNode, String id) {
        if (mainGridTemplateNode == null || id == null) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 检查是否为grid类型
        if (!mainGridTemplateNode.has("type") || !"grid".equals(mainGridTemplateNode.get("type").asText())) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 查找指定id的节点
        NodeInfo targetNode = findNodeById(mainGridTemplateNode, id);
        if (targetNode == null) {
            return LayoutTypeEnum.SIMPLE;
        }

        // 判断布局类型
        if (isComplexLayoutById(targetNode, mainGridTemplateNode)) {
            return LayoutTypeEnum.COMPLEX;
        } else if (isTwoCellUpDownLayoutById(targetNode.getParentGridCell() , mainGridTemplateNode)) {
            return LayoutTypeEnum.TWO_CELL_UP_DOWN;
        } else if (isOneCellUpDownLayoutByGridCell(targetNode.getParentGridCell() )) {
            return LayoutTypeEnum.ONE_CELL_UP_DOWN;
        } else {
            return LayoutTypeEnum.SIMPLE;
        }
    }

    /**
     * 查找指定id的节点信息（递归查找）
     */
    private static NodeInfo findNodeById(JsonNode node, String id) {
        return findNodeById(node, id, null);
    }

    private static NodeInfo findNodeById(JsonNode node, String id, JsonNode parentGridCell) {
        if (node == null) {
            return null;
        }
        // 如果当前节点是gridCell，递归其children，并把自己作为parentGridCell传下去
        if (node.has("type") && "gridCell".equals(node.get("type").asText())) {
            if (node.has("children") && node.get("children").isArray()) {
                for (JsonNode child : node.get("children")) {
                    NodeInfo result = findNodeById(child, id, node);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } else {
            // 检查当前节点
            if (node.has("id") && id.equals(node.get("id").asText())) {
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.setNode(node);
                nodeInfo.setParentGridCell(parentGridCell);
                return nodeInfo;
            }
            // 递归children
            if (node.has("children") && node.get("children").isArray()) {
                for (JsonNode child : node.get("children")) {
                    NodeInfo result = findNodeById(child, id, parentGridCell);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据id判断是否为COMPLEX布局
     * 规则：
     * 1. "id"的父节点是单元格(type=gridCell)，且存在"左侧的单元格"(即：gridRow相等，gridColumn的开始值相差1)
     * 2. "左侧的单元格"的children节点中包含1个"type"="label"的节点
     */
    private static boolean isComplexLayoutById(NodeInfo targetNode, JsonNode mainGridTemplateNode) {
        JsonNode parentGridCell = targetNode.getParentGridCell();
        
        // 检查父节点是否为gridCell
        if (!"gridCell".equals(parentGridCell.get("type").asText())) {
            return false;
        }

        // 获取当前单元格的gridRow和gridColumn
        String currentGridRow = getGridRow(parentGridCell);
        String currentGridColumn = getGridColumn(parentGridCell);
        
        if (currentGridRow == null || currentGridColumn == null) {
            return false;
        }

        // 查找左侧的单元格（gridRow相等，gridColumn的开始值相差1）
        JsonNode leftGridCell = findLeftGridCell(mainGridTemplateNode, currentGridRow, currentGridColumn);
        if (leftGridCell == null) {
            return false;
        }

        // 检查左侧单元格是否包含label节点
        return containsLabelNode(leftGridCell);
    }

    /**
     * 根据id判断是否为TWO_CELL_UP_DOWN布局
     * 规则：
     * 1. "id"的父节点是单元格(type=gridCell)，且存在"上方的单元格"(即：gridColumn相等，gridRow的开始值相差1)
     * 2. "上方的单元格"的children节点中包含1个"type"="label"的节点
     */
    private static boolean isTwoCellUpDownLayoutById( JsonNode parentGridCell, JsonNode mainGridTemplateNode) {

        
        // 检查父节点是否为gridCell
        if (!"gridCell".equals(parentGridCell.get("type").asText())) {
            return false;
        }

        // 获取当前单元格的gridRow和gridColumn
        String currentGridRow = getGridRow(parentGridCell);
        String currentGridColumn = getGridColumn(parentGridCell);
        
        if (currentGridRow == null || currentGridColumn == null) {
            return false;
        }

        // 查找上方的单元格（gridColumn相等，gridRow的开始值相差1）
        JsonNode upperGridCell = findUpperGridCell(mainGridTemplateNode, currentGridRow, currentGridColumn);
        if (upperGridCell == null) {
            return false;
        }

        // 检查上方单元格是否包含label节点
        return containsLabelNode(upperGridCell);
    }

    /**
     * 判断gridCell内是否存在container，且container内uiBusinessEdocOpinionBox节点上方有label节点
     */
    private static boolean isOneCellUpDownLayoutByGridCell(JsonNode gridCellNode) {
        if (gridCellNode == null || !gridCellNode.has("children") || !gridCellNode.get("children").isArray()) {
            return false;
        }
        ArrayNode children = (ArrayNode) gridCellNode.get("children");
        for (JsonNode child : children) {
            if (child.has("type") && "container".equals(child.get("type").asText())) {
                ArrayNode containerChildren = (ArrayNode) child.get("children");
                for (int i = 0; i < containerChildren.size(); i++) {
                    JsonNode node = containerChildren.get(i);
                    if (node.has("type") && "uiBusinessEdocOpinionBox".equals(node.get("type").asText())) {
                        // 检查前一个兄弟节点是否为label
                        if (i > 0) {
                            JsonNode prevNode = containerChildren.get(i - 1);
                            if (prevNode.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(prevNode.get("type").asText())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 查找左侧的gridCell
     * 规则：gridRow相等，gridColumn的开始值相差1
     * 例如："gridColumn": "1/2"在 "2/3"左侧
     */
    private static JsonNode findLeftGridCell(JsonNode gridNode, String currentGridRow, String currentGridColumn) {
        if (!gridNode.has("children") || !gridNode.get("children").isArray()) {
            return null;
        }

        try {
            int currentColumnStart = Integer.parseInt(currentGridColumn.split("/")[0]);
            
            ArrayNode children = (ArrayNode) gridNode.get("children");
            for (JsonNode child : children) {
                if (child.has("type") && "gridCell".equals(child.get("type").asText())) {
                    String gridRow = getGridRow(child);
                    String gridColumn = getGridColumn(child);
                    
                    if (currentGridRow.equals(gridRow) && gridColumn != null) {
                        int columnStart = Integer.parseInt(gridColumn.split("/")[0]);
                        // 左侧单元格的columnStart应该比当前单元格的columnStart小1
                        if (columnStart == currentColumnStart - 1) {
                            return child;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析gridColumn失败: currentGridColumn={}", currentGridColumn, e);
        }
        
        return null;
    }

    /**
     * 查找上方的gridCell
     * 规则：gridColumn相等，gridRow的开始值相差1
     * 例如："gridRow": "2/3"在"gridRow": "3/4"上方
     */
    private static JsonNode findUpperGridCell(JsonNode gridNode, String currentGridRow, String currentGridColumn) {
        if (!gridNode.has("children") || !gridNode.get("children").isArray()) {
            return null;
        }

        try {
            int currentRowStart = Integer.parseInt(currentGridRow.split("/")[0]);
            
            ArrayNode children = (ArrayNode) gridNode.get("children");
            for (JsonNode child : children) {
                if (child.has("type") && "gridCell".equals(child.get("type").asText())) {
                    String gridRow = getGridRow(child);
                    String gridColumn = getGridColumn(child);
                    
                    if (currentGridColumn.equals(gridColumn) && gridRow != null) {
                        int rowStart = Integer.parseInt(gridRow.split("/")[0]);
                        // 上方单元格的rowStart应该比当前单元格的rowStart小1
                        if (rowStart == currentRowStart - 1) {
                            return child;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析gridRow失败: currentGridRow={}", currentGridRow, e);
        }
        
        return null;
    }

    /**
     * 检查gridCell是否包含label节点
     */
    private static boolean containsLabelNode(JsonNode gridCellNode) {
        if (!gridCellNode.has("children") || !gridCellNode.get("children").isArray()) {
            return false;
        }

        ArrayNode children = (ArrayNode) gridCellNode.get("children");
        for (JsonNode child : children) {
            if (child.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(child.get("type").asText())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 收集所有包含指定groupId的节点信息
     */
    private static List<GroupNodeInfo> collectGroupNodes(JsonNode gridNode, String groupId) {
        List<GroupNodeInfo> groupNodes = new ArrayList<>();
        
        if (!gridNode.has("children") || !gridNode.get("children").isArray()) {
            return groupNodes;
        }

        ArrayNode children = (ArrayNode) gridNode.get("children");
        for (JsonNode child : children) {
            if (child.has("type") && "gridCell".equals(child.get("type").asText())) {
                collectGroupNodesFromGridCell(child, groupId, groupNodes);
            }
        }
        
        return groupNodes;
    }

    /**
     * 从gridCell中收集包含指定groupId的节点
     */
    private static void collectGroupNodesFromGridCell(JsonNode gridCellNode, String groupId, List<GroupNodeInfo> groupNodes) {
        if (!gridCellNode.has("children") || !gridCellNode.get("children").isArray()) {
            return;
        }

        ArrayNode children = (ArrayNode) gridCellNode.get("children");
        for (JsonNode child : children) {
            if (child.has("referGroup")) {
                JsonNode referGroup = child.get("referGroup");
                if (referGroup.has("groupId") && groupId.equals(referGroup.get("groupId").asText())) {
                    GroupNodeInfo nodeInfo = new GroupNodeInfo();
                    nodeInfo.setNode(child);
                    nodeInfo.setParentGridCell(gridCellNode);
                    nodeInfo.setGroupType(referGroup.has("groupType") ? referGroup.get("groupType").asText() : "");
                    groupNodes.add(nodeInfo);
                }
            }
        }
    }

    /**
     * 判断是否为ONE_CELL_UP_DOWN布局
     * 规则：满足groupId的父节点的children中包含type="container"和groupId节点，且type="container"中包含一个label节点
     */
    private static boolean isOneCellUpDownLayout(List<GroupNodeInfo> groupNodes) {
        if (groupNodes.size() != 1) {
            return false;
        }

        GroupNodeInfo nodeInfo = groupNodes.get(0);
        JsonNode parentGridCell = nodeInfo.getParentGridCell();
        
        if (!parentGridCell.has("children") || !parentGridCell.get("children").isArray()) {
            return false;
        }

        ArrayNode children = (ArrayNode) parentGridCell.get("children");
        boolean hasContainer = false;
        boolean hasGroupIdNode = false;
        boolean containerHasLabel = false;

        for (JsonNode child : children) {
            if (child.has("type")) {
                String type = child.get("type").asText();
                if ("container".equals(type)) {
                    hasContainer = true;
                    // 检查container中是否包含label节点
                    if (child.has("children") && child.get("children").isArray()) {
                        ArrayNode containerChildren = (ArrayNode) child.get("children");
                        for (JsonNode containerChild : containerChildren) {
                            if (containerChild.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(containerChild.get("type").asText()) && "fieldTitle".equals(containerChild.get("type").asText()) ) {
                                containerHasLabel = true;
                                break;
                            }
                        }
                    }
                } else if (child.has("referGroup")) {
                    JsonNode referGroup = child.get("referGroup");
                    if (referGroup.has("groupId") && referGroup.has("groupType") && 
                        "component".equals(referGroup.get("groupType").asText())) {
                        hasGroupIdNode = true;
                    }
                }
            }
        }

        return hasContainer && hasGroupIdNode && containerHasLabel;
    }

    /**
     * 判断是否为TWO_CELL_UP_DOWN布局
     * 规则：
     * 1. 满足groupId的节点集合数量 >= 2
     * 2. 满足groupId的节点集合中包含"groupType"="title"和"component"
     * 3. 满足groupId的节点的父节点type都是gridCell
     * 4. "groupType"="title"和"component"的父节点的settings->styles->gridColumn相等
     * 5. "groupType"="title"父节点的settings->styles->gridRow要小于"groupType"="component"父节点的settings->styles->gridRow
     */
    private static boolean isTwoCellUpDownLayout(List<GroupNodeInfo> groupNodes) {
        if (groupNodes.size() < 2) {
            return false;
        }

        // 检查是否包含title和component
        boolean hasTitle = false;
        boolean hasComponent = false;
        GroupNodeInfo titleNode = null;
        GroupNodeInfo componentNode = null;

        for (GroupNodeInfo nodeInfo : groupNodes) {
            if ("title".equals(nodeInfo.getGroupType())) {
                hasTitle = true;
                titleNode = nodeInfo;
            } else if ("component".equals(nodeInfo.getGroupType())) {
                hasComponent = true;
                componentNode = nodeInfo;
            }
        }

        if (!hasTitle || !hasComponent || titleNode == null || componentNode == null) {
            return false;
        }

        // 检查父节点是否都是gridCell
        if (!"gridCell".equals(titleNode.getParentGridCell().get("type").asText()) ||
            !"gridCell".equals(componentNode.getParentGridCell().get("type").asText())) {
            return false;
        }

        // 检查gridColumn是否相等
        String titleGridColumn = getGridColumn(titleNode.getParentGridCell());
        String componentGridColumn = getGridColumn(componentNode.getParentGridCell());
        
        if (titleGridColumn == null || componentGridColumn == null || 
            !titleGridColumn.equals(componentGridColumn)) {
            return false;
        }

        // 检查gridRow，title的gridRow要小于component的gridRow
        String titleGridRow = getGridRow(titleNode.getParentGridCell());
        String componentGridRow = getGridRow(componentNode.getParentGridCell());
        
        if (titleGridRow == null || componentGridRow == null) {
            return false;
        }

        try {
            int titleRow = Integer.parseInt(titleGridRow.split("/")[0]);
            int componentRow = Integer.parseInt(componentGridRow.split("/")[0]);
            return titleRow < componentRow;
        } catch (Exception e) {
            log.warn("解析gridRow失败: titleRow={}, componentRow={}", titleGridRow, componentGridRow, e);
            return false;
        }
    }

    /**
     * 判断是否为COMPLEX布局
     * 规则：
     * 1. 满足groupId的节点集合数量 >= 2
     * 2. 满足groupId的节点集合中包含"groupType"="title"和"component"
     * 3. 满足groupId的节点的父节点type都是gridCell
     * 4. "groupType"="title"和"component"的父节点的settings->styles->gridRow相等
     * 5. "groupType"="title"父节点的settings->styles->gridColumn要小于"groupType"="component"父节点的settings->styles->gridColumn
     */
    private static boolean isComplexLayout(List<GroupNodeInfo> groupNodes) {
        if (groupNodes.size() < 2) {
            return false;
        }

        // 检查是否包含title和component
        boolean hasTitle = false;
        boolean hasComponent = false;
        GroupNodeInfo titleNode = null;
        GroupNodeInfo componentNode = null;

        for (GroupNodeInfo nodeInfo : groupNodes) {
            if ("title".equals(nodeInfo.getGroupType())) {
                hasTitle = true;
                titleNode = nodeInfo;
            } else if ("component".equals(nodeInfo.getGroupType())) {
                hasComponent = true;
                componentNode = nodeInfo;
            }
        }

        if (!hasTitle || !hasComponent || titleNode == null || componentNode == null) {
            return false;
        }

        // 检查父节点是否都是gridCell
        if (!"gridCell".equals(titleNode.getParentGridCell().get("type").asText()) ||
            !"gridCell".equals(componentNode.getParentGridCell().get("type").asText())) {
            return false;
        }

        // 检查gridRow是否相等
        String titleGridRow = getGridRow(titleNode.getParentGridCell());
        String componentGridRow = getGridRow(componentNode.getParentGridCell());
        
        if (titleGridRow == null || componentGridRow == null || 
            !titleGridRow.equals(componentGridRow)) {
            return false;
        }

        // 检查gridColumn，title的gridColumn要小于component的gridColumn
        String titleGridColumn = getGridColumn(titleNode.getParentGridCell());
        String componentGridColumn = getGridColumn(componentNode.getParentGridCell());
        
        if (titleGridColumn == null || componentGridColumn == null) {
            return false;
        }

        try {
            int titleColumn = Integer.parseInt(titleGridColumn.split("/")[0]);
            int componentColumn = Integer.parseInt(componentGridColumn.split("/")[0]);
            return titleColumn < componentColumn;
        } catch (Exception e) {
            log.warn("解析gridColumn失败: titleColumn={}, componentColumn={}", titleGridColumn, componentGridColumn, e);
            return false;
        }
    }

    /**
     * 获取gridColumn值
     */
    private static String getGridColumn(JsonNode gridCellNode) {
        if (gridCellNode.has("settings") && 
            gridCellNode.get("settings").has("styles") && 
            gridCellNode.get("settings").get("styles").has("gridColumn")) {
            return gridCellNode.get("settings").get("styles").get("gridColumn").asText();
        }
        return null;
    }

    /**
     * 获取gridRow值
     */
    private static String getGridRow(JsonNode gridCellNode) {
        if (gridCellNode.has("settings") && 
            gridCellNode.get("settings").has("styles") && 
            gridCellNode.get("settings").get("styles").has("gridRow")) {
            return gridCellNode.get("settings").get("styles").get("gridRow").asText();
        }
        return null;
    }

    /**
     * 组节点信息内部类
     */
    private static class GroupNodeInfo {
        private JsonNode node;
        private JsonNode parentGridCell;
        private String groupType;

        public JsonNode getNode() {
            return node;
        }

        public void setNode(JsonNode node) {
            this.node = node;
        }

        public JsonNode getParentGridCell() {
            return parentGridCell;
        }

        public void setParentGridCell(JsonNode parentGridCell) {
            this.parentGridCell = parentGridCell;
        }

        public String getGroupType() {
            return groupType;
        }

        public void setGroupType(String groupType) {
            this.groupType = groupType;
        }
    }

    /**
     * 节点信息内部类（用于id判断）
     */
    private static class NodeInfo {
        private JsonNode node;
        private JsonNode parentGridCell;

        public JsonNode getNode() {
            return node;
        }

        public void setNode(JsonNode node) {
            this.node = node;
        }

        public JsonNode getParentGridCell() {
            return parentGridCell;
        }

        public void setParentGridCell(JsonNode parentGridCell) {
            this.parentGridCell = parentGridCell;
        }
    }
}
