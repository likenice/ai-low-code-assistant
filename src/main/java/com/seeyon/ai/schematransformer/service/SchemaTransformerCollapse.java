package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Slf4j
public class SchemaTransformerCollapse {

    /**
     * grid类型, 合并模板数据与布局数据 (用于 gridCell->component  和 group->component)
     * @param gridCell 模板节点数据
     * @param layoutNode 布局节点数据
     * @return 合并后的节点数据
     */
    public static JsonNode mergeGridTemplateAndLayout(JsonNode gridCell, JsonNode layoutNode,Map<String,String> collectTypeAndSourceMap) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();

        // 复制模板节点的基本属性
        result.put("id", SchemaTransformerUtil.generateShortUUID());
        result.put("type", gridCell.get("type").asText());
        result.set("settings", gridCell.get("settings").deepCopy());
        result.set("sourceSchema", gridCell.get("sourceSchema").deepCopy());

        // 处理children节点
        ArrayNode children = JsonNodeFactory.instance.arrayNode();
        JsonNode templateNode = gridCell.get("children").get(0);



        // 添加styles
        JsonNode cellColRow = layoutNode.get("cellColRow");
        ObjectNode styles = JsonNodeFactory.instance.objectNode();

        boolean isDisplay = true;
        if(cellColRow.has("display")) {
            String display = cellColRow.get("display").asText();
            if("none".equals(display)) {
                //单独处理空白gridCell
                isDisplay = false;
            }

        }
        if(!isDisplay){
            styles.put("display", "none");
        } else {
            styles.put("display", "flex");
        }
        styles.put("gridRow", cellColRow.get("rowIndex").asInt() + "/" + (cellColRow.get("rowIndex").asInt() + cellColRow.get("flexRowSize").asInt()));
        styles.put("gridColumn", cellColRow.get("colIndex").asInt() + "/" + (cellColRow.get("colIndex").asInt() + cellColRow.get("flexColSize").asInt()));
//        result.set("styles", styles);
        ((ObjectNode)result.get("settings")).set("styles", styles);

        String layoutType = layoutNode.get("type").asText();

        ObjectNode mergedChild = mergeGridTemplateAndLayoutCore(templateNode, layoutNode, collectTypeAndSourceMap);

        if(isDisplay) {
            children.add(mergedChild);
        }
        //如果是container容器 在深入一层处理
        if("container".equals(layoutType)){
            ArrayNode childNodes = JsonNodeFactory.instance.arrayNode();
            JsonNode childComponents = layoutNode.get("components");
            if(childComponents != null){
                for (JsonNode childComponent : childComponents) {
                    ObjectNode containerChild = mergeGridTemplateAndLayoutCore(templateNode, childComponent, collectTypeAndSourceMap);
                    childNodes.add(containerChild);

                }
            }
            mergedChild.put("children", childNodes);
        }


        result.set("children", children);

