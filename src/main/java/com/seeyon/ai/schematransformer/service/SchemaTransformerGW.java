package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.dto.Pair;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import com.seeyon.ai.schematransformer.dto.SchemaTransformGridTemplateDTO;
import com.seeyon.ai.schematransformer.dto.TemplateTagType;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import io.swagger.v3.core.util.Json;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将udcSchema -> dslSchema
 */
@Slf4j
public class SchemaTransformerGW {

    /**
     * udc模版和ocr图片 转换为dslSchema 入口方法
     */
    public static JsonNode convertLayoutByTemplate(JsonNode layoutSchema, JsonNode template,boolean isUseOcrLayout) {


        ObjectNode templateDslSchema = SchemaTransformer.convertUdc2Dsl(template);
        //生成结果以模版中、最大字体；如果没有最大字体，在顶部增加一个标准默认标题容器（程萌提供规范）；
        SchemaTransformerBase.initTitleNodeName(templateDslSchema);
        //ocr网格缺失修复
        JsonNode groupsNode = layoutSchema.get("groups");
        if (groupsNode != null && groupsNode.isArray()) {
            for (JsonNode groupNode : groupsNode) {
                SchemaTransformerJsonUtil.fixComponents((ObjectNode) groupNode);
            }
        }

        return convertLayoutByDslSchema(layoutSchema, templateDslSchema, isUseOcrLayout);
    }

    /**
     * udc模版和ocr图片 转换为dslSchema 入口方法
     */
    public static JsonNode convertLayoutByDslSchema(JsonNode layoutSchema, ObjectNode templateDslSchema,boolean isUseOcrLayout) {

        //获取标题名称
        String titleName = "";
        JsonNode titleJsonNode = layoutSchema.get("titleName");
        if (titleJsonNode != null) {
            titleName = titleJsonNode.asText();
        }

        //获取实体名称
        String entityName = "";
        JsonNode dataSourceJsonNode = layoutSchema.get("dataSource");
        if (dataSourceJsonNode != null) {
            JsonNode entityNameNode = dataSourceJsonNode.get("entityName");
            entityName = entityNameNode.asText();

        }

        //获取布局套用模版后result结合信息. 包括: 被替换node坐标,替换的node节点
        List<NodePosition> schemaTransformResultList = convertLayoutByDslTemplate(layoutSchema, templateDslSchema, isUseOcrLayout);

        //生成最终文件.
        JsonNode resultDslSchema = rendering(templateDslSchema, titleName, entityName, schemaTransformResultList);
        return resultDslSchema;
    }

    /**
     * 将 文单标题, 实体名称, 主网格 信息替换掉模版中的数据. 并删除无用的节点.(标签: 删除).
     *
     * @return
     */
    public static JsonNode rendering(ObjectNode templateDslSchemaParamNode, String titleName, String entityName, List<NodePosition> schemaTransformPositionList) {
        ObjectNode templateDslSchema = templateDslSchemaParamNode.deepCopy();
        //删除标记为"删除"的节点
        SchemaTransformerUtil.deleteNodeByName(templateDslSchema, TemplateTagType.DELETE.getValue());


        //替换标题
        Boolean updateTitleSuccess = SchemaTransformerBase.updateTitleNode(templateDslSchema, titleName);
        //如果替换失败, 则插入一个container里面有一个label, 内容为titleName
        if (!updateTitleSuccess) {
            SchemaTransformerUtil.insertContainerNode(templateDslSchema, titleName);
        }
        //替换实体名称
        SchemaTransformerBase.updateFormNode(templateDslSchema, entityName);

        //替换主网格
        if (schemaTransformPositionList != null) {
            ArrayNode layoutList = JsonNodeFactory.instance.arrayNode();
            NodePosition layoutReplacePosition = null;
            for (NodePosition schemaTransformPosition : schemaTransformPositionList) {
                layoutList.add(schemaTransformPosition.getObjectNode());
                if (layoutReplacePosition == null) {
                    layoutReplacePosition = schemaTransformPosition;
                }

            }
            // 如果layoutList不为空,处理dslSchema
            if (!layoutList.isEmpty()) {

                if (layoutReplacePosition != null) {
                    SchemaTransformerUtil.deleteNodePosition(templateDslSchema, layoutReplacePosition);
                }
                //删除所有type=dataGrid的节点
                SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "collapse");
                SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "dataGrid");
                SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "dataGridView");

