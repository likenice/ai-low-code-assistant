package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import com.seeyon.ai.schematransformer.dto.OcrGroupType;
import com.seeyon.ai.schematransformer.util.JsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import com.seeyon.boot.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * 将udcSchema -> dslSchema
 */
@Slf4j
public class SchemaTransformer {

    @Data
    public static class SchemaTransformResult {
        private NodePosition nodePosition;
        private ArrayNode layoutList;
        private boolean isDeleteTabsNode = false;
    }

    private static String transformType(String type) {
        if (type == null) {
            return type;
        }
        
        String newType = type;
        
        // 如果以Udc开头，去掉Udc前缀
        if (type.startsWith("Udc")) {
            newType = type.substring(3);
        } else if (type.equalsIgnoreCase("page")) {
            newType = "page";
        }
        
        // 首字母转小写（对所有type都处理）
        return Character.toLowerCase(newType.charAt(0)) + newType.substring(1);
    }

    /**
     * 递归处理节点及其子节点，将UDC schema转换为DSL schema
     * 
     * @param parent 当前处理的源节点
     * @param parentNode 转换后的目标节点
     * @param udcSchema 完整的UDC schema，用于查找子节点
     */
    private static void processChildren(JsonNode parent, ObjectNode parentNode, JsonNode udcSchema) {
        parentNode.put("id", SchemaTransformerUtil.generateShortUUID());
        // 设置转换后的type
        if (parent.has("type")) {
            
            String type = parent.get("type").asText();
            parentNode.put("type", transformType(type));
            
            // 增加name属性
            if (parent.has("name")) {
                parentNode.put("name", parent.get("name").asText());
            }
        }
        
        // 处理不同类型节点的特殊逻辑
        if (parent.has("type")) {
            String type = parent.get("type").asText();
            switch (type) {
                case "UdcForm":
                    processUdcForm(parent, parentNode, udcSchema);
                    break;
                case "UdcDataGrid":
                    processUdcDataGrid(parent, parentNode, udcSchema);
                    break;
                case "UdcGrid":
                    processUdcGrid(parent, parentNode, udcSchema);
                    break;  
                default:
                    processDefaultNode(parent, parentNode, udcSchema);
                    break;
            }
        }
    }

    /**
     * 处理UdcForm类型节点
     */
    private static void processUdcForm(JsonNode parent, ObjectNode parentNode, JsonNode udcSchema) {
        // 将 UdcForm 转换为 form 类型
        parentNode.put("type", "form");
        
        // 处理 dataSource
        JsonNode dataSource = parent.get("dataSource");
        if (dataSource != null && "ENTITY".equals(dataSource.get("type").asText())) {
            JsonNode entity = dataSource.get("entity");
            if (entity != null && entity.get("name") != null) {
                ObjectNode dataSourceNode = parentNode.putObject("dataSource");
                dataSourceNode.put("type", "ENTITY");
                dataSourceNode.put("entityName", entity.get("name").asText());
                dataSourceNode.put("entityFullName", entity.get("fullName").asText());
            }
           
        }
        if (parent.get("children") != null && parent.get("children").isArray() 
                && parent.get("children").size() > 0) {
            ArrayNode childrenArray = parentNode.putArray("children");
            
            // 遍历子元素ID数组
            for (JsonNode childId : parent.get("children")) {
                processChildNode(childId.asText(), childrenArray, udcSchema);
            }
        }
    }

    /**
     * 处理UdcDataGrid类型节点
     */
    private static void processUdcDataGrid(JsonNode parent, ObjectNode parentNode, JsonNode udcSchema) {
        JsonNode extraChildren = parent.get("extraChildren");
        if (extraChildren != null && extraChildren.get("columnChildren") != null) {
            ArrayNode childrenArray = parentNode.putArray("children");
            
            // 遍历columnChildren数组
            for (JsonNode childId : extraChildren.get("columnChildren")) {
                processChildNode(childId.asText(), childrenArray, udcSchema);
            }
        }

    }