        return result;
    }

    public static ObjectNode mergeGridTemplateAndLayoutCore(JsonNode templateNode, JsonNode layoutNode, Map<String,String> collectTypeAndSourceMap) {

        ObjectNode mergedChild = JsonNodeFactory.instance.objectNode();
        mergedChild.put("id", SchemaTransformerUtil.generateShortUUID());


        // 合并settings
        ObjectNode mergedSettings = JsonNodeFactory.instance.objectNode();
        if (templateNode != null && templateNode.has("settings")) {
            mergedSettings.setAll((ObjectNode)templateNode.get("settings"));
        }
        if (layoutNode.has("settings")) {
            mergedSettings.setAll((ObjectNode)layoutNode.get("settings"));
        }

        // 使用布局节点的type
        String layoutType = layoutNode.get("type").asText();
        String layoutSourceSchemaId = collectTypeAndSourceMap.get(layoutType);

        mergedChild.put("type", layoutType);
        mergedChild.set("settings", mergedSettings);

        // 复制sourceSchema
        ObjectNode sourceSchemaNode = JsonNodeFactory.instance.objectNode();
        if(templateNode != null && templateNode.has("sourceSchema")){
            sourceSchemaNode = (ObjectNode)templateNode.get("sourceSchema").deepCopy();
        }


        if (layoutSourceSchemaId != null) {
            sourceSchemaNode.put("id",layoutSourceSchemaId);
        }
        mergedChild.set("sourceSchema", sourceSchemaNode);


        // 复制dataSource
        if (layoutNode.has("dataSource")) {
            mergedChild.set("dataSource", layoutNode.get("dataSource").deepCopy());
        }
        return  mergedChild;
    }



    public static void group2TemplateSettings(JsonNode sourceGroup,JsonNode templateNode){

//        String titleName = "";
//        String belongTabs = "";
//        JsonNode gridTemplateColumnsNode = null;
//        JsonNode gridTemplateRowsNode = null;

        ObjectNode settingNode = null;
        if(!sourceGroup.has("settings")){
            settingNode = ((ObjectNode)sourceGroup).putObject("settings");
        } else {
            settingNode = (ObjectNode)sourceGroup.get("settings");
        }

        // 1. 更新collapse的settings->title信息
//        if ("collapse".equals(sourceGroup.get("type").asText()) || "datagrid".equalsIgnoreCase(sourceGroup.get("type").asText())) {
        JsonNode templateSettings = templateNode.get("settings");
        if(settingNode.get("titleName") != null) {
            ((ObjectNode) templateSettings).put("title", settingNode.get("titleName"));
        }
        if (settingNode.get("belongTabs") != null) {
            ((ObjectNode) templateSettings).put("belongTabs", settingNode.get("belongTabs"));
        }
    }

    /**
     * 按照流式布局, 将源分组结构与样式结构合并 (已完成)
     * @param sourceGroup 源分组结构（包含components的collapse分组）
     * @param templateNode 样式结构模板（包含container的collapse模板）
     * @return 转换后的结构
     */
    public static JsonNode convertGroupStreamStructure(JsonNode sourceGroup, JsonNode templateNode,Map<String,String> collectTypeAndSourceMap) {


        // 1. 克隆模板节点作为基础结构
        ObjectNode newNode = null;
        if (templateNode == null || !templateNode.isObject()) {
            newNode = JsonNodeFactory.instance.objectNode();
        } else {
            newNode = templateNode.deepCopy();
        }
        newNode.put("id",SchemaTransformerUtil.generateShortUUID());


        // 2. 从sourceGroup复制settings到newNode
        group2TemplateSettings(sourceGroup,newNode);


        // 3. 查找form节点和其下的container节点
//        JsonNode formNode = SchemaTransformerUtil.getFirstNodeByType(newNode, "form");
//        if (formNode != null) {
        NodePosition mostFrequentNodePosition = SchemaTransformerUtil.getMostFrequentNodePosition(newNode);
        JsonNode containerNode = mostFrequentNodePosition.getObjectNode();
        if (containerNode instanceof ObjectNode) {
            ObjectNode container = (ObjectNode) containerNode;

            // 4. 获取模板中container的第一个组件的sourceSchema id

            String firstTemplateSourceSchemaId = null;
            JsonNode containerChildren = container.get("children");
            if (containerChildren != null && containerChildren.isArray() && containerChildren.size() > 0) {
                JsonNode firstComponent = containerChildren.get(0);
                if (firstComponent.has("sourceSchema")) {
                    firstTemplateSourceSchemaId = firstComponent.get("sourceSchema").get("id").asText();
                }
            }

            // 5. 设置container的columns
            JsonNode gridTemplateColumns = sourceGroup.get("settings").get("gridTemplateColumns");
            if (gridTemplateColumns != null && gridTemplateColumns.isArray()) {
                container.with("settings").put("columns", gridTemplateColumns.size());
            }

            // 6. 处理components
            if (sourceGroup.has("components") && sourceGroup.get("components").isArray()) {
                ArrayNode newComponents = container.putArray("children");

                for (JsonNode component : sourceGroup.get("components")) {
                    ObjectNode newComponent = newComponents.addObject();
                    String componentType = component.get("type").asText();
                    // 复制基本属性
                    newComponent.put("type", componentType);

                    // 复制settings
                    if (component.has("settings")) {
                        newComponent.set("settings", component.get("settings"));
                    }

                    // 复制dataSource
                    if (component.has("dataSource")) {
                        newComponent.set("dataSource", component.get("dataSource"));
                    }

                    // 设置sourceSchema
                    String templateSourceSchemaId = collectTypeAndSourceMap.get(componentType);
                    if(templateSourceSchemaId == null){
                        templateSourceSchemaId = firstTemplateSourceSchemaId;
                    }
                    newComponent.putObject("sourceSchema").put("id", templateSourceSchemaId);

                    // 复制cellColRow到settings中
                    if (component.has("cellColRow")) {
                        JsonNode cellColRow = component.get("cellColRow");
                        ObjectNode settings = newComponent.has("settings") ?
                                (ObjectNode) newComponent.get("settings") :
                                newComponent.putObject("settings");

                        settings.put("flexRowSize", cellColRow.get("flexColSize").asInt());
                    }
                }
            }
        }
//        }

        return newNode;
    }


    /**
     * 网格中组件没有拆分成label和组件:
     *  将源分组结构转换为目标样式结构
     * @param sourceGroup 源分组结构（包含components的collapse/dataGrid分组）
     * @param templateNode 样式结构模板（包含grid的collapse/dataGrid模板）
     * @return 转换后的结构
     */
    @SuppressWarnings("deprecation")
    public static JsonNode convertGroupToGridStructure(JsonNode sourceGroup, JsonNode templateNode,Map<String,String> collectTypeAndSourceMap) {
        if (templateNode == null || !templateNode.isObject()) {
            log.info("无效的模板节点");
            return null;
        }

        // 深拷贝模板节点
        ObjectNode newNode = templateNode.deepCopy();
        newNode.put("id",SchemaTransformerUtil.generateShortUUID());

        //初始化模版的settings参数
        group2TemplateSettings(sourceGroup,newNode);

//        }


        // 获取templateNode中的第一个grid节点id. 和 第一个gridCell节点id
        NodePosition mostFrequentNodeType = SchemaTransformerUtil.getMostFrequentNodePosition(newNode);

        JsonNode gridNode = mostFrequentNodeType.getObjectNode(); //包含最多实体组件的gridId
        JsonNode gridCellNode = SchemaTransformerUtil.getFirstNodeByType(gridNode, "gridCell"); //第一个网格cell样式id
        JsonNode gridNodeId = gridNode != null && gridNode.has("sourceSchema") ? gridNode.get("sourceSchema").get("id") : null;
        // JsonNode firstGridCellId = firstGridCell != null && firstGridCell.has("sourceSchema") ? firstGridCell.get("sourceSchema").get("id") : null;
        // JsonNode firstComponentId = firstComponent != null && firstComponent.has("sourceSchema") ? firstComponent.get("sourceSchema").get("id") : null;

        //按照newNode格式,生成sourceGroup布局的数据:sourceNewNode
        //sourceGroup的settings 赋值 newNode的settings

        ArrayNode targetGridNodes = new ArrayNode(JsonNodeFactory.instance);
        // 2. 遍历components节点下的所有对象
        if (sourceGroup.has("components") && sourceGroup.get("components").isArray()) {
            ArrayNode components = (ArrayNode) sourceGroup.get("components");

            //补全 合并的gridCell
            handleMultiRowComponents(components);

            //新建一个空白的JsonNode节点
            ObjectNode targetGridNode = new ObjectNode(JsonNodeFactory.instance);
            targetGridNode.put("id",SchemaTransformerUtil.generateShortUUID());
            targetGridNode.put("type", "grid");
            //firstGrid的settings 赋值 targetGridNode的settings
            if(gridNode.has("settings")){
                targetGridNode.put("settings", gridNode.get("settings"));
            } else {
                targetGridNode.putObject("settings");
            }
            ObjectNode settingsNode = (ObjectNode)targetGridNode.get("settings");
            JsonNode groupSettingsNode = sourceGroup.get("settings");

            settingsNode.put("gridTemplateColumns",groupSettingsNode.get("gridTemplateColumns"));
            settingsNode.put("gridTemplateRows",groupSettingsNode.get("gridTemplateRows"));
            JsonNode dataSourceNode = gridNode.get("dataSource");
            if (dataSourceNode != null) {
                targetGridNode.put("dataSource", dataSourceNode);
            }
            targetGridNode.putObject("sourceSchema").put("id", gridNodeId);
            // 创建gridCell
            ArrayNode gridCellNodes = ((ObjectNode) targetGridNode).putArray("children");

            // 遍历每个组件
            for (JsonNode component : components) {

                //模版数据与布局数据合并
                JsonNode mergeGridCellNode = mergeGridTemplateAndLayout(gridCellNode, component,collectTypeAndSourceMap);
                // 将合并后的数据添加到gridChildren中
                gridCellNodes.add(mergeGridCellNode);

            }
            targetGridNodes.add(targetGridNode);
            //删除原来的grid
            SchemaTransformerUtil.deleteNodePosition(newNode,mostFrequentNodeType);
            //插入新的targetGridNode
            return  SchemaTransformerUtil.insertLayoutListAtCollapsePosition(newNode, mostFrequentNodeType, targetGridNodes);


        }

        return newNode;
    }

    /**
     * 网格中组件没有拆分成label和组件:
     *  将源分组结构转换为目标样式结构
     * @param sourceGroup 源分组结构（包含components的collapse/dataGrid分组）
     * @param templateNode 样式结构模板（包含grid的collapse/dataGrid模板）
     * @return 转换后的结构
     */
    @SuppressWarnings("deprecation")
    public static JsonNode convertGroupToContainerStructure(JsonNode sourceGroup, JsonNode templateNode,Map<String,String> collectTypeAndSourceMap) {
        if (templateNode == null || !templateNode.isObject()) {
            log.info("无效的模板节点");
            return null;
        }

        // 深拷贝模板节点
        ObjectNode newNode = templateNode.deepCopy();
        newNode.put("id",SchemaTransformerUtil.generateShortUUID());

        //初始化模版的settings参数
        group2TemplateSettings(sourceGroup,newNode);

//        }


        // 获取templateNode中的第一个grid节点id. 和 第一个gridCell节点id
        NodePosition mostFrequentNodeType = SchemaTransformerUtil.getMostFrequentNodePosition(newNode);

        JsonNode gridNode = mostFrequentNodeType.getObjectNode(); //包含最多实体组件的gridId
        JsonNode gridCellNode = SchemaTransformerUtil.getFirstNodeByType(gridNode, "gridCell"); //第一个网格cell样式id
        JsonNode gridNodeId = gridNode != null && gridNode.has("sourceSchema") ? gridNode.get("sourceSchema").get("id") : null;
        // JsonNode firstGridCellId = firstGridCell != null && firstGridCell.has("sourceSchema") ? firstGridCell.get("sourceSchema").get("id") : null;
        // JsonNode firstComponentId = firstComponent != null && firstComponent.has("sourceSchema") ? firstComponent.get("sourceSchema").get("id") : null;

        //按照newNode格式,生成sourceGroup布局的数据:sourceNewNode
        //sourceGroup的settings 赋值 newNode的settings

        ArrayNode targetGridNodes = new ArrayNode(JsonNodeFactory.instance);
        // 2. 遍历components节点下的所有对象
        if (sourceGroup.has("components") && sourceGroup.get("components").isArray()) {
            ArrayNode components = (ArrayNode) sourceGroup.get("components");
            //新建一个空白的JsonNode节点
            ObjectNode targetGridNode = new ObjectNode(JsonNodeFactory.instance);
            targetGridNode.put("id",SchemaTransformerUtil.generateShortUUID());
            targetGridNode.put("type", "grid");
            //firstGrid的settings 赋值 targetGridNode的settings
            if(gridNode.has("settings")){
                targetGridNode.put("settings", gridNode.get("settings"));
            } else {
                targetGridNode.putObject("settings");
            }
            ObjectNode settingsNode = (ObjectNode)targetGridNode.get("settings");
            JsonNode groupSettingsNode = sourceGroup.get("settings");

            settingsNode.put("gridTemplateColumns",groupSettingsNode.get("gridTemplateColumns"));
            settingsNode.put("gridTemplateRows",groupSettingsNode.get("gridTemplateRows"));
            JsonNode dataSourceNode = gridNode.get("dataSource");
            if (dataSourceNode != null) {
                targetGridNode.put("dataSource", dataSourceNode);
            }
            targetGridNode.putObject("sourceSchema").put("id", gridNodeId);
            // 创建gridCell
            ArrayNode gridCellNodes = ((ObjectNode) targetGridNode).putArray("children");

            // 遍历每个组件
            for (JsonNode component : components) {

                //模版数据与布局数据合并
                JsonNode mergeGridCellNode = mergeGridTemplateAndLayout(gridCellNode, component,collectTypeAndSourceMap);
                // 将合并后的数据添加到gridChildren中
                gridCellNodes.add(mergeGridCellNode);

            }
            targetGridNodes.add(targetGridNode);
            //删除原来的grid
            SchemaTransformerUtil.deleteNodePosition(newNode,mostFrequentNodeType);
            //插入新的targetGridNode
            return  SchemaTransformerUtil.insertLayoutListAtCollapsePosition(newNode, mostFrequentNodeType, targetGridNodes);


        }

        return newNode;
    }


    /**
     * 网格中一个组件拆分成label和组件 2个网格:
     *  将源分组结构转换为目标样式结构
     * @param sourceGroup 源分组结构（包含components的collapse/dataGrid分组）
     * @param templateNode 样式结构模板（包含grid的collapse/dataGrid模板）
     *
     * @param collectTypeAndSourceMap 模版中获取的 类别->datasourceId
     * @param isComplexGroup 是否是复杂分组
     * @return 转换后的结构
     */
    public static JsonNode convertComplexGroupCellTo2CellStructure(JsonNode sourceGroup, JsonNode templateNode,Map<String,String> collectTypeAndSourceMap, boolean isComplexGroup) {
        if (templateNode == null || !templateNode.isObject()) {
            log.info("无效的模板节点");
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        // 深拷贝模板节点
        ObjectNode newNode = templateNode.deepCopy();
        newNode.put("id",SchemaTransformerUtil.generateShortUUID());

        // 2. 从sourceGroup复制settings到newNode
        group2TemplateSettings(sourceGroup,newNode);


        JsonNode targetGridNode = null;
        NodePosition gridNodePosition = SchemaTransformerUtil.getNodeHasMostComponent(newNode, "grid");

        if( newNode.has("type") && newNode.get("type").asText().equals("grid")){ //templateNode 是grid节点
            targetGridNode = newNode ;
        } else {
            targetGridNode =  gridNodePosition.getObjectNode();//templateNode 是collapse节点
            ((ObjectNode)targetGridNode).put("id",SchemaTransformerUtil.generateShortUUID());
        }



        // 获取模板中第一个和第二个gridCell的sourceSchema.id
        String firstGridCellComponentId = null;
        String secondGridCellComponentId = null;
        String firstGridCellId = null;
        String secondGridCellId = null;
        JsonNode gridNode = SchemaTransformerUtil.getNodeHasMostComponent(templateNode, "grid").getObjectNode();
        if (gridNode != null && gridNode.has("children") && gridNode.get("children").isArray()) {

            // 1. 更新collapse的settings信息


            ArrayNode gridChildren = (ArrayNode) gridNode.get("children");
//            (ObjectNode)gridNode.set("",);
            if (gridChildren.size() >= 2) {
                // 获取第一个gridCell的children中的sourceSchema.id
                JsonNode firstGridCell = gridChildren.get(0);
                if (firstGridCell.has("children") && firstGridCell.get("children").isArray()
                        && firstGridCell.get("children").get(0).has("sourceSchema")) {
                    firstGridCellComponentId = firstGridCell.get("children").get(0).get("sourceSchema").get("id").asText();

                    firstGridCellId = firstGridCell.get("sourceSchema").get("id").asText();
                }

                // 获取第二个gridCell的children中的sourceSchema.id
                JsonNode secondGridCell = gridChildren.get(1);
                if (secondGridCell.has("children") && secondGridCell.get("children").isArray()
                        && secondGridCell.get("children").get(0).has("sourceSchema")) {
                    secondGridCellComponentId = secondGridCell.get("children").get(0).get("sourceSchema").get("id").asText();

                    secondGridCellId = secondGridCell.get("sourceSchema").get("id").asText();
                }
            }
        }


        // 2. 遍历components节点下的所有对象
        if (sourceGroup.has("components") && sourceGroup.get("components").isArray()) {
            //兼容gridTemplateRows
            //isComplexGroup :true 则不会将sourceGroup中的组件分成2个cell.以及也不会修改gridTemplateColumns
            fixGridTemplateRows(sourceGroup);

            ArrayNode components = (ArrayNode) sourceGroup.get("components");
            JsonNode settingsNode = sourceGroup.get("settings");
            if(newNode.has("type") && "collapse".equals(newNode.get("type").asText()) && settingsNode.has("title") ){

                String collapseTitle = settingsNode.get("title").asText();
                ObjectNode collapseSettingsNode = mapper.createObjectNode();
                collapseSettingsNode.put("title", collapseTitle);
                newNode.set("settings", collapseSettingsNode);
            }

            // 获取grid节点

            if (targetGridNode != null && "grid".equals(targetGridNode.get("type").asText())) {

                ((ObjectNode)targetGridNode).set("settings", gridNode.get("settings").deepCopy());
                ObjectNode gridSettingsNode = ((ObjectNode)targetGridNode.get("settings"));

                JsonNode groupSettingsNode = sourceGroup.get("settings");
                JsonNode gridTemplateColumns = groupSettingsNode.get("gridTemplateColumns");
                gridSettingsNode.put("gridTemplateColumns",isComplexGroup ? gridTemplateColumns : SchemaTransformerUtil.splitGridTemplateColumns( gridTemplateColumns));
                gridSettingsNode.put("gridTemplateRows",groupSettingsNode.get("gridTemplateRows"));

                ArrayNode gridChildren = ((ObjectNode) targetGridNode).putArray("children");


                //补全 合并的gridCell
                handleMultiRowComponents(components);
                // 遍历每个组件
                for (JsonNode component : components) {

                    JsonNode cellColRow = component.get("cellColRow");
                    if (cellColRow != null) {
                        String componentType = null;
                        JsonNode componentTypeNode = component.get("type");
                        if (componentTypeNode != null) {
                            componentType = componentTypeNode.asText();
                        }
                        // 先从模版中找相同类型组件, 如果没有找到, 以偶数网格组件的id来替换.
                        String secondGridCellComponentIdTemp = collectTypeAndSourceMap.get(componentType);
                        if (secondGridCellComponentIdTemp == null) {
                            secondGridCellComponentIdTemp = secondGridCellComponentId;
                        }

                        if(isComplexGroup) {
                            // ===============创建grid逐个映射cell的gridCell===============
                            createComplexComponentGridCellNode(component, gridChildren, cellColRow, firstGridCellId, firstGridCellComponentId, secondGridCellId, secondGridCellComponentIdTemp,secondGridCellId,secondGridCellComponentId);
                        }else{
                            // ===============创建label的gridCell===============
                            createLabelGridCellNode(component, gridChildren, cellColRow, firstGridCellId, firstGridCellComponentId,false);
                            // ===============创建组件的gridCell===============
                            createComponentGridCellNode(component, gridChildren, cellColRow, secondGridCellId, secondGridCellComponentIdTemp,false);
                        }
                    }
                }
            }
        }
        if( newNode.has("type") && newNode.get("type").asText().equals("grid")){ //templateNode 是grid节点
            return newNode;
        } else {
            ArrayNode targetGridArrayNode = mapper.createArrayNode();

            // 将JsonNode添加到ArrayNode中
            targetGridArrayNode.add(targetGridNode);
            JsonNode tempNewNode = SchemaTransformerUtil.insertLayoutListAtCollapsePosition(newNode, gridNodePosition, targetGridArrayNode);
            SchemaTransformerUtil.deleteNodeById((ObjectNode) tempNewNode,gridNodePosition.getId());
            return tempNewNode;

        }


    }

    /**
     *  ocr识别网格和内部的组件. 创建group的组件精准套用模版的节点
     *
     * @param component ocr DSL. 示例 : {"settings":{"gridTemplateColumns":[],"gridTemplateRows":[],"titleName":"申请部门","validation":{"required":false}},"type":"reference","ocrRelationId":"6bdf5a11-ea54-4b6e-94e5-b74414719c04","dataSource":{"dataField":"shenqingbumen"},"cellColRow":{"rowIndex":1,"flexRowSize":1,"colIndex":2,"flexColSize":1}}
     * @param gridChildren 各group, 页面dsl 示例: [{"type":"gridCell","settings":{"styles":{"display":"flex","gridColumn":"1/2","gridRow":"1/2"}},"children":[{"type":"label","settings":{"content":"申请人"},"sourceSchema":{"id":"label_v2bx"},"dataSource":{"dataField":"applicant"}}],"sourceSchema":{"id":"udcGridCell_Dv65"}},{"type":"gridCell","settings":{"styles":{"display":"flex","gridColumn":"2/3","gridRow":"1/2"}},"children":[{"type":"reference","settings":{"gridTemplateColumns":[],"gridTemplateRows":[],"titleName":"申请人","validation":{"required":false}},"sourceSchema":{"id":"udcReference_uDr2"},"dataSource":{"dataField":"applicant"}}],"sourceSchema":{"id":"udcGridCell_yP5z"}}]
     * @param cellColRow    网格位置信息. 示例: {"rowIndex":1,"flexRowSize":1,"colIndex":2,"flexColSize":1}
     * @param firstGridCellId    gridCell样式id: 示例: udcGridCell_Dv65
     * @param firstGridCellComponentId gridCell组件id: 示例:label_v2bx
     *
     */
    public static void createComplexComponentGridCellNode(JsonNode component,ArrayNode gridChildren, JsonNode cellColRow,String firstGridCellId,String firstGridCellComponentId,
                                                          String secondGridCellId,String secondGridCellComponentIdTemp,String normalGridCellId,String normalComponentId) {

        boolean isComponentLabel = false;
        boolean isNormalLabel = false;
        if(SchemaTransformerUtil.isLabelOrFieldTitle(component.get("type").asText())) {
            if(component.has("dataSource") && component.get("dataSource").has("dataField")) {
                isComponentLabel = true;
            } else {
                isNormalLabel = true;
            }

        }
        // ===============创建label的gridCell===============
        if(isComponentLabel){
            createLabelGridCellNode(component, gridChildren, cellColRow, firstGridCellId, firstGridCellComponentId,true);
        } else if(isNormalLabel){
            createLabelGridCellNode(component, gridChildren, cellColRow, normalGridCellId, normalComponentId,true);//正常label 使用偶数列的网格背景色等样式信息
        } else {
            // ===============创建组件的gridCell===============
            createComponentGridCellNode(component, gridChildren, cellColRow, secondGridCellId, secondGridCellComponentIdTemp,true);
        }


    }

    /**
     * 创建label的gridCell
     *
     * @param component ocr DSL. 示例 : {"settings":{"gridTemplateColumns":[],"gridTemplateRows":[],"titleName":"申请部门","validation":{"required":false}},"type":"reference","ocrRelationId":"6bdf5a11-ea54-4b6e-94e5-b74414719c04","dataSource":{"dataField":"shenqingbumen"},"cellColRow":{"rowIndex":1,"flexRowSize":1,"colIndex":2,"flexColSize":1}}
     * @param gridChildren 各group, 页面dsl 示例: [{"type":"gridCell","settings":{"styles":{"display":"flex","gridColumn":"1/2","gridRow":"1/2"}},"children":[{"type":"label","settings":{"content":"申请人"},"sourceSchema":{"id":"label_v2bx"},"dataSource":{"dataField":"applicant"}}],"sourceSchema":{"id":"udcGridCell_Dv65"}},{"type":"gridCell","settings":{"styles":{"display":"flex","gridColumn":"2/3","gridRow":"1/2"}},"children":[{"type":"reference","settings":{"gridTemplateColumns":[],"gridTemplateRows":[],"titleName":"申请人","validation":{"required":false}},"sourceSchema":{"id":"udcReference_uDr2"},"dataSource":{"dataField":"applicant"}}],"sourceSchema":{"id":"udcGridCell_yP5z"}}]
     * @param cellColRow    网格位置信息. 示例: {"rowIndex":1,"flexRowSize":1,"colIndex":2,"flexColSize":1}
     * @param gridCellId    gridCell样式id: 示例: udcGridCell_Dv65
     * @param gridCellComponentId gridCell组件id: 示例:label_v2bx
     * @param isComplex 是否是复杂组件. 如果是true,则认为入参component中,已经对组件拆解成label和component
     */
    public static void createLabelGridCellNode(JsonNode component,ArrayNode gridChildren, JsonNode cellColRow,String gridCellId,String gridCellComponentId, boolean isComplex) {

        // 调整: 如果组件设置了必填(required=true)，则在标题前添加星号(*)
        if (component.has("settings") && component.get("settings").has("validation")
                && component.get("settings").get("validation").has("required")
                && component.get("settings").get("validation").get("required").asBoolean()) {

            JsonNode settingsNode2 = component.get("settings");
            if (settingsNode2.has("titleName")) {
                String titleName = settingsNode2.get("titleName").asText();
                if (!titleName.startsWith("*")) {
                    ((ObjectNode)settingsNode2).put("titleName", "*" + titleName);
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        int rowIndex = cellColRow.get("rowIndex").asInt();
        int flexRowSize = cellColRow.get("flexRowSize").asInt();
        int colIndex = cellColRow.get("colIndex").asInt();
        int flexColSize = cellColRow.get("flexColSize").asInt();
        boolean isDisplay = true;
        if(cellColRow.has("display")) {
            String display = cellColRow.get("display").asText();
            if("none".equals(display)) {
                //单独处理空白gridCell
                isDisplay = false;
            }

        }

        ObjectNode labelCell = gridChildren.addObject();
        labelCell.put("type", "gridCell");
        ObjectNode labelCellSettings = labelCell.putObject("settings");

        // 添加label组件
        ArrayNode labelChildren = labelCell.putArray("children");
        ObjectNode label = null;
        if(isDisplay) {
            label = labelChildren.addObject();
            label.put("type", "label");
            ObjectNode labelSettings = label.putObject("settings");
            JsonNode settingsNode = component.get("settings");
            String titleName = "";
            if(settingsNode != null){
                JsonNode titleNameNode = settingsNode.get("titleName");

                if(titleNameNode != null){
                    titleName = titleNameNode.asText();
                }
            }
            labelSettings.put("content", titleName);

        }
        // 添加labelCell的styles
//        JsonNode labelStyles = labelCell.get("settings");

        ObjectNode settingsStyles = mapper.createObjectNode();
//        ObjectNode labelGridCellSettings = mapper.createObjectNode();
        int colStart = (colIndex*2-1);
        int colEnd = (colIndex*2-1) + 1;
        int rowStart = rowIndex;
        int rowEnd = rowIndex + flexRowSize;
        settingsStyles.put("display", "flex");

        if(isComplex){
            settingsStyles.put("gridColumn", colIndex + "/" +  (colIndex + flexColSize));
        }else {
            settingsStyles.put("gridColumn", colStart + "/" + colEnd);
        }

        settingsStyles.put("gridRow", rowStart + "/" + rowEnd);
        if(!isDisplay){
            settingsStyles.put("display", "none");
        }
        // 修改这里: 将styles添加到labelCell的settings中
        labelCellSettings.put("styles", settingsStyles);



        // 设置label的sourceSchema.id
        if (gridCellId != null) {
            labelCell.putObject("sourceSchema").put("id", gridCellId);
        }
        if (gridCellComponentId != null && isDisplay) {
            label.putObject("sourceSchema").put("id", gridCellComponentId);
        }

        // 复制dataSource到label
        if (component.has("dataSource") && isDisplay) {
            label.set("dataSource", component.get("dataSource"));
        }
//        return labelCell;
    }
    public static void createComponentGridCellNode(JsonNode component,ArrayNode gridChildren, JsonNode cellColRow,String gridCellId,String gridCellComponentId, boolean isComplex) {
        ObjectMapper mapper = new ObjectMapper();
        int rowIndex = cellColRow.get("rowIndex").asInt();
        int flexRowSize = cellColRow.get("flexRowSize").asInt();
        int colIndex = cellColRow.get("colIndex").asInt();
        int flexColSize = cellColRow.get("flexColSize").asInt();

        int colStart = (colIndex*2-1);
        int colEnd = (colIndex*2-1) + 1;
        int rowStart = rowIndex;
        int rowEnd = rowIndex + flexRowSize;

        boolean isDisplay = true;
        if(cellColRow.has("display")) {
            String display = cellColRow.get("display").asText();
            if("none".equals(display)) {
                //单独处理空白gridCell
                isDisplay = false;
            }

        }

        ObjectNode gridCellNode = gridChildren.addObject();
        gridCellNode.put("type", "gridCell");
        ObjectNode gridCellSettingsNode = gridCellNode.putObject("settings");

        // 添加实际组件
        ArrayNode componentChildren = gridCellNode.putArray("children");
        ObjectNode actualComponent = null;
        String componentType = component.get("type").asText();

        if(isDisplay && !"gridCell".equalsIgnoreCase(componentType)) {
            actualComponent = componentChildren.addObject();
            actualComponent.put("type", componentType);
            // 复制组件的settings
            if (component.has("settings")) {
                ObjectNode componentSettings = actualComponent.putObject("settings");
                component.get("settings").fields().forEachRemaining(field ->
                        componentSettings.set(field.getKey(), field.getValue()));
            }
        }
        // 添加组件的styles
        ObjectNode gridCellSettingsStyles = mapper.createObjectNode();

        int componentColStart = colStart + 1;
        int componentColEnd = (colStart + 2) + ((flexColSize - 1) * 2);
        gridCellSettingsStyles.put("display", "flex");
        gridCellSettingsStyles.put("gridRow", rowStart + "/" + rowEnd);

        if(isComplex){
            gridCellSettingsStyles.put("gridColumn", colIndex + "/" + (colIndex + flexColSize));
        }else {
            gridCellSettingsStyles.put("gridColumn", componentColStart + "/" + componentColEnd);
        }

        if(!isDisplay){
            gridCellSettingsStyles.put("display", "none");
        }
        // 修改这里：将styles添加到componentSettings中
        gridCellSettingsNode.set("styles", gridCellSettingsStyles);



        // 设置组件的sourceSchema.id
        if (actualComponent != null && gridCellId != null) {
            gridCellNode.putObject("sourceSchema").put("id", gridCellId);
        }
        if (actualComponent != null && gridCellComponentId != null && isDisplay) {
            actualComponent.putObject("sourceSchema").put("id", gridCellComponentId);
        }

        // 复制dataSource到组件
        if (component.has("dataSource") && isDisplay) {
            actualComponent.set("dataSource", component.get("dataSource"));
        }
    }

    /**
     * 根据collapse节点的结构特征，选择合适的转换方法
     * @param sourceGroup  布局结构 如: {"name":"分组","type":"collapse","settings":{"title":"分组1"}}
     * @param templateNode 模版结构模板 如: {"type":"collapse","settings":{},"sourceSchema":{"id":"collapse_lNmB"},"id":"MzUwOTEzYzE"}
     * @return 转换后的结构 如: {"type":"collapse","settings":{"title":"分组1"},"sourceSchema":{"id":"collapse_lNmB"},"id":"MzUwOTEzYzE"}
     */
    public static JsonNode convertGroupByLayout(JsonNode sourceGroup, JsonNode templateNode) {

        Map<String,String> collectTypeAndSourceMap = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(templateNode);

        boolean isComplexGridCell = false;
        boolean isSpliteLabel = false;
        String gridType = "";
        if (sourceGroup.has("settings") && sourceGroup.get("settings").has("gridType") && StringUtils.isNotBlank(sourceGroup.get("settings").get("gridType").asText())) {
            gridType = sourceGroup.get("settings").get("gridType").asText();
        }
        if("complex".equals(gridType)){
            isComplexGridCell = true;
        }

        String groupType = null;
        if(templateNode == null){
            groupType = "stream";
        } else {
            groupType = determineLayoutType(templateNode);
        }

        // 1. 如果没有grid节点，使用流式布局

        if ("stream".equals(groupType)) {
            if(isComplexGridCell){
                throw new RuntimeException("ocr识别的gridType的网格布局不能套用到流式布局模版!");
            }
            log.info("使用流式布局转换");
            return convertGroupStreamStructure(sourceGroup, templateNode,collectTypeAndSourceMap);
        }


        // 2. 如果gridCell的children中第一个组件是label，使用label加组件网格布局
        else if ("gridLabelCell".equals(groupType)) {
            isSpliteLabel = true;
            log.info("[complex]使用label加组件网格布局转换");
            return convertComplexGroupCellTo2CellStructure(sourceGroup, templateNode, collectTypeAndSourceMap,isComplexGridCell);
        }

        // 3. 如果gridCell的children中第一个组件不是label，使用组件网格布局 (兼容 精确布局 "complex".equals(gridType))
        else if ("gridCell".equals(groupType)) {
            log.info("使用组件网格布局转换");
            return convertGroupToGridStructure(sourceGroup, templateNode, collectTypeAndSourceMap);
//            return convertComplexGroupCellTo2CellStructure(sourceGroup, templateNode, collectTypeAndSourceMap,isSpliteLabel,isComplexGridCell);
        } else {
            throw new RuntimeException("不支持的group类型:"+groupType);
        }
    }

    /**
     * 判断布局类型
     * @param jsonNode 要判断的节点
     * @return 布局类型: stream/gridLabelCell/gridCell/grid_stream/grid_gridLabelCell/grid_gridCell
     */
    private static String determineLayoutType(JsonNode jsonNode) {

            // 判断是否为流式布局
            boolean hasContainer = false;
            boolean hasGrid = false;

        NodePosition mostFrequentNodePosition = SchemaTransformerUtil.getMostFrequentNodePosition(jsonNode);
//        if(mostFrequentNodePosition != null ){
            ObjectNode mostFrequentNode = mostFrequentNodePosition.getObjectNode();
            if (mostFrequentNode != null && mostFrequentNode.has("type")) {
                 String mostFreqType = mostFrequentNode.get("type").asText();
                 if("container".equals(mostFreqType)){
                     return "stream";
                 }
            }

//        }

        // 递归检查是否存在 grid 和 container
            hasGrid = SchemaTransformerUtil.checkNodeTypeExists(jsonNode, "grid");
            hasContainer = SchemaTransformerUtil.checkNodeTypeExists(jsonNode, "container");

            // 如果不存在 grid 且存在 container，则为流式布局
            if (!hasGrid && hasContainer) {
                return "stream";
            }

            // 检查是否存在 gridCell
            boolean hasGridCell = SchemaTransformerUtil.checkNodeTypeExists(mostFrequentNode, "gridCell");
            if (hasGridCell) {
                List<NodePosition> gridCell = SchemaTransformerUtil.getAllTypeNodeInfo(mostFrequentNode, "gridCell");
                if(gridCell.size() > 1){
                    NodePosition firstNodePosition = getNodeInNodePositions(gridCell,1,1);
                    NodePosition secondNodePosition =getNodeInNodePositions(gridCell,1,2);
                    ObjectNode secondGridCellNode = secondNodePosition.getObjectNode();
                    boolean isFirstNodeLabel = SchemaTransformerUtil.checkNodeTypeExists(firstNodePosition.getObjectNode(), "label");
                    boolean isFirstNodeFieldTitle = SchemaTransformerUtil.checkNodeTypeExists(firstNodePosition.getObjectNode(), "fieldTitle");
                    // 如果第一个子节点的类型是 label，则为 gridLabelCell 布局
                    if(isFirstNodeLabel || isFirstNodeFieldTitle) {
                        if (secondGridCellNode != null && secondGridCellNode.has("children") && secondGridCellNode.get("children").isArray()) {
                            JsonNode secondGridCellChildNode = secondGridCellNode.get("children");
                            if (secondGridCellChildNode != null && secondGridCellChildNode.size() > 0) {
                                JsonNode secondChild = secondGridCellChildNode.get(0);
                                // 如果第二个子节点的类型是 基础类型，则为 gridLabelCell 布局
                                Set<String> entityOptionTypes = SchemaTransformerUtil.getEntityOptionTypes();
                                if (secondChild != null && secondChild.has("type") ){
                                    String typeText = secondChild.get("type").asText();
                                    boolean isLabelOrFieldTitle = SchemaTransformerUtil.isLabelOrFieldTitle(typeText);
                                    if(!isLabelOrFieldTitle){
                                        return "gridLabelCell";
                                    }

                                }
                            }

                        }
                    }


                }
            }

            // 默认返回 gridCell
            return "gridCell";

    }

    public static NodePosition getNodeInNodePositions(List<NodePosition> gridCells,int row, int col){
        for (NodePosition gridCellPositon : gridCells) {
            ObjectNode gridCellNode = gridCellPositon.getObjectNode();
            JsonNode settingsNode = gridCellNode.get("settings");
            if (settingsNode != null && settingsNode.has("styles") ) {
                JsonNode stylesNode = settingsNode.get("styles");
                if(stylesNode.has("gridColumn") && stylesNode.has("gridRow")){
                    String gridColumn = stylesNode.get("gridColumn").asText();
                    String gridRow = stylesNode.get("gridRow").asText();
                    if(gridColumn.startsWith(col+"/") && gridRow.startsWith(row+"/")){
                        return gridCellPositon;
                    }

                }
            }
        }

        return new NodePosition();
    }


    /**
     * 处理多行组件，为flexRowSize>1的节点插入额外行
     * @param components 需要处理的组件数组
     */
    public static void handleMultiRowComponents(ArrayNode components) {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> nodesToAdd = new ArrayList<>();
        
        for (JsonNode component : components) {
            JsonNode cellColRow = component.get("cellColRow");

            if (cellColRow != null && cellColRow.has("flexRowSize") && cellColRow.has("flexColSize")) {
                int flexColSize = cellColRow.get("flexColSize").asInt();
                int flexRowSize = cellColRow.get("flexRowSize").asInt();
                int rowIndex = cellColRow.get("rowIndex").asInt();
                int colIndex = cellColRow.get("colIndex").asInt();
                for(int i= 0 ; i<flexRowSize; i++){
                    for(int j= 0; j<flexColSize; j++){
                        int currRowIndex = rowIndex + i;
                        int currColIndex = colIndex + j;
                        if(rowIndex==currRowIndex && colIndex==currColIndex){

                        }else{
                            ObjectNode newNode = mapper.createObjectNode();

                            // 复制基本结构
                            newNode.put("type", "gridCell");
                            newNode.putObject("settings");

                            // 复制dataSource
                            if (component.has("dataSource")) {
                                newNode.set("dataSource", component.get("dataSource").deepCopy());
                            }

                            // 创建cellColRow
                            ObjectNode newCellColRow = newNode.putObject("cellColRow");
                            newCellColRow.put("display","none");
                            newCellColRow.put("rowIndex", currRowIndex);
                            newCellColRow.put("flexRowSize", 1);
                            newCellColRow.put("colIndex", currColIndex);
                            newCellColRow.put("flexColSize", 1);

                            // 收集需要添加的节点
                            nodesToAdd.add(newNode);
                        }

                    }
                }

            }
        }
        
        // 将收集到的所有新节点添加到原始数组中
        for (JsonNode node : nodesToAdd) {
            components.add(node);
        }
    }

    /**
     * 修复网格行数，确保gridTemplateRows的数量与实际占用的行数相匹配
     * @param group 需要修复的JSON节点
     */
    public static void fixGridTemplateRows(JsonNode group) {
//        if (jsonNode == null || !jsonNode.has("groups")) {
//            return;
//        }

//        ArrayNode groups = (ArrayNode) jsonNode.get("groups");
//        for (JsonNode group : groups) {
            if (!group.has("settings") || !group.has("components")) {
                return;
            }

            ObjectNode settings = (ObjectNode) group.get("settings");
            ArrayNode components = (ArrayNode) group.get("components");
            ArrayNode gridTemplateRows = (ArrayNode) settings.get("gridTemplateRows");
            
            if (gridTemplateRows == null) {
                return;
            }

            // 计算所有组件的flexRowSize之和
            int totalFlexRowSize = 0;
            Set<String> usedRow = new HashSet<>();
            for (JsonNode component : components) {
                JsonNode cellColRow = component.get("cellColRow");
                String rowIndexValue = cellColRow.get("rowIndex").asText();
                if (component.has("cellColRow") && !usedRow.contains(rowIndexValue)) {

                    totalFlexRowSize += cellColRow.get("flexRowSize").asInt();
                    usedRow.add(rowIndexValue);
                }

            }

            // 如果gridTemplateRows的数量等于flexRowSize之和，则不需要处理
            if (gridTemplateRows.size() == totalFlexRowSize) {
                return;
            }
            
            // 以下是原有的处理逻辑
            // 1. 计算实际需要的总行数
            int maxRowIndex = 0;
            for (JsonNode component : components) {
                if (component.has("cellColRow")) {
                    JsonNode cellColRow = component.get("cellColRow");
                    int rowIndex = cellColRow.get("rowIndex").asInt();
                    int flexRowSize = cellColRow.get("flexRowSize").asInt();
                    maxRowIndex = Math.max(maxRowIndex, rowIndex + flexRowSize - 1);
                }
            }

            // 3. 创建新的gridTemplateRows数组
            ArrayNode newGridTemplateRows = JsonNodeFactory.instance.arrayNode();
            
            // 遍历每一行，检查是否需要拆分
            for (int i = 0; i < gridTemplateRows.size(); i++) {
                double value = gridTemplateRows.get(i).asDouble();
                
                // 检查这一行是否有组件占用多行
                boolean hasMultiRow = false;
                int additionalRows = 0;
                
                for (JsonNode component : components) {
                    if (component.has("cellColRow")) {
                        JsonNode cellColRow = component.get("cellColRow");
                        int rowIndex = cellColRow.get("rowIndex").asInt();
                        int flexRowSize = cellColRow.get("flexRowSize").asInt();
                        
                        // 如果组件从当前行开始且占用多行
                        if (rowIndex == i + 1 && flexRowSize > 1) {
                            hasMultiRow = true;
                            additionalRows = flexRowSize - 1;
                            break;
                        }
                    }
                }
                
                if (hasMultiRow) {
                    // 将多行值平均分配
                    double splitValue = value / (additionalRows + 1);
                    for (int j = 0; j <= additionalRows; j++) {
                        newGridTemplateRows.add(splitValue);
                    }
                } else {
                    newGridTemplateRows.add(value);
                }
            }
            
            // 4. 更新settings中的gridTemplateRows
            settings.set("gridTemplateRows", newGridTemplateRows);
//        }
    }

}