                templateDslSchema = (ObjectNode) SchemaTransformerUtil.insertLayoutListAtCollapsePosition(templateDslSchema, layoutReplacePosition, layoutList);

            }
        }

        return templateDslSchema;
    }

    /**
     * 获取gridName 下所有网格属性.
     * 如果不存在gridName标签. 则按照 产品给出规则 查询.
     * 1. 从外向里找. 第一个顶部和底部都不是透明的网格, 作为主网格.(以及,对比主网格 同级别顶部和底部都不是透明的网格的网格,则取业务属性组件最多的网格 )
     * 2. 如果没有找到 , 则取业务属性组件最多的网格.
     *
     * @param templateDslNode
     * @param gridName        默认:主网格
     * @return
     */
    public static SchemaTransformGridTemplateDTO initGridTemplateNode(JsonNode templateDslNode, String gridName) {
        SchemaTransformGridTemplateDTO schemaTransformGridTemplateDTO = new SchemaTransformGridTemplateDTO();
        //主网格 节点样式
        NodePosition mainGridPosition = SchemaTransformerUtil.getFirstNodePositionByName(templateDslNode, gridName);

        if (mainGridPosition == null || mainGridPosition.getObjectNode() == null) {
            //按照规则查找
            mainGridPosition = SchemaTransformerUtil.getMostFrequentNodePosition(templateDslNode);

            if (mainGridPosition == null || mainGridPosition.getObjectNode() == null) { //没有找到主网格
                log.info("templateDslNode: {}", templateDslNode);
                throw new RuntimeException("没有找到主网格");
            }

        }
        JsonNode mainGridTemplateNode = mainGridPosition.getObjectNode();
        if (mainGridPosition != null && mainGridTemplateNode != null) {
            // mainGridTemplateNode转换为schemaTransformGridTemplateDTO 结构
            schemaTransformGridTemplateDTO = convertGridTemplateDto(mainGridTemplateNode);

            //设置主网格所在模版位置
            schemaTransformGridTemplateDTO.setGridTemplatePosition(mainGridPosition);

        }
        return schemaTransformGridTemplateDTO;
    }

    /**
     * 拆分 mainGridTemplateNode 中的模版样式
     *
     * 
     * @return
     */
    private static SchemaTransformGridTemplateDTO convertGridTemplateDto(JsonNode mainGridTemplateNode) {
        SchemaTransformGridTemplateDTO schemaTransformGridTemplateDTO = new SchemaTransformGridTemplateDTO();

        //初始化 title, component, desc(默认) 样式
        initTitleComponentDescTemplateInfo(mainGridTemplateNode, schemaTransformGridTemplateDTO);

        //初始化 desc 样式 (产品规则:复杂情况)
        initDescTemplateInfo(mainGridTemplateNode, schemaTransformGridTemplateDTO);

        //初始化 typeMap 和 dataFieldMap
        recursionGridTemplateDTO(mainGridTemplateNode, schemaTransformGridTemplateDTO);

        return schemaTransformGridTemplateDTO;
    }

    /**
     * 初始化 title, component ,desc 样式
     *
     * @param gridNode
     * @param schemaTransformGridTemplateDTO
     */
    private static void initTitleComponentDescTemplateInfo(JsonNode gridNode, SchemaTransformGridTemplateDTO schemaTransformGridTemplateDTO) {
        JsonNode childrenNodes = gridNode.get("children");
        if (childrenNodes != null && childrenNodes.isArray()) {
            for (int i = 0; i < childrenNodes.size(); i++) {
                JsonNode currNode = childrenNodes.get(i);
                int j = i + 1;
                if (j < childrenNodes.size()) {
                    JsonNode nextNode = childrenNodes.get(j);

                    if (schemaTransformGridTemplateDTO.getGridCellTitleNode() == null || schemaTransformGridTemplateDTO.getGridCellComponentNode() == null) {
                        boolean titleAndComponentNode = isTitleAndComponentNode(currNode, nextNode);
                        if (titleAndComponentNode) {
                            //设置 title, component , titleCell, componentCell
                            JsonNode currChildrenNodes = currNode.get("children");
                            JsonNode titleNode = currChildrenNodes.get(0);

                            schemaTransformGridTemplateDTO.setGridCellTitleNode(currNode);
                            schemaTransformGridTemplateDTO.setTitleNode(titleNode);

                            JsonNode nextChildrenNodes = nextNode.get("children");
                            JsonNode componentNode = nextChildrenNodes.get(0);

                            schemaTransformGridTemplateDTO.setGridCellComponentNode(nextNode);
                            schemaTransformGridTemplateDTO.setComponentDefaultNode(componentNode);

                            //desc默认和component一样
                            schemaTransformGridTemplateDTO.setDescNode(componentNode);
                            schemaTransformGridTemplateDTO.setGridCellDescNode(nextNode);
                        }
                    }
                }
            }

            //没有找到奇偶样式不一致的网格cell,则默认取第一个
            if (schemaTransformGridTemplateDTO.getGridCellTitleNode() == null && childrenNodes.size() > 0) {
//                JsonNode firstNode = childrenNodes.get(0);

                for (JsonNode firstNode : childrenNodes) {
                    if (firstNode != null && firstNode.has("children") && firstNode.get("children").size() > 0) {
                        JsonNode firstChildNode = firstNode.get("children").get(0);

                        schemaTransformGridTemplateDTO.setGridCellTitleNode(firstNode);
                        schemaTransformGridTemplateDTO.setTitleNode(firstChildNode);
                        schemaTransformGridTemplateDTO.setGridCellComponentNode(firstNode);
                        schemaTransformGridTemplateDTO.setComponentDefaultNode(firstChildNode);
                        //desc默认和component一样
                        schemaTransformGridTemplateDTO.setGridCellDescNode(firstNode);
                        schemaTransformGridTemplateDTO.setDescNode(firstChildNode);
                        break;
                    }
                }
            }
        }

    }


    /**
     * 遍历gridNode,找出 描述类型网格的节点.
     * 要求: 1. type=gridCell
     * 2. children下只有一个节点,且节点的 type=label
     * 3. 获取所有满足,1,2条件的 gridCell节点. 倒序排序children[0]->settings -> styles-> "textFontSize" 和 "textColor" 组合出现次数.找出出现最少得
     * 如果没有满足要求的节点.返回null
     *
     * @param gridNode
     * @param schemaTransformGridTemplateDTO
     */
    private static void initDescTemplateInfo(JsonNode gridNode, SchemaTransformGridTemplateDTO schemaTransformGridTemplateDTO) {
        if (gridNode == null || !gridNode.isObject()) {
            return;
        }

        // 获取所有gridCell节点
        List<NodePosition> gridCellNodes = SchemaTransformerUtil.getAllTypeNodeInfo(gridNode, "gridCell");
        if (gridCellNodes == null || gridCellNodes.isEmpty()) {
            return;
        }

        // 用于存储满足条件的gridCell节点
        List<ObjectNode> descNodeCandidates = new ArrayList<>();

        // 遍历所有gridCell节点，筛选满足条件的节点
        for (NodePosition position : gridCellNodes) {
            ObjectNode gridCellNode = position.getObjectNode();

            // 条件1: type=gridCell (已由getAllTypeNodeInfo筛选)
            // 条件2: children下只有一个节点，且节点的type=label
            if (gridCellNode.has("children") && gridCellNode.get("children").isArray()) {
                ArrayNode children = (ArrayNode) gridCellNode.get("children");
                if (children.size() == 1) {
                    JsonNode childNode = children.get(0);
                    if (childNode.has("type") && SchemaTransformerUtil.isLabelOrFieldTitle(childNode.get("type").asText())) {
                        descNodeCandidates.add(gridCellNode);
                    }
                }
            }
        }

        if (descNodeCandidates.isEmpty()) {
            return;
        }

        // 用于统计textFontSize和textColor组合出现的次数
        Map<String, Integer> styleFrequencyMap = new HashMap<>();
        Map<String, ObjectNode> styleNodeMap = new HashMap<>();

        // 统计每个组合出现的次数
        for (ObjectNode gridCellNode : descNodeCandidates) {
            JsonNode labelNode = gridCellNode.get("children").get(0);

            if (labelNode.has("settings")) {
                JsonNode settings = labelNode.get("settings");
                String fontSize = "";
                String textColor = "";

                if (settings.has("textFontSize")) {
                    fontSize = settings.get("textFontSize").asText();
                }

                if (settings.has("textColor")) {
                    textColor = settings.get("textColor").asText();
                }

                // 组合fontsize和color作为key
                String styleKey = fontSize + "_" + textColor;

                // 更新频率统计
                styleFrequencyMap.merge(styleKey, 1, Integer::sum);

                // 记录样式与节点的对应关系
                if (!styleNodeMap.containsKey(styleKey)) {
                    styleNodeMap.put(styleKey, gridCellNode);
                }
            }
        }

        // 如果没有有效的样式组合
        if (styleFrequencyMap.isEmpty()) {
            return;
        }

        // 找出出现次数最少的样式组合
        String leastFrequentStyle = styleFrequencyMap.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (leastFrequentStyle != null) {
            ObjectNode descGridCellNode = styleNodeMap.get(leastFrequentStyle);
            JsonNode descLabelNode = descGridCellNode.get("children").get(0);

            // 设置描述节点样式
            schemaTransformGridTemplateDTO.setGridCellDescNode(descGridCellNode);
            schemaTransformGridTemplateDTO.setDescNode(descLabelNode);
        }
    }

    /**
     * 判断currNode 是否lableCell , nextNode是否componentCell
     * <p>
     * 规则:
     * 1. currNode.children[] 只包含1个节点
     * 2. currNode.children[0].type = "label"
     * 3. nextNode.children[] 只包含1个节点
     * 4. nextNode.children[0].type != "label"
     * 5. currNode.settings.styles.gridRow == nextNode.settings.styles.gridRow
     *
     * @param currCellNode
     * @param nextCellNode
     * @return 满足规则 返回true, 不满足返回false
     */
    private static boolean isTitleAndComponentNode(JsonNode currCellNode, JsonNode nextCellNode) {
        // 检查当前节点和下一节点是否为null或不是对象
        if (currCellNode == null || !currCellNode.isObject() ||
                nextCellNode == null || !nextCellNode.isObject() ||
                currCellNode.get("children") == null || nextCellNode.get("children")== null ||
                currCellNode.get("children").size() == 0 || nextCellNode.get("children").size() == 0
        ) {
            return false;
        }

        // 规则1: currNode.children[] 只包含1个节点
        if (!currCellNode.has("children") || !currCellNode.get("children").isArray() ||
                currCellNode.get("children").size() != 1) {
            return false;
        }

        // 规则2: currNode.children[0].type = "label"
        JsonNode currChildNode = currCellNode.get("children").get(0);
        if (!currChildNode.has("type") || !SchemaTransformerUtil.isLabelOrFieldTitle(currChildNode.get("type").asText())) {
            return false;
        }

        // 规则3: nextNode.children[] 只包含1个节点
        if (!nextCellNode.has("children") || !nextCellNode.get("children").isArray() ||
                nextCellNode.get("children").size() != 1) {
            return false;
        }

        // 规则4: nextNode.children[0].type != "label"
        JsonNode nextChildNode = nextCellNode.get("children").get(0);
        if (!nextChildNode.has("type") || SchemaTransformerUtil.isLabelOrFieldTitle(nextChildNode.get("type").asText())) {
            return false;
        }

        // 规则5: currNode.settings.styles.gridRow == nextNode.settings.styles.gridRow
        String currGridRow = "";
        String nextGridRow = "";

        if (currCellNode.has("settings") && currCellNode.get("settings").get("styles") != null &&
                currCellNode.get("settings").get("styles").has("gridRow")) {
            currGridRow = currCellNode.get("settings").get("styles").get("gridRow").asText();
        }

        if (nextCellNode.has("settings") && nextCellNode.get("settings").get("styles") != null &&
                nextCellNode.get("settings").get("styles").has("gridRow")) {
            nextGridRow = nextCellNode.get("settings").get("styles").get("gridRow").asText();
        }

        // 如果gridRow都为空或两者不相等，返回false
        if (currGridRow.isEmpty() || nextGridRow.isEmpty() || !currGridRow.equals(nextGridRow)) {
            return false;
        }

        // 所有规则都满足，返回true
        return true;
    }

    /**
     * 递归遍历节点并处理
     *
     * @param node                           当前节点
     * @param schemaTransformGridTemplateDTO 样式信息
     */
    private static void recursionGridTemplateDTO(JsonNode node, SchemaTransformGridTemplateDTO schemaTransformGridTemplateDTO) {
        if (node == null || !node.isObject()) {
            return;
        }
        Map<String, JsonNode> typeTemplateMap = schemaTransformGridTemplateDTO.getTypeTemplateMap();
        Map<String, JsonNode> dataFieldTemplateMap = schemaTransformGridTemplateDTO.getDataFieldTemplateMap();


        // 检查当前节点类型
        if (node.has("type")) {
            String currType = node.get("type").asText();
            if (!typeTemplateMap.keySet().contains(currType)) { //不包含
                typeTemplateMap.put(currType, node);
            }
        }

        if (node.has("dataSource") && node.get("dataSource").has("dataField")) {
            String dataFieldValue = node.get("dataSource").get("dataField").asText();
            if (!dataFieldTemplateMap.keySet().contains(dataFieldValue)) { //不包含
                typeTemplateMap.put(dataFieldValue, node);
            }
        }

        // 处理children节点
        if (node.has("children") && node.get("children").isArray()) {
            ArrayNode children = (ArrayNode) node.get("children");
            for (int i = 0; i < children.size(); i++) {
                JsonNode child = children.get(i);
                recursionGridTemplateDTO(child, schemaTransformGridTemplateDTO);
            }
        }

        //产品给的,没有找到模版中的样式时, 设置默认container样式
        JsonNode containerNode = typeTemplateMap.get("container");
        if (containerNode == null) {

            ObjectNode containerObjectNode = JsonNodeFactory.instance.objectNode();
            containerObjectNode.put("type", "container");
            containerObjectNode.putObject("id");
            containerObjectNode.put("name","");
            ObjectNode settings = containerObjectNode.putObject("settings");
            settings.put("flexWrap","wrap");
            settings.put("gap","12px 20px");
            settings.put("alignItems","center");
            settings.put("justifyContent","center");
            ObjectNode boxModelNode = settings.putObject("boxModel");
            boxModelNode.put("margin","0 0 0 0");
            boxModelNode.put("padding","8px 0px 8px 12px");
            containerObjectNode.putArray("children");
            typeTemplateMap.put("container", containerObjectNode);

        }

        //产品给的,没有找到模版中的样式时, 设置默认grid样式
        JsonNode gridNode = typeTemplateMap.get("grid");
        if (gridNode == null) {

            ObjectNode gridObjectNode = JsonNodeFactory.instance.objectNode();
            gridObjectNode.put("type", "grid");
            gridObjectNode.putObject("id");
            gridObjectNode.put("name","");
            ObjectNode settings = gridObjectNode.putObject("settings");
            settings.put("gap","0px 0px");
            ObjectNode boxModelNode = settings.putObject("boxModel");
            boxModelNode.put("margin","0 0 0 0");
            boxModelNode.put("padding","8px 12px 8px 12px");
            ArrayNode gridTemplateColumnsNodes = settings.putArray("gridTemplateColumns");
            gridTemplateColumnsNodes.add(1);
            ArrayNode gridTemplateRowsNodes = settings.putArray("gridTemplateRows");
            gridTemplateRowsNodes.add(28);
            ObjectNode gridBorderNode = settings.putObject("gridBorder");
            gridBorderNode.put("type","");
            gridBorderNode.put("borderType","solid");
            gridBorderNode.put("borderWidth","1px");
            gridBorderNode.put("borderColor","var(--error-6)");


            gridObjectNode.putArray("children");
            typeTemplateMap.put("grid", gridObjectNode);

        }
        //产品给的,没有找到模版中的样式时, 设置默认gridCell样式
        JsonNode gridCellNode = typeTemplateMap.get("gridCell");

        ObjectNode gridCellObjectNode = JsonNodeFactory.instance.objectNode();
        if (gridCellNode == null) {

            gridCellObjectNode.put("type", "gridCell");
            gridCellObjectNode.putObject("id");
            gridCellObjectNode.put("name","");
            ObjectNode settings = gridCellObjectNode.putObject("settings");

            ObjectNode stylesNode = settings.putObject("styles");
            stylesNode.put("display","flex");
            stylesNode.put("gridRow","1/2");
            stylesNode.put("gridColumn","1/2");
            stylesNode.put("borderTop","1px solid var(--error-6)");
            stylesNode.put("borderRight","1px solid var(--error-6)");
            stylesNode.put("borderBottom","1px solid var(--error-6)");
            stylesNode.put("borderLeft","1px solid var(--error-6)");



            gridCellObjectNode.putArray("children");
            typeTemplateMap.put("gridCell", gridCellObjectNode);

        }



    }


    /**
     * 将布局Schema 套用 模版DSLSchema样式, 输出 主网格group集合转换结果
     *
     * @param layoutSchema     布局Schema
     * @param gridTemplateNode 模版DSLSchema
     * @return 主网格group集合转换结果 . 目前只有一条数据, 后续预留增加页面属性
     */
    public static List<NodePosition> convertLayoutByDslTemplate(JsonNode layoutSchema, JsonNode gridTemplateNode,boolean isUseOcrLayout) {

        List<NodePosition> resultList = new ArrayList<>();

        JsonNode groups = layoutSchema.get("groups");

        if (groups != null && groups.isArray()) {
            //获取 所有渲染需要的样式
            SchemaTransformGridTemplateDTO mainGridTemplateNode = initGridTemplateNode(gridTemplateNode, "主网格");
            // 主网格 layoutTypeEnum
            LayoutTypeEnum templateLayoutTypeEnum = getTemplateLayoutType(mainGridTemplateNode);

            // 主网格 opinionBox layoutTypeEnum
            LayoutTypeEnum templateOpinionBoxLayoutTypeEnum = getTemplateOpinionBoxLayoutType(mainGridTemplateNode);
            if (templateOpinionBoxLayoutTypeEnum ==null) {
                templateOpinionBoxLayoutTypeEnum = templateLayoutTypeEnum;
            }

            // ocr layoutTypeEnum
            LayoutTypeEnum ocrLayoutTypeEnum = getOcrLayoutType(groups);

            // 主网格 opinionBox layoutTypeEnum
            LayoutTypeEnum ocrOpinionBoxLayoutTypeEnum = getOcrOpinionBoxLayoutType(groups);
            if (ocrOpinionBoxLayoutTypeEnum ==null) {
                ocrOpinionBoxLayoutTypeEnum = ocrLayoutTypeEnum;
            }


            if(!isUseOcrLayout) { //当以模版布局为主时,进行转换.
                if(ocrLayoutTypeEnum.equals(LayoutTypeEnum.SIMPLE) && templateLayoutTypeEnum.equals(LayoutTypeEnum.COMPLEX)){
                    //分离component组件为label+组件
                    unsplit2Split(layoutSchema);
                } else if(ocrLayoutTypeEnum.equals(LayoutTypeEnum.COMPLEX) && templateLayoutTypeEnum.equals(LayoutTypeEnum.SIMPLE) ){
                    //合并label+组件 为 component组件
                    mergeLabel2Component(layoutSchema,null);
                }

                //修改意见组件布局 (左侧有label就合并)
                if(templateLayoutTypeEnum.equals(LayoutTypeEnum.COMPLEX)){
                    if(templateOpinionBoxLayoutTypeEnum.equals(LayoutTypeEnum.SIMPLE) || templateOpinionBoxLayoutTypeEnum.equals(LayoutTypeEnum.ONE_CELL_UP_DOWN) ){
                        mergeLabel2Component(layoutSchema,"uiBusinessEdocOpinionBox");
                    }
                }
           
            }


            NodePosition mainGridTemplatePosition = mainGridTemplateNode.getGridTemplatePosition();

            //递归 groups, 将其内所有type=grid的node套用网格模版(gridTemplateNode) , 将其内所有type=container的node使用默认容器样式
            ArrayNode groupsDSLNode = recursionGroups((ArrayNode) groups, mainGridTemplateNode);

            for (JsonNode groupDslNode : groupsDSLNode) {

                NodePosition tempPosition = new NodePosition();
                tempPosition.setId(mainGridTemplatePosition.getId());
                tempPosition.setFound(mainGridTemplatePosition.isFound());
                tempPosition.setIndex(mainGridTemplatePosition.getIndex());
                tempPosition.setParentId(mainGridTemplatePosition.getParentId());
                tempPosition.setObjectNode((ObjectNode) groupDslNode);

                resultList.add(tempPosition);

            }


        } else {
            throw new RuntimeException("layoutSchema.groups is not array");
        }
        return resultList;
    }

    /**
     * 递归 groups, 将其内所有type=grid的node套用网格模版(gridTemplateNode) , 将其内所有type=container的node使用默认容器样式
     *
     * @param groups
     * @param gridTemplateDTO return JsonNode
     */
    public static ArrayNode recursionGroups(ArrayNode groups, SchemaTransformGridTemplateDTO gridTemplateDTO) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();

        for (JsonNode group : groups) {
            if (group == null || !group.has("type")) {
                String id = "";
                if (group.has("id")) {
                    id = group.get("id").asText();
                }
                log.error("id:" + id + ", 未找到 type属性, 跳过该节点");
                continue;
            }
            String type = group.get("type").asText();
            ArrayNode componentsNode = (ArrayNode) group.get("components");

            checkGroupFormat(group);

            if ("grid".equals(type)) {
                JsonNode jsonNode = group.get("settings").get("gridTemplateColumns");
                int gridColumns = 1;
                if(jsonNode != null && jsonNode.isArray()){
                    gridColumns = jsonNode.size();
                } else {
                    gridColumns = 1;
                }

                //套用网格模版. 创建网格
                ObjectNode gridNodeDsl = SchemaTransformerCollapseGW.generateComponentStructure(group, gridTemplateDTO);
                // 创建children数组节点
                ArrayNode gridChildren = JsonNodeFactory.instance.arrayNode();
                gridNodeDsl.set("children", gridChildren);

                if (componentsNode != null) {
                    //补全 合并的gridCell
                    SchemaTransformerCollapseGW.handleMultiRowComponents(componentsNode);

                    for (JsonNode componentNode : componentsNode) {
                        checkComponentFormat(componentNode);
                        //创建gridCell
                        ObjectNode gridCellDsl = SchemaTransformerCollapseGW.generateGridCellStructure(componentNode, gridTemplateDTO, gridColumns);
                        if (gridCellDsl == null) {
                            throw new RuntimeException("创建gridCell失败!");
                        }
                        String groupType = componentNode.get("type").asText();
                        if ("gridCell".equalsIgnoreCase(groupType)) {
                            JsonNode childGroupsNode = componentNode.get("groups");
                            //递归调用
                            if (childGroupsNode != null && childGroupsNode.isArray()) {
                                ArrayNode childGroupDsls = recursionGroups((ArrayNode) childGroupsNode, gridTemplateDTO);
                                gridCellDsl.set("children", childGroupDsls);
                            }
                        } else {
                            //  获取 子组件的dsl
                            JsonNode childComponentDSL = SchemaTransformerCollapseGW.generateComponentStructure(componentNode, gridTemplateDTO);
                            if(childComponentDSL != null) {
                                ArrayNode tempArrayNode = JsonNodeFactory.instance.arrayNode();
                                tempArrayNode.add(childComponentDSL);
                                gridCellDsl.set("children", tempArrayNode);
                            }else {
                                log.warn("componentNode not find template.",componentNode.toString());
                            }
                        }
                        // 将gridCellDsl添加到children数组中，而不是覆盖
                        gridChildren.add(gridCellDsl);
                    }
                    result.add(gridNodeDsl);
                }

            }
            else if ("container".equalsIgnoreCase(type)) {

                //创建 container容器
                ObjectNode childContainerDsl = SchemaTransformerCollapseGW.generateComponentStructure(group, gridTemplateDTO);
//                ObjectNode childContainerDslClone = childContainerDsl.deepCopy();
                if (componentsNode != null) {
                    ArrayNode tempArrayNode = JsonNodeFactory.instance.arrayNode();
                    ArrayNode componentNodes = (ArrayNode) componentsNode;
                    for (JsonNode componentNode : componentNodes) {
                        checkComponentFormat(componentNode);
                        // 创建业务组件
                        ObjectNode componentDsl = SchemaTransformerCollapseGW.generateComponentStructure(componentNode, gridTemplateDTO);
                        tempArrayNode.add(componentDsl);

                    }
                    childContainerDsl.put("children", tempArrayNode);
                }

                result.add(childContainerDsl);
            } else {
                throw new RuntimeException("不支持的组类型: " + type);
            }

        }

        return result;

    }


    private static void checkGroupFormat(JsonNode groupNode) {

    }

    private static void checkComponentFormat(JsonNode conponentNode) {

    }


    /**
     * 查找最大字体的label节点并设置其name为"文单标题"
     *
     * @param node 要处理的节点
     */
    private static void setNodeNameByMaxFontSize(JsonNode node) {
        // 获取所有label节点
        List<NodePosition> labelNodes = SchemaTransformerUtil.getAllTypeNodeInfo(node, "label");
        if (labelNodes == null || labelNodes.isEmpty()) {
            return;
        }

        // 初始化最大字体节点信息
        NodePosition maxFontSizeNode = null;
        int maxFontSize = 0;

        int maxNum = 1;
        // 查找最大字体的label节点
        for (NodePosition position : labelNodes) {
            ObjectNode labelNode = position.getObjectNode();
            if (labelNode != null && labelNode.has("settings") && labelNode.get("settings").has("textFontSize")) {
                int fontSize = labelNode.get("settings").get("textFontSize").asInt();
                if (fontSize > maxFontSize) {

                    maxFontSize = fontSize;
                    maxFontSizeNode = position;
                } else if (fontSize == maxFontSize) {
                    maxNum++;
                }
            }
        }

        // 如果找到最大字体节点，设置其name为"文单标题" .maxNum==1 表示最大字体节点唯一
        if (maxFontSizeNode != null && maxNum == 1) {
            maxFontSizeNode.getObjectNode().put("name", "文单标题");
        }
    }

     /** 
     *
     * @return
     */
    public static LayoutTypeEnum getTemplateOpinionBoxLayoutType(SchemaTransformGridTemplateDTO mainGridTemplateNodeDTO) {
        ObjectNode mainGridTemplateNode = mainGridTemplateNodeDTO.getGridTemplatePosition().getObjectNode();

        JsonNode firstBuzNode = JudgeLayoutTypeEnumService.getFirstOpinionBoxNode(mainGridTemplateNode);
        if(firstBuzNode == null){
            log.info("模版格式错误! 没有找到第一个意见组件");
            return null;
        }
        if(firstBuzNode.has("referGroup") && firstBuzNode.get("referGroup").has("groupId")){
            String groupId = firstBuzNode.get("referGroup").get("groupId").asText();
            return  JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(mainGridTemplateNode, groupId);
        }else{
            // 没有groupId, 根据id判断类型
            String id = firstBuzNode.get("id").asText();
            return JudgeLayoutTypeEnumService.getLayoutTypeEnumById(mainGridTemplateNode, id);
        }
    }
    /** 
     *
     * 
     * @return
     */
    public static LayoutTypeEnum getTemplateLayoutType(SchemaTransformGridTemplateDTO mainGridTemplateNodeDTO) {
        ObjectNode mainGridTemplateNode = mainGridTemplateNodeDTO.getGridTemplatePosition().getObjectNode();

        JsonNode firstBuzNode = JudgeLayoutTypeEnumService.getFirstBuzNode(mainGridTemplateNode);
        if(firstBuzNode == null){
            throw new RuntimeException("模版格式错误! 没有找到第一个业务组件");
        }
        if(firstBuzNode.has("referGroup") && firstBuzNode.get("referGroup").has("groupId")){
            String groupId = firstBuzNode.get("referGroup").get("groupId").asText();
            return  JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(mainGridTemplateNode, groupId);
        }else{
            // 没有groupId, 根据id判断类型
            String id = firstBuzNode.get("id").asText();
            return JudgeLayoutTypeEnumService.getLayoutTypeEnumById(mainGridTemplateNode, id);
        }

        
    }

    
    /** 遍历ocrNode中groups, 获取第一个type=grid节点,并找到该节点下的"第一个child节点" 和 "第二个child节点" .
     *  如果 第一个child节点 是 type=label 或 fieldTitle , 
     *      第二个child节点 不是 type=label 且 不是 fieldTitle 
     *      则返回 LayoutTypeEnum.COMPLEX
     * 否则 返回 LayoutTypeEnum.SIMPLE
     *
     * @param ocrNode 是 ocr_schema结构
     * @return
     */
    public static LayoutTypeEnum getOcrLayoutType(JsonNode ocrNode) {
        // ocrNode 可能是整个schema，也可能直接是groups数组
        ArrayNode groupsNodes;
        if (ocrNode == null) {
            return LayoutTypeEnum.SIMPLE;
        }
        if (ocrNode.isArray()) {
            groupsNodes = (ArrayNode) ocrNode;
        } else {
            groupsNodes = (ArrayNode) ocrNode.get("groups");
        }
        if (groupsNodes == null || groupsNodes.size() == 0) {
            return LayoutTypeEnum.SIMPLE;
        }
        // 找到第一个 type=grid 的分组
        JsonNode firstGridGroup = null;
        for (JsonNode group : groupsNodes) {
            if (group != null && group.has("type") && "grid".equalsIgnoreCase(group.get("type").asText())) {
                firstGridGroup = group;
                break;
            }
        }
        if (firstGridGroup == null) {
            return LayoutTypeEnum.SIMPLE;
        }
        // 获取components
        JsonNode componentsNode = firstGridGroup.get("components");
        if (componentsNode == null || !componentsNode.isArray() || componentsNode.size() < 2) {
            // 少于2个组件，按SIMPLE处理
            return LayoutTypeEnum.SIMPLE;
        }

        JsonNode firstComponent = null;
        JsonNode secondComponent = null;

        for (JsonNode componentNode : componentsNode) {
            int colIndex = componentNode.get("cellColRow").get("colIndex").asInt();
            int rowIndex = componentNode.get("cellColRow").get("rowIndex").asInt();
            int flexColSize = componentNode.get("cellColRow").get("flexColSize").asInt();
            JsonNode groupsNode = componentNode.get("groups");
            String typeStr = componentNode.get("type").asText();

            if(colIndex == 1 && (groupsNode == null || groupsNode.isNull())){
                if(SchemaTransformerUtil.isLabelOrFieldTitle(typeStr)){

                    for (JsonNode component2Node : componentsNode) {
                        int colIndex2 = component2Node.get("cellColRow").get("colIndex").asInt();
                        int rowIndex2 = component2Node.get("cellColRow").get("rowIndex").asInt();
                        int flexColSize2 = component2Node.get("cellColRow").get("flexColSize").asInt();
                        String typeStr2 = component2Node.get("type").asText();
                        if(rowIndex2 == rowIndex && colIndex2 == (colIndex+flexColSize) && !SchemaTransformerUtil.isLabelOrFieldTitle(typeStr2)){
                            return LayoutTypeEnum.COMPLEX;
                        }
                    }
                }
            }
        }

        return LayoutTypeEnum.SIMPLE;
    }

    /**
     * ocr中意见组件布局方式
     * @param ocrNode 是 ocr_schema结构
     * @return
     */
    public static LayoutTypeEnum getOcrOpinionBoxLayoutType(JsonNode ocrNode) {
        // ocrNode 可能是整个schema，也可能直接是groups数组
        ArrayNode groupsNodes;
        if (ocrNode == null) {
            return null;
        }
        if (ocrNode.isArray()) {
            groupsNodes = (ArrayNode) ocrNode;
        } else {
            groupsNodes = (ArrayNode) ocrNode.get("groups");
        }
        if (groupsNodes == null || groupsNodes.size() == 0) {
            return null;
        }
        // 找到第一个 type=grid 的分组
        JsonNode firstGridGroup = null;
        for (JsonNode group : groupsNodes) {
            if (group != null && group.has("type") && "grid".equalsIgnoreCase(group.get("type").asText())) {
                firstGridGroup = group;
                break;
            }
        }
        if (firstGridGroup == null) {
            return null;
        }
        // 获取components
        JsonNode componentsNode = firstGridGroup.get("components");
        if (componentsNode == null || !componentsNode.isArray() || componentsNode.size() < 2) {
            // 少于2个组件，按SIMPLE处理
            return LayoutTypeEnum.SIMPLE;
        }

        JsonNode firstComponent = null;
        JsonNode secondComponent = null;

        for (JsonNode componentNode : componentsNode) {
            int colIndex = componentNode.get("cellColRow").get("colIndex").asInt();
            int rowIndex = componentNode.get("cellColRow").get("rowIndex").asInt();
            int flexColSize = componentNode.get("cellColRow").get("flexColSize").asInt();
            JsonNode groupsNode = componentNode.get("groups");
            String typeStr = componentNode.get("type").asText();

            if(colIndex == 1 && (groupsNode == null || groupsNode.isNull())){
                if(SchemaTransformerUtil.isLabelOrFieldTitle(typeStr)){

                    for (JsonNode component2Node : componentsNode) {
                        int colIndex2 = component2Node.get("cellColRow").get("colIndex").asInt();
                        int rowIndex2 = component2Node.get("cellColRow").get("rowIndex").asInt();
                        int flexColSize2 = component2Node.get("cellColRow").get("flexColSize").asInt();
                        String typeStr2 = component2Node.get("type").asText();
                        if(rowIndex2 == rowIndex && colIndex2 == (colIndex+flexColSize) && "uiBusinessEdocOpinionBox".equalsIgnoreCase(typeStr2)){
                            return LayoutTypeEnum.COMPLEX;
                        }
                    }
                }
            }
        }

        return null;
    }