    /**
     * 处理UdcGrid类型节点
     */
    private static void processUdcGrid(JsonNode parent, ObjectNode parentNode, JsonNode udcSchema) {
        JsonNode extraChildren = parent.get("extraChildren");
        JsonNode extraConfig = parent.get("extraConfig");
        
        if (extraChildren != null && extraConfig != null) {
            JsonNode parentSettings = parent.get("settings");
            //设置settings-> gridTemplateColumns
            // if(parent.get("settings") != null && parent.get("settings").get("gridTemplateColumns") != null) {
            //     JsonNode gridTemplateColumns = parent.get("settings").get("gridTemplateColumns");
            //     ObjectMapper mapper = new ObjectMapper();
            //     ObjectNode settingsNode = mapper.createObjectNode();
            //     settingsNode.put("gridTemplateColumns", gridTemplateColumns);

            //     SchemaTransformerUtil.insertNodeProps(templateSettingsNode, settings, Arrays.asList("alignment","contentFontFamily"));
            //     SchemaTransformerUtil.insertComplexNodeProps(templateSettingsNode, settings, Arrays.asList("bgColor","textColor","textBgColor"));

            //     parentNode.set("settings", settingsNode);
            // }
            ObjectNode settingsNode = parentNode.putObject("settings");
            SchemaTransformerUtil.insertNodeProps( parentSettings,settingsNode, Arrays.asList("gridTemplateColumns","gridTemplateRows","rows"));




            ArrayNode childrenArray = parentNode.putArray("children");
            
            // 遍历extraChildren的所有属性（gridCell）
            extraChildren.fields().forEachRemaining(entry -> {
                processGridCell(entry, extraConfig, childrenArray, udcSchema, parent);
            });
        }
    }


    /**
     * 处理默认类型节点
     */
    private static void processDefaultNode(JsonNode parent, ObjectNode parentNode, JsonNode udcSchema) {
        if (parent.get("children") != null && parent.get("children").isArray() 
                && parent.get("children").size() > 0) {
            ArrayNode childrenArray = parentNode.putArray("children");
            
            // 遍历子元素ID数组
            for (JsonNode childId : parent.get("children")) {
                processChildNode(childId.asText(), childrenArray, udcSchema);
            }
        }
    }

    /**
     * 处理单个子节点
     */
    private static void processChildNode(String childId, ArrayNode childrenArray, JsonNode udcSchema) {
        // 添加对udcSchema的空值检查
        if (udcSchema == null || udcSchema.get("childById") == null) {
            return;
        }
        
        JsonNode child = udcSchema.get("childById").get(childId);
        if (child != null) {
            ObjectNode childNode = childrenArray.addObject();
            String type = transformType(child.get("type").asText());
            childNode.put("type", type);
            ObjectNode settings = childNode.putObject("settings");

            //如果存在dataField
            if(child.has("dataSource") && child.get("dataSource").has("dataField")){
                ObjectNode dataSourceNode = childNode.putObject("dataSource");
                String sourceDataFieldString = child.get("dataSource").get("dataField").asText();
                dataSourceNode.put("dataField", sourceDataFieldString);
                if(child.get("dataSource").has("fullName")) {
                    String fullNameStr = child.get("dataSource").get("fullName").asText();
                    String entityFullName = JsonUtil.getEntityFullNameByFieldFullName(fullNameStr);
                    dataSourceNode.put("entityFullName", entityFullName);
                }
            }


            if (SchemaTransformerUtil.isLabelOrFieldTitle(type)) {
                if(child.has("settings") ) {
                    if(child.get("settings").has("textFontSize")) {
                        String textFontSize = "";
                        JsonNode textFontSizeNode = child.get("settings").get("textFontSize");
                        if (textFontSizeNode != null) {
                            textFontSize = textFontSizeNode.asText();

                        }else if (textFontSizeNode.has("simple")) {
                            textFontSize = textFontSizeNode.get("simple").asText();
                        }else {
                            throw new RuntimeException("id:"+child.get("id")+"  textFontSize is not match!");
                        }

                        //判断textFontSize是否数字
                        if (StringUtils.isNumeric(textFontSize)) {
                            int fontSize = Integer.parseInt(textFontSize);
                            settings.put("textFontSize", fontSize);
                        } else if(textFontSize.startsWith("font-") ){
                            String textFontSizeIndex = textFontSize.replace("font-", "");
                            if (StringUtils.isNumeric(textFontSizeIndex)) {
                                int fontSize = Integer.parseInt(textFontSizeIndex);
                                settings.put("textFontSize", 10+fontSize*2);
                            }

                        }
                    }
                    if(child.get("settings").has("textStyle")) {
                        String fontStyle = child.get("settings").get("textStyle").get("fontStyle").asText();
                        if (StringUtils.isNotBlank(fontStyle)) {
                            settings.put("textFontStyle", fontStyle);
                        }
                    }
                    
                    // 添加对stylesNode的空值检查
                    JsonNode templateSettingsNode = child.has("settings") ? child.get("settings") : null;
                    if (templateSettingsNode != null) {
                        SchemaTransformerUtil.insertNodeProps(templateSettingsNode, settings, Arrays.asList("alignment","contentFontFamily"));
                        SchemaTransformerUtil.insertComplexNodeProps(templateSettingsNode, settings, Arrays.asList("bgColor","textColor","textBgColor"));
                    }
                }
            }
            
            childNode.putObject("sourceSchema").put("id", child.get("id").asText());
            
            // 递归处理该子节点的子元素
            processChildren(child, childNode, udcSchema);
        }
    }