//
//    /**
//     *  udc模版页面中中意见组件布局方式
//     *
//     * 
//     * @return
//     */
//    public static Map<String,LayoutTypeEnum> getEdocOpinionTemplateLayoutType(SchemaTransformGridTemplateDTO mainGridTemplateNodeDTO) {
//        Map<String,LayoutTypeEnum> result = new HashMap<>();
//        LayoutTypeEnum commonType = null; //普通组件布局类型
//        LayoutTypeEnum edocOpinionType = null; //意见组件布局类型
//        ObjectNode mainGridTemplateNode = mainGridTemplateNodeDTO.getGridTemplatePosition().getObjectNode();
//        String mainGridTemplateNodeType = mainGridTemplateNode.get("type").asText();
//
//        //获取模版中 第一个业务组件 和 第一个意见组件.
//
//        throw new RuntimeException("模版格式错误!mainGridTemplateNode 需要是一个grid结构的dsl");
//
//
//
//
//    }

    /**
     * 实现 将jsonSchemaNode中的非label和fieldTitle类型节点, 与 左侧的label节点合并.
     * 规则: 
     * 1. 遍历groups下type==grid , 下的所有 components节点.获取 所有"类别不是label和fieldTitle类型节点"
     * 2. 遍历groups下type==grid , 下的所有 components节点. 根据cellColRow属性来找到 所有"类别不是label和fieldTitle类型节点"节点左侧的label和fieldTitle类型节点. 并删除 左侧的label和fieldTitle类型节点. 以及修改 titleDisplay = left, titleName= label节点的titleName , colIndex 和 flexColSize 也调整为合并后的值
     * @param jsonSchemaNode
     */
    public static void mergeLabel2Component(JsonNode jsonSchemaNode,String mergeType){
        if (jsonSchemaNode == null || !jsonSchemaNode.has("groups")) {
            return;
        }
        
        JsonNode groups = jsonSchemaNode.get("groups");
        if (groups == null || !groups.isArray()) {
            return;
        }
        
        // 遍历所有groups
        for (JsonNode group : groups) {
            if (group == null || !group.has("type") || !"grid".equals(group.get("type").asText())) {
                continue;
            }
            
            if (!group.has("components")) {
                continue;
            }
            
            ArrayNode components = (ArrayNode) group.get("components");
            if (components == null || components.size() == 0) {
                continue;
            }
            
            // 收集需要删除的组件索引
            List<Integer> toRemoveIndices = new ArrayList<>();
            // 收集需要修改的组件
            List<Pair<Integer, ObjectNode>> toUpdateComponents = new ArrayList<>();
            
            // 遍历所有组件，找到非label和fieldTitle类型的组件
            for (int i = 0; i < components.size(); i++) {
                JsonNode component = components.get(i);
                if (component == null || !component.has("type")) {
                    continue;
                }
                
                String type = component.get("type").asText();
                // 跳过label和fieldTitle类型的组件
                if (SchemaTransformerUtil.isLabelOrFieldTitle(type)) {
                    continue;
                }

                if(StringUtils.isNotBlank(mergeType)){
                    if(!type.equalsIgnoreCase(mergeType)){
                        continue;
                    }

                }
                
                // 检查是否有cellColRow属性
                if (!component.has("cellColRow")) {
                    continue;
                }
                
                JsonNode cellColRow = component.get("cellColRow");
                if (!cellColRow.has("rowIndex") || !cellColRow.has("colIndex")) {
                    continue;
                }
                
                int rowIndex = cellColRow.get("rowIndex").asInt();
                int colIndex = cellColRow.get("colIndex").asInt();
                int flexColSize = cellColRow.get("flexColSize").asInt();
                
                // 查找左侧的label节点
                for (int j = 0; j < components.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    
                    JsonNode leftComponent = components.get(j);
                    if (leftComponent == null || !leftComponent.has("type")) {
                        continue;
                    }
                    
                    String leftType = leftComponent.get("type").asText();
                    if (!SchemaTransformerUtil.isLabelOrFieldTitle(leftType)) {
                        continue;
                    }
                    
                    // 检查左侧组件是否有cellColRow属性
                    if (!leftComponent.has("cellColRow")) {
                        continue;
                    }
                    
                    JsonNode leftCellColRow = leftComponent.get("cellColRow");
                    if (!leftCellColRow.has("rowIndex") || !leftCellColRow.has("colIndex")) {
                        continue;
                    }
                    
                    int leftRowIndex = leftCellColRow.get("rowIndex").asInt();
                    int leftColIndex = leftCellColRow.get("colIndex").asInt();
                    int leftFlexColSize = leftCellColRow.get("flexColSize").asInt();
                    // 检查是否满足合并条件：同行且左侧
                    if (leftRowIndex == rowIndex && (leftColIndex + leftFlexColSize) == colIndex ) {
                        // 找到匹配的label节点，进行合并
                        ObjectNode mergedComponent = component.deepCopy();
                        
                        // 修改settings
                        if (!mergedComponent.has("settings")) {
                            mergedComponent.putObject("settings");
                        }
                        ObjectNode settings = (ObjectNode) mergedComponent.get("settings");
                        if("uiBusinessEdocOpinionBox".equalsIgnoreCase(type)) {
                            settings.put("titleDisplay", "top");
                        }else{
                            settings.put("titleDisplay", "left");
                        }
                        
                        // 从左侧label节点复制titleName
                        if (leftComponent.has("settings") && leftComponent.get("settings").has("titleName")) {
                            settings.put("titleName", leftComponent.get("settings").get("titleName").asText());
                        }
                        
                        // 修改cellColRow
                        ObjectNode mergedCellColRow = (ObjectNode) mergedComponent.get("cellColRow");
                        mergedCellColRow.put("colIndex", leftColIndex); // 使用左侧label的colIndex
                        
                        // 合并flexColSize
                        int currentFlexColSize = mergedCellColRow.has("flexColSize") ? mergedCellColRow.get("flexColSize").asInt() : 1;
                        mergedCellColRow.put("flexColSize", leftFlexColSize + currentFlexColSize);
                        
                        // 记录需要删除的左侧label节点索引
                        toRemoveIndices.add(j);
                        // 记录需要更新的组件
                        toUpdateComponents.add(new Pair<>(i, mergedComponent));
                        
                        break; // 找到一个匹配的label节点就退出内层循环
                    }
                }
            }
            
            // 执行删除和更新操作
            // 注意：从后往前删除，避免索引变化
            toRemoveIndices.sort((a, b) -> b.compareTo(a));
            for (Integer index : toRemoveIndices) {
                components.remove(index.intValue());
            }
            
            // 更新组件
            for (Pair<Integer, ObjectNode> update : toUpdateComponents) {
                int index = update.getKey();
                // 由于删除了左侧节点，需要调整索引
                int adjustedIndex = index;
                for (Integer removedIndex : toRemoveIndices) {
                    if (removedIndex < index) {
                        adjustedIndex--;
                    }
                }
                if (adjustedIndex < components.size()) {
                    components.set(adjustedIndex, update.getValue());
                }
            }
        }
    }

    /**
     * 将groups 非意见组件 从split 转为 合并label节点
     * @param splitNode
     * @return
     */
    public static void split2MergeLabel(JsonNode splitNode) {
        JsonNode groups = splitNode.get("groups");
        
        for (JsonNode group : groups) {
            if (group == null || !group.has("type") || !"grid".equals(group.get("type").asText())) {
                continue;
            }
            ObjectNode newGroup = (ObjectNode)group;
            
            if (newGroup.has("components")) {
                ArrayNode originalComponents = (ArrayNode) newGroup.get("components");

                List<Pair<JsonNode, JsonNode>> componentPairs = new ArrayList<>();
                List<JsonNode> componentComponent = new ArrayList<>();
                // 先收集所有 非label 节点
                for (JsonNode component : originalComponents) {
                    if (component.has("type") && !SchemaTransformerUtil.isLabelOrFieldTitle(component.get("type").asText())) {
                        componentComponent.add(component);
                    }
                }

                //遍历componentComponent, 如果左侧是label,则合并label到当前节点, 调整colIndex ,flexColSize. 
                for (int i = 0; i < componentComponent.size(); i++) {
                    JsonNode currentComponent = componentComponent.get(i);
                    
                    //判定左侧组件规则是. currentComponent.rowIndex == currentComponent.rowIndex 相等, 当前节点colIndex 
                    // TODO: 实现具体的合并逻辑
                }
            }
        }
    }

    /**
     * 将groups 从Unsplit 转为 split
     * @param unsplitNode
     * @return
     */
    public static void unsplit2Split(JsonNode unsplitNode) {
//        ObjectNode result = JsonNodeFactory.instance.objectNode();

        JsonNode groups = unsplitNode.get("groups");
//        JsonNode titleName = unsplitNode.get("titleName");
        if (groups == null || !groups.isArray()) {
            return ;
        }
//        ArrayNode resultGroup = JsonNodeFactory.instance.arrayNode();
        
        for (JsonNode group : groups) {
            if (group == null || !group.has("type") || !"grid".equals(group.get("type").asText())) {

                continue;
            }

            // 复制group节点
            ObjectNode newGroup = (ObjectNode) group;

            // 处理gridTemplateColumns：将每列拆分为2列
            if (newGroup.has("settings") && newGroup.get("settings").has("gridTemplateColumns")) {
                ArrayNode originalColumns = (ArrayNode) newGroup.get("settings").get("gridTemplateColumns");
                ArrayNode newColumns = JsonNodeFactory.instance.arrayNode();

                for (JsonNode column : originalColumns) {
                    double colValue = column.asDouble();
                    // 拆分为1/3和2/3，并保留两位小数
                    double col1 = Math.round(colValue * 2 * 100.0 / 3.0) / 100.0;
                    double col2 = Math.round(colValue * 2 * 200.0 / 3.0) / 100.0;
                    newColumns.add(col1);
                    newColumns.add(col2);
                }

                ((ObjectNode) newGroup.get("settings")).set("gridTemplateColumns", newColumns);
            }

            // 处理components：拆分label和组件
            if (newGroup.has("components")) {
                ArrayNode originalComponents = (ArrayNode) newGroup.get("components");
                ArrayNode addComponents = JsonNodeFactory.instance.arrayNode();

                for (JsonNode component : originalComponents) {
                    if (component.has("type")) {
                        //如果type=container , 不拆分
                        if("gridCell".equalsIgnoreCase(component.get("type").asText())){
                            
                            //拆分cell为2个
                            updateSplitGridCell(component);
                        }else{
                           
                            // 创建label节点
                            ObjectNode labelNode = createLabelNode(component);
                            addComponents.add(labelNode);
    
                            // 创建拆分后的组件节点
                            updateSplitComponent(component);

                        }
                        


                    } else {
                        log.warn("not find type . component:" + component.toString());
                    }
                }
                originalComponents.addAll(addComponents);

            }
        }
//                        if(component.has("settings") && component.get("settings").has("styles") && component.get("settings").get("styles").has("display")){
//
//                            ObjectNode stylesDisplayNode = (ObjectNode)component.get("settings").get("styles").get("display");
//                            //修改前display值
//                            stylesDisplay = stylesDisplayNode.asText();
//                            ObjectNode newObjectNode = component.deepCopy();
//                            gridColumn
//                            if(){
//
//                            }
//
//                        }
                        //"display": "none" 的gridCell 需要拆分成2个.
//                        if ("gridCell".equals(type) && "none".equalsIgnoreCase(stylesDisplay)) {
//
//
//                            ObjectNode gridCell = component.deepCopy();
//                            int oldSize = gridCell.get("cellColRow").get("flexColSize").asInt();
//                            ((ObjectNode) gridCell.get("cellColRow")).put("flexColSize", oldSize * 2);
//                            newComponents.add(gridCell);
//                        } else { //设置合并
//                           if (shouldSplitComponent(component)) {
//                                // 创建label节点
//                                ObjectNode labelNode = createLabelNode(component);
//                                newComponents.add(labelNode);
//
//                                // 创建拆分后的组件节点
//                                ObjectNode splitComponent = createSplitComponent(component);
//                                newComponents.add(splitComponent);
//                            } else {
//                                // 不需要拆分的组件直接添加, 修改为合并组件
//                                newComponents.add(component);
//                            }
//                        }
//                    }
//                }
//
//                newGroup.set("components", newComponents);
//            }
//
//            resultGroup.add(newGroup);
//        }
//        return unsplitNode;
    }

    /**
     * TODO: 这里不做容器通用. 而是单独一个意见组件
     * @param jsonNode
     * @return
     */
    public static void unsplit2OneCellUpDown(JsonNode jsonNode,String componentType) {
        // ObjectNode result = JsonNodeFactory.instance.objectNode();
        JsonNode groups = jsonNode.get("groups");
        JsonNode titleName = jsonNode.get("titleName");
        if (groups == null || !groups.isArray()) {
            return ;
        }
        //ArrayNode resultGroup = JsonNodeFactory.instance.arrayNode();

        for (JsonNode group : groups) {
            if (group == null || !group.has("type") || !"grid".equals(group.get("type").asText())) {
                // resultGroup.add(group);
                continue;
            }

            // 复制group节点
            ObjectNode newGroup = (ObjectNode)group;


            // 处理components：拆分label和组件
            if (newGroup.has("components")) {
                ArrayNode originalComponents = (ArrayNode) newGroup.get("components");
                ArrayNode newComponents = JsonNodeFactory.instance.arrayNode();

                // 记录需要删除的索引
                List<Integer> toRemoveIndices = new ArrayList<>();
                for (int i = 0; i < originalComponents.size(); i++) {
                    JsonNode component = originalComponents.get(i);
                    if (componentType.equals(component.get("type").asText())) {

                        JsonNode oneCellUpDownNode = createOneCellUpDownNode(component);
                        toRemoveIndices.add(i);
                        newComponents.add(oneCellUpDownNode);
                    }
                }
                // 从后往前删除
                toRemoveIndices.sort((a, b) -> b.compareTo(a));
                for (Integer idx : toRemoveIndices) {
                    originalComponents.remove(idx.intValue());
                }
                // 添加新节点
                for (JsonNode node : newComponents) {
                    originalComponents.add(node);
                }
            }

        }
 
    }



    /**
     * 判断组件是否需要拆分
     */
    private static boolean shouldSplitComponent(JsonNode component) {
        // 条件：type不等于gridCell和label，并且settings->titleDisplay不等于空和"none"
        if (!component.has("type")) {
            throw new RuntimeException("未找到type类型,component:"+component.toString());
        }
        
        String type = component.get("type").asText();
        if ("gridCell".equals(type) || SchemaTransformerUtil.isLabelOrFieldTitle(type)) {
            return false;
        }
        
        // 检查titleDisplay
//        if (component.has("settings") && component.get("settings").has("titleDisplay")) {
//            String titleDisplay = component.get("settings").get("titleDisplay").asText();
//            if ("".equals(titleDisplay) || "none".equals(titleDisplay)) {
//                return false;
//            }
//        }
        
        return true;
    }
    
    /**
     * 创建label节点
     */
    private static ObjectNode createLabelNode(JsonNode originalComponent) {
        ObjectNode labelNode = JsonNodeFactory.instance.objectNode();
        
        // 复制基本属性
//        if (originalComponent.has("id")) {
            labelNode.put("id", RandomUtils.nextLong());
//        }
        if (originalComponent.has("name")) {
            labelNode.put("name", originalComponent.get("name").asText());
        }

        // 设置type为label
        labelNode.put("type", "label");

        // 设置settings
        ObjectNode settings = JsonNodeFactory.instance.objectNode();
        if (originalComponent.has("settings") && originalComponent.get("settings").has("titleName")) {
            settings.put("titleName", originalComponent.get("settings").get("titleName").asText());
        }
        labelNode.set("settings", settings);
        
        // 设置cellColRow：使用原组件的colIndex，但colSize为1
        if (originalComponent.has("cellColRow")) {
            ObjectNode cellColRow = (ObjectNode) originalComponent.get("cellColRow").deepCopy();
            cellColRow.put("colIndex", cellColRow.get("colIndex").asInt() * 2 - 1); // 调整列索引
            cellColRow.put("flexColSize", 1);
            labelNode.set("cellColRow",cellColRow);

        }
        
        // 设置referGroup
        if (originalComponent.has("referGroup") && !originalComponent.get("referGroup").isNull()) {
            ObjectNode referGroup = (ObjectNode) originalComponent.get("referGroup");
            referGroup.put("groupType", "title");
            labelNode.set("referGroup", referGroup);
        }
//
//        // 复制groups
//        if (originalComponent.has("groups")) {
//            labelNode.set("groups", originalComponent.get("groups"));
//        }
        
        return labelNode;
    }
    
    /**
     * 创建拆分后的组件节点
     */
    private static void updateSplitComponent(JsonNode originalComponent) {
        ObjectNode splitComponent = (ObjectNode)originalComponent;
        
        // 修改settings中的titleDisplay为none
        if (splitComponent.has("settings")) {
            ((ObjectNode) splitComponent.get("settings")).put("titleDisplay", "none");
        }
        
        // 调整cellColRow：使用原组件的colIndex，但colSize为1，colIndex+1
        if (splitComponent.has("cellColRow")) {
            ObjectNode cellColRow = (ObjectNode) splitComponent.get("cellColRow");
            int originalColIndex = cellColRow.get("colIndex").asInt();
            int flexColSize = cellColRow.get("flexColSize").asInt();
            cellColRow.put("colIndex", originalColIndex * 2); // 调整列索引
            cellColRow.put("flexColSize", flexColSize*2-1);

        }

    }

    /**
     * 拆分container的组件节点
     */
    private static void updateSplitGridCell(JsonNode originalComponent) {
        ObjectNode splitComponent = (ObjectNode)originalComponent;
        
        // 调整cellColRow：使用原组件的colIndex，但colSize为1，colIndex+1
        if (splitComponent.has("cellColRow")) {
            ObjectNode cellColRow = (ObjectNode) splitComponent.get("cellColRow");

            int originalColIndex = cellColRow.get("colIndex").asInt();
            int flexColSize = cellColRow.get("flexColSize").asInt();
            cellColRow.put("colIndex", originalColIndex * 2-1); // 调整列索引
            cellColRow.put("flexColSize", flexColSize*2);

        }

    }

    /**
     * TODO: 临时修改为Simple模式 top显示
     * @param component 原始组件节点

     */
    public static JsonNode createOneCellUpDownNodeSimple(JsonNode component) {

        ObjectNode originalComponent = component.deepCopy();
        // 2. gridCell节点.cellColRow = 转换前uiBusinessEdocOpinionBox节点.cellColRow
        if (originalComponent.has("settings")) {
            ObjectNode settingsNode = (ObjectNode)originalComponent.get("settings");
            settingsNode.put("titleDisplay","top");
        }
        return originalComponent;
    }
    /**
     * 创建ONE_CELL_UP_DOWN布局的意见组件节点
     * @param originalComponent 原始组件节点

     */
    public static ObjectNode createOneCellUpDownNode(JsonNode originalComponent) {
        // 1. 创建一个新的 gridCell节点
        ObjectNode gridCellNode = JsonNodeFactory.instance.objectNode();
        gridCellNode.put("type", "gridCell");
        
        // 2. gridCell节点.cellColRow = 转换前uiBusinessEdocOpinionBox节点.cellColRow
        if (originalComponent.has("cellColRow")) {
            JsonNode cellColRowNode = originalComponent.get("cellColRow").deepCopy();

            Integer flexColSize = cellColRowNode.get("flexColSize").asInt();
//            flexColSize=flexColSize*2;
            ((ObjectNode)cellColRowNode).put("flexColSize", flexColSize);


            gridCellNode.set("cellColRow", cellColRowNode);
        }
        
        // 3. 创建container节点
        ObjectNode containerNode = JsonNodeFactory.instance.objectNode();
        containerNode.put("type", "container");
        
        // 5. container节点 settings属性设置
        ObjectNode containerSettings = JsonNodeFactory.instance.objectNode();
        containerSettings.put("flexWrap", "nowrap");
        containerSettings.put("display", "flex");
        containerSettings.put("flexDirection", "column");
        containerSettings.put("gap", "0px 20px");
        containerSettings.put("alignItems", "stretch");
        containerSettings.put("justifyContent", "stretch");

        ObjectNode boxModelNode = JsonNodeFactory.instance.objectNode();
        boxModelNode.put("margin", "0 0 0 0");
        boxModelNode.put("padding", "0px 0px 0px 0px");
        containerSettings.put("boxModel",boxModelNode);
        containerNode.set("settings", containerSettings);
        
        // 4. 创建label节点
        ObjectNode labelNode = JsonNodeFactory.instance.objectNode();
        labelNode.put("type", "label");
        
        // 6. 设置label节点的settings->titleName 为 转换前uiBusinessEdocOpinionBox节点.settings.titleName
        ObjectNode labelSettings = JsonNodeFactory.instance.objectNode();
        if (originalComponent.has("settings") && originalComponent.get("settings").has("titleName")) {
            labelSettings.put("titleName", originalComponent.get("settings").get("titleName").asText());
        }
        labelNode.set("settings", labelSettings);
        
        // 7. 克隆 转换前uiBusinessEdocOpinionBox节点 所有属性到 新uiBusinessEdocOpinionBox节点
        ObjectNode newOpinionBoxNode = originalComponent.deepCopy();
        // 去除cellColRow属性
        if (newOpinionBoxNode.has("cellColRow")) {
            newOpinionBoxNode.remove("cellColRow");
        }
        // 设置settings->titleDisplay="none"，并去除titleName属性
        if (newOpinionBoxNode.has("settings")) {
            ObjectNode settings = (ObjectNode) newOpinionBoxNode.get("settings");
            settings.put("titleDisplay", "none");
            // 去除titleName属性
            if (settings.has("titleName")) {
                settings.remove("titleName");
            }
        }
        
        // 4. 将label节点, 新uiBusinessEdocOpinionBox节点 加入 container节点 的components属性集合中
        ArrayNode containerComponents = JsonNodeFactory.instance.arrayNode();
        containerComponents.add(labelNode);
        containerComponents.add(newOpinionBoxNode);
        containerNode.set("components", containerComponents);
        
        // 3. 将 container节点 加入 gridCell节点 的 groups属性集合中
        ArrayNode gridCellGroups = JsonNodeFactory.instance.arrayNode();
        gridCellGroups.add(containerNode);
        gridCellNode.set("groups", gridCellGroups);
        
        return gridCellNode;
    }

}