    private static void processGridCell(Entry<String, JsonNode> entry, JsonNode extraConfig, 
            ArrayNode childrenArray, JsonNode udcSchema, JsonNode parent) {
        String cellId = entry.getKey();
        JsonNode cellConfig = extraConfig.get(cellId);
        
        if (cellConfig != null) {
            ObjectNode cellNode = childrenArray.addObject();
            cellNode.put("id", SchemaTransformerUtil.generateShortUUID());
            cellNode.put("type", transformType(cellConfig.get("type").asText()));
            ObjectNode settingsNode = cellNode.putObject("settings");
            ObjectNode settingsStylesNode = settingsNode.putObject("styles");
            cellNode.putObject("sourceSchema").put("id", cellConfig.get("id").asText());
            
            // 检查settings->styles->gridColumn是否为合并一整行
            if (cellConfig.has("settings") && cellConfig.get("settings").has("styles") 
                    && cellConfig.get("settings").get("styles").has("gridColumn")) {
                String gridColumn = cellConfig.get("settings").get("styles").get("gridColumn").asText();
                JsonNode gridCellStylesNode = cellConfig.get("settings").get("styles");

                SchemaTransformerUtil.insertNodeProps(gridCellStylesNode, settingsStylesNode, Arrays.asList("borderBottom","borderLeft","borderRight","borderTop","display","gridColumn","gridRow"));
                SchemaTransformerUtil.insertComplexNodeProps(gridCellStylesNode, settingsStylesNode, Arrays.asList("bgColor"));


                // 获取父节点的gridTemplateColumns，计算总列数
                if (parent != null && parent.has("settings") 
                        && parent.get("settings").has("gridTemplateColumns")) {
                    JsonNode gridTemplateColumns = parent.get("settings").get("gridTemplateColumns");
                    if (gridTemplateColumns.isArray()) {
                        int totalColumns = gridTemplateColumns.size();
                        // 如果gridColumn是"1/{总列数+1}"格式，表示合并一整行
                        String expectedGridColumn = "1/" + (totalColumns + 1);
                        if (gridColumn.equals(expectedGridColumn)) {
                            settingsNode.put("isMergeAllColumns", true);
                        }
                    }
                }

            }
            
            // 处理该GridCell中的子元素
            ArrayNode cellChildrenArray = cellNode.putArray("children");
            JsonNode cellChildren = entry.getValue();
            if (cellChildren.isArray()) {
                for (JsonNode childId : cellChildren) {
                    processChildNode(childId.asText(), cellChildrenArray, udcSchema);
                }
            }
        }
    }

 
     /**
      * //遍历dslSchema所有节点和子节点 ,第一个遇到属性type=collapse或type=dataGrid 的节点. 删除这个节点. 并用layoutList插入这个位置. 
        //继续遍历删除所有的type=collapse和type=dataGrid的所有节点
        // 遍历template并处理节点
      * @param dslSchema
      * @param layoutList
      * @param hasProcessedFirstCollapse
      * @return
      */
     private static JsonNode processTemplateNodes(JsonNode dslSchema, List<JsonNode> layoutList, Boolean hasProcessedFirstCollapse) {
        if (dslSchema == null) {
            return null;
        }

        // 如果是文本节点或其他简单类型节点，直接返回
        if (!dslSchema.isContainerNode()) {
            return dslSchema;
        }

        if (hasProcessedFirstCollapse == null) {
            hasProcessedFirstCollapse = false;
        }

        // 处理对象节点
        if (dslSchema.isObject()) {
            ObjectNode objNode = (ObjectNode) dslSchema;
            
            // 处理子节点
            if (objNode.has("children") && objNode.get("children").isArray()) {
                ArrayNode children = (ArrayNode) objNode.get("children");
                for (int i = children.size() - 1; i >= 0; i--) {
                    JsonNode child = children.get(i);
                    
                    if (child.has("type")) {
                        String type = child.get("type").asText();
                        
                        // 处理collapse节点
                        if ("collapse".equals(type)) {
                            if (!hasProcessedFirstCollapse) {
                                // 第一个collapse节点，替换内容
                                if (child.isObject()) {
                                    ObjectNode collapseNode = (ObjectNode) child;
                                    if (collapseNode.has("children")) {
                                        ArrayNode collapseChildren = (ArrayNode) collapseNode.get("children");
                                        collapseChildren.removeAll();
                                        for (JsonNode layoutNode : layoutList) {
                                            collapseChildren.add(layoutNode);
                                        }
                                    }
                                }
                                hasProcessedFirstCollapse = true;
                            } else {
                                // 其他collapse节点，直接删除
                                children.remove(i);
                            }
                        }
                        // 处理dataGrid节点，直接删除
                        else if ("dataGrid".equals(type)) {
                            children.remove(i);
                        }
                    }
                }
            }

            // 递归处理所有字段
            ObjectNode resultNode = objNode.deepCopy();
            java.util.Iterator<Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> entry = fields.next();
                JsonNode processedValue = processTemplateNodes(entry.getValue(), layoutList, hasProcessedFirstCollapse);
                if (processedValue != null) {
                    resultNode.set(entry.getKey(), processedValue);
                }
            }
            return resultNode;
        } 
        // 处理数组节点
        else if (dslSchema.isArray()) {
            ArrayNode arrayNode = (ArrayNode) dslSchema;
            ArrayNode resultArray = arrayNode.deepCopy();
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode processedElement = processTemplateNodes(arrayNode.get(i), layoutList, hasProcessedFirstCollapse);
                if (processedElement != null) {
                    resultArray.set(i, processedElement);
                }
            }
            return resultArray;
        }

        return dslSchema;
    }

    public static ObjectNode convertUdc2Dsl(JsonNode template){
        ObjectMapper mapper = new ObjectMapper();

        //====将template 转为 dslSchema====
        //====将udcSchema转换为dslSchema.====
        ObjectNode dslSchema = mapper.createObjectNode();
//        try {
        dslSchema.put("id", SchemaTransformerUtil.generateShortUUID());
        dslSchema.put("type", "page");
        dslSchema.put("pageType",template.get("pageType")==null ? "":template.get("pageType").asText());
        dslSchema.put("urlType", template.get("urlType") == null ? "": template.get("urlType").asText());

        // 设置 settings
        ObjectNode settings = dslSchema.putObject("settings");
        settings.put("dataFrom", "ocr");

        // 设置 sourceSchema
        ObjectNode sourceSchema = dslSchema.putObject("sourceSchema");
        sourceSchema.put("id", template.get("id") == null ? "": template.get("id").asText());

        // 处理 children
        processChildren(template, dslSchema, template);
        return dslSchema;
    }

    /**
     * udc模版和ocr图片 转换为dslSchema 入口方法
     *
     */
    public static JsonNode convertLayoutByTemplate(JsonNode layoutSchema, JsonNode template) {

        //layoutSchema gridCell数据修正(因为人工介入时,会破坏网格结构)
        JsonNode groupsNode = layoutSchema.get("groups");
        if (groupsNode != null && groupsNode.isArray()) {
            for (JsonNode groupNode : groupsNode) {
                SchemaTransformerJsonUtil.fixComponents((ObjectNode) groupNode);
            }
        }
        ObjectNode templateDslSchema = convertUdc2Dsl(template);
        return convertLayoutByDslSchema(layoutSchema,templateDslSchema);
    }

     /**
     * udc模版和ocr图片 转换为dslSchema 入口方法
     * 
     */
    public static JsonNode convertLayoutByDslSchema(JsonNode layoutSchema,ObjectNode templateDslSchema) {

        //如果没有"表头"补全表头
        SchemaTransformerBase.initTitleNodeName(templateDslSchema);

        //====layoutSchema 与 dslSchema合并成  转换后的JsonNode====

        //设置dslSchema的标题名称 
        String titleName = null;
        JsonNode titleJsonNode = layoutSchema.get("titleName");
        if (titleJsonNode != null) {
            titleName = titleJsonNode.asText();
            SchemaTransformerBase.updateTitleNode(templateDslSchema, titleName);
        }

        //设置实体名称
        String entityName = null;
        JsonNode dataSourceJsonNode = layoutSchema.get("dataSource");
        if (dataSourceJsonNode != null) {
            JsonNode entityNameNode = dataSourceJsonNode.get("entityName");
            entityName = entityNameNode.asText();
            SchemaTransformerBase.updateFormNode(templateDslSchema, entityName);
        }


        //====将layoutSchema, 模版Schema 转为 dslSchema====
        SchemaTransformResult schemaTransformResult = convertTabsLayoutByDslTemplate(layoutSchema, templateDslSchema);


        ArrayNode layoutList = schemaTransformResult.getLayoutList();
        NodePosition insertCollapsePositionNode = schemaTransformResult.getNodePosition();
        // 如果layoutList不为空,处理dslSchema
        if (!layoutList.isEmpty()) {

            if(insertCollapsePositionNode != null){
                SchemaTransformerUtil.deleteNodePosition(templateDslSchema,insertCollapsePositionNode);
            }
            //删除所有type=dataGrid的节点
            SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "collapse");
            SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "dataGrid");
            SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "dataGridView");
            if(schemaTransformResult.isDeleteTabsNode()){
                SchemaTransformerUtil.deleteNodeByType(templateDslSchema, "tabs");
            }


            return SchemaTransformerUtil.insertLayoutListAtCollapsePosition(templateDslSchema, insertCollapsePositionNode, layoutList);


        }

        return templateDslSchema;
    }


    public static SchemaTransformResult convertTabsLayoutByDslTemplate(JsonNode layoutSchema, JsonNode templateDslSchema){
        String layoutAndTabsRelation = SchemaTransformerJsonUtil.getLayoutAndTabsRelation(layoutSchema);
        boolean checkNodeTypeExists = SchemaTransformerUtil.checkNodeTypeExists(templateDslSchema, "tabs");
        //layoutSchema中不存在tabs
        SchemaTransformResult schemaTransformResult = null;
        //模版中不存在tabs节点, 或者layoutSchema中不存在tabs节点
        if("none".equals(layoutAndTabsRelation) || !checkNodeTypeExists){
            schemaTransformResult = convertLayoutByDslTemplate(layoutSchema, templateDslSchema);

        }
        else if("all".equals(layoutAndTabsRelation)){ //tabs在最外层, 包含所有组件

            ObjectNode newLayoutSchema = SchemaTransformerJsonUtil.removeTabsNodeInOcrSchema(layoutSchema);
            schemaTransformResult = convertLayoutByDslTemplate(newLayoutSchema, templateDslSchema);

            JsonNode layoutSchemaTabsSetting = null;
            //获取layoutSchema中tabs->settings->tabsSetting[]->name 集合
            List<String> tabsNameList = new ArrayList<>();
            JsonNode groupNode = layoutSchema.get("groups").get(0);
            if(groupNode != null && groupNode.has("settings") && groupNode.get("settings").has("tabsSetting")){
                layoutSchemaTabsSetting = groupNode.get("settings").get("tabsSetting");
                if(layoutSchemaTabsSetting != null && layoutSchemaTabsSetting.isArray()){
                    for(JsonNode tabsSettingNode : layoutSchemaTabsSetting){
                        String tabsName = tabsSettingNode.get("name").asText();
                        tabsNameList.add(tabsName);
                    }
                }
            }
            if(layoutSchemaTabsSetting == null){
                throw new RuntimeException("layoutSchemaTabsSetting is null");
            }


            //修改schemaTransformResult中type=tabs的节点 (当前函数用不到. 修改的是入参:templateDslSchema . 后续会用)
            JsonNode tabsNode = SchemaTransformerUtil.getFirstNodeByType(templateDslSchema, "tabs");
            if(tabsNode != null){
                ObjectNode tabsObjectNode = (ObjectNode)tabsNode;
                ObjectNode settingsNode = null;
                if (tabsObjectNode.get("settings") == null) {
                    settingsNode = tabsObjectNode.putObject("settings");
                }else {
                    settingsNode = (ObjectNode)tabsObjectNode.get("settings");
                }
                settingsNode.set("tabsSetting", layoutSchemaTabsSetting);

            }



        }
        else if("part".equals(layoutAndTabsRelation)){ //tabs包含了部分组件
            ObjectNode newLayoutSchema = SchemaTransformerJsonUtil.removeTabsNodeInOcrSchema(layoutSchema);
            //按照没有tabs的情况替换模版.
            schemaTransformResult = convertLayoutByDslTemplate(newLayoutSchema, templateDslSchema);
            //获取templateDslSchema中所有tabs节点集合
            List<JsonNode> tabsNodeList = SchemaTransformerJsonUtil.getTabsArrayNodeByTemplateAndOcr(layoutSchema, templateDslSchema);
            
              
            //将tabsNodeList中的节点添加到schemaTransformResult中type=tabs的节点中
            SchemaTransformerJsonUtil.addTabs(schemaTransformResult.getLayoutList(), tabsNodeList);
            schemaTransformResult.setDeleteTabsNode(true);

        } else{
            throw new RuntimeException("layoutAndTabsRelation is not valid");
        }
        return schemaTransformResult;
    }




    /**
     * 将布局Schema 套用 模版DSLSchema样式, 输出转换结果
     * @param layoutSchema  布局Schema
     * @param dslSchema     模版DSLSchema
     * @return
     */
    public static SchemaTransformResult convertLayoutByDslTemplate(JsonNode layoutSchema, JsonNode dslSchema){


        //整体布局(页面属性, 分组, 重复表)的前后关系. layoutSchema 下 groups
        //获取模版中  第一个分组信息 (当没有分组时,获取"包含最多实体属性的网格或容器" ) / 第一个重复表信息
        JsonNode firstCollapseNode = SchemaTransformerUtil.getFirstNodeByType(dslSchema, "collapse");

        //获取模版中第一个重复节信息
        JsonNode firstDataGridNode = null;
        firstDataGridNode = SchemaTransformerUtil.getFirstNodeByType(dslSchema, "dataGrid");
        if(firstDataGridNode == null){
            firstDataGridNode = SchemaTransformerUtil.getFirstNodeByType(dslSchema, "dataGridView");
        }
        //获取模版中  分组包含的第一个重复节信息. 如果存在,则选择这个分组作为重复节模版.
        List<NodePosition> collapseList = SchemaTransformerUtil.getAllTypeNodeInfo(dslSchema, "collapse");
        for (NodePosition nodePosition : collapseList) {
            String parentId = nodePosition.getParentId();
//            firstCollapseNod
            JsonNode collapseNode = nodePosition.getObjectNode();
            //collapseNode中包含dataGrid
            JsonNode tempDataGrid = SchemaTransformerUtil.getFirstNodeByType(collapseNode, "dataGrid");
            if(tempDataGrid == null){
                tempDataGrid = SchemaTransformerUtil.getFirstNodeByType(collapseNode, "dataGridView");
            }
            if(tempDataGrid != null){

                NodePosition firstCollapseNodePosition = SchemaTransformerUtil.getNodePositionById(dslSchema, firstCollapseNode.get("id").asText());
                if(firstCollapseNodePosition != null && firstCollapseNodePosition.getParentId().equalsIgnoreCase(nodePosition.getParentId())) {
                    firstDataGridNode = collapseNode;
                }
            }
        }

        NodePosition pagePropsNodePosition = SchemaTransformerUtil.getFirstNodePositionByName(dslSchema, "页面属性");
        
        //确定布局转换后json插入位置.
        // NodePosition insertPoint = new NodePosition();

        //整体布局(页面属性, 分组, 重复表)的前后关系. layoutSchema 下 groups 
        ArrayNode layoutList = new ArrayNode(null);
        JsonNode groups = layoutSchema.get("groups");

        NodePosition insertCollapsePositionNode = null;
        if (groups != null && groups.isArray()) {
            for (JsonNode group : groups) {
                if (group == null || !group.has("type")) {
                    continue;
                }
                String name = group.get("name").asText();
                String type = group.get("type").asText();


                if(groups.size() == 1 && OcrGroupType.PAGE_PROPS.getValue().equals(type)){//如果只有一个group. 且type=pageProps,则认为识别错误. 按照分组处理.
                    type =  "collapse";
                }
                
                if(pagePropsNodePosition == null && "pageProps".equals(type)){// 模版中没有页面属性时.也按照分组处理.
                    type =  "collapse";
                }
                try {
                    if ("collapse".equals(type) ) {
                        if(firstCollapseNode != null){

                            //分组逻辑
                            JsonNode collapseJsonNode = SchemaTransformerCollapse.convertGroupByLayout(group, firstCollapseNode);

                            if (collapseJsonNode != null) {
                                NodePosition mostCollapseNodePosition = SchemaTransformerUtil.getNodeHasMostComponent(dslSchema,"collapse");
                                insertCollapsePositionNode = mostCollapseNodePosition;
                                log.info("分组:"+name+" 加入layoutList类别:分组逻辑" );

                                if(collapseJsonNode.isArray()) {
                                    layoutList.addAll((ArrayNode) collapseJsonNode);
                                } else {
                                    layoutList.add(collapseJsonNode);
                                }
                            }

                        } else {
                            //存在重复表(包括: 网格(多层套网格/容器) ,容器套网格 , 容器流式布局),且实体组件在重复表的兄弟节点
                            boolean isDataGrid = SchemaTransformerUtil.checkNodeTypeExists(dslSchema, "dataGrid");
                            boolean isDataGridView = SchemaTransformerUtil.checkNodeTypeExists(dslSchema, "dataGridView");
                            //获取"dataGrid"节点评级中包含最多实体组件的节点.
                            NodePosition dataGridBrotherNode = SchemaTransformerUtil.getDataGridBrotherNode(dslSchema);
                            if ((isDataGrid || isDataGridView) && dataGridBrotherNode != null) {
                                insertCollapsePositionNode = dataGridBrotherNode;
                                ObjectNode brotherNode = dataGridBrotherNode.getObjectNode();
                                if(dataGridBrotherNode != null && brotherNode != null){
                                    JsonNode collapseJsonNode = SchemaTransformerCollapse.convertGroupByLayout(group, brotherNode);
                                    if (collapseJsonNode != null) {
                                        log.info("分组:"+name+" 加入layoutList类别:无分组,有重复表" );
                                        if(collapseJsonNode.isArray()) {
                                            layoutList.addAll((ArrayNode) collapseJsonNode);
                                        } else {
                                            layoutList.add(collapseJsonNode);
                                        }
                                        continue;
                                    }
                                }

                            }

//                            //无分组, 无重复节, 从grid或container中找到包含实体组件最多的容器.
//                            boolean isGrid = SchemaTransformerUtil.checkNodeTypeExists(dslSchema, "grid");
//                            if (isGrid) {
                                //获取"dataGrid"节点评级中包含最多实体组件的节点.

                                //获取包含实体组件最多的 grid或container
                                NodePosition mostComponentNodePosition = SchemaTransformerUtil.getMostFrequentNodePosition(dslSchema);

                                insertCollapsePositionNode = mostComponentNodePosition;
                                if(mostComponentNodePosition != null && mostComponentNodePosition.getObjectNode() != null) {
                                    ObjectNode mostComponentObjectNode = mostComponentNodePosition.getObjectNode();
                                    JsonNode collapseJsonNode = SchemaTransformerCollapse.convertGroupByLayout(group, mostComponentObjectNode);
                                    if (collapseJsonNode != null) {
                                        //没有分组, 无分组, 无重复节 时比较特殊.指定insertPositionId

                                        log.info("分组:"+name+" 加入layoutList类别:无分组,无重复表" );
                                        if(collapseJsonNode.isArray()) {
                                            layoutList.addAll((ArrayNode) collapseJsonNode);
                                        } else {
                                            layoutList.add(collapseJsonNode);
                                        }
                                        continue;
                                    }
                                }
//                            }
//
//                            // 无分组, 无重复节, 无网格
//                            //获取没有网格的node
//                            NodePosition mostComponentNodePosition = SchemaTransformerUtil.getNodeHasMostComponent(dslSchema,"container");
//
//                            if(mostComponentNodePosition != null && mostComponentNodePosition.getObjectNode() != null) {
//                                JsonNode collapseJsonNode = SchemaTransformerCollapse.convertGroupByLayout(group, mostComponentNodePosition.getObjectNode());
//                                if (collapseJsonNode != null) {
//                                    log.info("分组:"+name+" 加入layoutList类别:无分组,无重复表,无网格" );
//                                    layoutList.add(collapseJsonNode);
//                                    continue;
//                                }
//                            }


                        }
                    } else if ("dataGrid".equals(type) ) {
                        JsonNode dataGridJsonNode = SchemaTransformerDataGrid.convertGroupDataGridStructure(group, firstDataGridNode);
                        if (dataGridJsonNode != null) {
                            log.info("重复表:"+name+" 加入layoutList类别" );
                            layoutList.add(dataGridJsonNode);
                        }
                    } else if ("pageProps".equals(type) ) {
                        // 通过title="页面属性"获取节点对象templateNode . 现在因为没有规律.暂时忽略.
//                        JsonNode templateNode = null;
//                        JsonNode pagePropsJsonNode = SchemaTransformerPageProps.convertGroupStructure(group, templateNode);
//                        if (pagePropsJsonNode != null) {
                            log.info("页面属性:"+name+" 加入layoutList类别" );
//                        }
//                        SchemaTransformerPageProps.updatePagePropsNode(dslSchema, pagePropsJsonNode);
                    } else {
                        log.error("不支持的组类型: " + type);
                        // throw new RuntimeException("不支持的组类型: " + type);
                    }
                } catch (Exception e) {
                    log.info("处理组 " + group.get("name") + " 时发生错误: " + e.getMessage());
                    e.printStackTrace();
                    // 继续处理下一个组，而不是中断整个过程
                    continue;
                }
            }
        }
        if(insertCollapsePositionNode == null){
            throw new BusinessException("没有找到需要替换的容器和网格 !, 请检查ocr和模版中是否有容器和网格");
        }
        SchemaTransformResult schemaTransformResult = new SchemaTransformResult();
        schemaTransformResult.setNodePosition(insertCollapsePositionNode);
        schemaTransformResult.setLayoutList(layoutList);
        return schemaTransformResult;
    }

    
}
