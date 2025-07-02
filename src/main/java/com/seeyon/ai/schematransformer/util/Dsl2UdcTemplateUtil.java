package com.seeyon.ai.schematransformer.util;

import com.alibaba.excel.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.constant.PresetFieldConstants;
import com.seeyon.ai.schematransformer.enums.StereotypeEnum;
import com.seeyon.ai.schematransformer.model.DslTransformConstant;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

public class Dsl2UdcTemplateUtil {

    private static final List<String> UDC_CONTAINER_COMP = Arrays.asList("form", "container", "dataGrid", "grid", "gridCell");
    private static final List<String> UDC_PREFIX_COMP = new ArrayList<String>() {{
        addAll(UDC_CONTAINER_COMP);
        add("reference");
    }};
    private static final List<String> UDC_INLINE_COMP_TYPE = Arrays.asList("UdcForm", "UdcContainer", "UdcDataGrid", "UdcGrid", "UdcGridCell");

    
    /**
     * 数据清洗, 遍历dsl中所有节点 和 子节点, 如果节点为null, 则删除该节点. 主要用于避免 jsonNode.isNull()判断.直接采用!=null判断.
     * @param dsl
     */
    public static void removeNullNode(ObjectNode dsl) {
        if (dsl == null) {
            return;
        }

        // 获取所有字段名
        Iterator<String> fieldNames = dsl.fieldNames();
        List<String> fieldsToRemove = new ArrayList<>();

        // 遍历所有字段
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode node = dsl.get(fieldName);

            // 如果节点为null，添加到待删除列表
            if (node == null) {
                fieldsToRemove.add(fieldName);
                continue;
            }

            // 递归处理对象节点
            if (node.isObject()) {
                removeNullNode((ObjectNode) node);
            }
            // 递归处理数组节点
            else if (node.isArray()) {
                ArrayNode arrayNode = (ArrayNode) node;
                for (int i = 0; i < arrayNode.size(); i++) {
                    JsonNode element = arrayNode.get(i);
                    if (element.isObject()) {
                        removeNullNode((ObjectNode) element);
                    }
                }
            }
        }

        // 删除所有null值节点
        for (String fieldName : fieldsToRemove) {
            dsl.remove(fieldName);
        }
    }

    /**
     * 将dsl 的所有节点id 和 type 替换为 udc格式的id 和type 
     * 
     * @param dsl
     */
    public static void changeAllIdAndType(ObjectNode dsl) {

        DslTransformUtil.loopChildren(dsl, null, 0, (data, parentData, index) -> {
            String type = data.get("type") == null ? "" : data.get("type").asText();
            if (UDC_PREFIX_COMP.contains(type)) {
                data.put("id", "udc" + DslTransformUtil.capitalizeFirstLetter(type) + "_" + RandomStringUtils.random(4, true, true));
                data.put("type", "Udc" + DslTransformUtil.capitalizeFirstLetter(type));
            } else {
                data.put("id", type + "_" + RandomStringUtils.random(4, true, true));
                data.put("type", DslTransformUtil.capitalizeFirstLetter(type));
            }
        });
    }

    public static Map<String,JsonNode> getRootChildrenAndChildById(ObjectNode dsl,ArrayNode entityLists,JsonNode tempSchemaChildById, JsonNode appInfo, boolean  isDoc){
        ObjectMapper objectMapper = new ObjectMapper();

        ArrayNode children = objectMapper.createArrayNode();
        ObjectNode childById = objectMapper.createObjectNode();
        ObjectNode newDsl = objectMapper.createObjectNode();
        newDsl.put("children", children);
        newDsl.put("childById", childById);

        DslTransformUtil.loopChildren(dsl, null, 0, (data, parentData, index) -> {

            ObjectNode singleComp2 = Dsl2UdcTemplateUtil.setComponentInfo(data, parentData, index);
            if (singleComp2 != null && !"page".equalsIgnoreCase(singleComp2.get("type").asText())) {

                String parentId = singleComp2.get("parentId").asText();
                if ( "page".equalsIgnoreCase(parentId)) {
                    String compId = singleComp2.get("id") == null ? "" :singleComp2.get("id").asText();
                    children.add(compId);
                }
                String singleComp2Id = singleComp2.get("id").asText();
                childById.set(singleComp2Id, singleComp2);

            }
        });


        // 7.遍历所有节点, 设置数据源
        Iterator<String> compIds2 = childById.fieldNames();
        while (compIds2.hasNext()) {
            String compId = compIds2.next();
            //设置数据源
            setDataSourceWithJsonNode(compId, entityLists,children,childById , tempSchemaChildById, appInfo, isDoc);
        }


        HashMap<String,JsonNode> resultMap = new HashMap<>();
        resultMap.put("children",children);
        resultMap.put("childById",childById);
        return resultMap;
    }




    /**
     * 设置默认模板设置
     */
    @SuppressWarnings("unchecked")
    public static void setDefaultTempSettings(String compId, JsonNode childById, JsonNode tempSchemaChildById) {
        if (compId == null ||  tempSchemaChildById == null) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode childById = newDsl.get("childById");
        if (childById == null || !childById.isObject()) {
            return;
        }

        JsonNode comp = childById.get(compId);
        if (comp == null || !comp.isObject()) {
            return;
        }

        JsonNode parentId = comp.get("parentId");
        JsonNode fComp = parentId != null ? childById.get(parentId.asText()) : null;
        JsonNode sourceSchema = comp.path("sourceSchema").path("id");

        if (!sourceSchema.isMissingNode()) {
            String sourceSchemaId = sourceSchema.asText();
            JsonNode findTempComp = tempSchemaChildById.get(sourceSchemaId);

            // 单元格特殊处理 属性在extraConfig中
            if ("UdcGridCell".equals(comp.path("type").asText())) {
                Iterator<String> tempCompIds = tempSchemaChildById.fieldNames();
                while (tempCompIds.hasNext()) {
                    String tempCompId = tempCompIds.next();
                    JsonNode tempComp = tempSchemaChildById.get(tempCompId);
                    if ("UdcGrid".equals(tempComp.path("type").asText())) {
                        JsonNode extraGridCell = tempComp.path("extraConfig").path(sourceSchemaId);
                        if (!extraGridCell.isMissingNode()) {
                            findTempComp = extraGridCell;
                            ObjectNode compSettings = (ObjectNode) comp.get("settings");
                            if (compSettings == null) {
                                compSettings = objectMapper.createObjectNode();
                            }
                            
                            // 合并styles
                            ObjectNode styles = compSettings.has("styles") ? 
                                (ObjectNode) compSettings.get("styles") : objectMapper.createObjectNode();
                            ObjectNode tempStyles = (ObjectNode)extraGridCell.path("settings").path("styles");
                            ObjectNode mergeStyles = tempStyles.deepCopy();
                            if (tempStyles.isObject()) {
                                mergeStyles.setAll( tempStyles);
                                mergeStyles.setAll( styles);
                            }
                            compSettings.set("styles", mergeStyles);
                            ((ObjectNode) comp).set("settings", compSettings);
                            break;
                        }
                    }
                }
            }

            if (findTempComp != null && findTempComp.isObject()) {
                // 过滤模版无用属性
                if (findTempComp.has("settings")) {
                    ObjectNode tempSettings = (ObjectNode) findTempComp.get("settings");
                    if (tempSettings.has("columns")) {
                        tempSettings.remove("columns");
                    }
                }

                // 处理验证规则合并
                ObjectNode validationObj = objectMapper.createObjectNode();
                JsonNode tempValidation = findTempComp.path("settings").path("validation");
                JsonNode compValidation = comp.path("settings").path("validation");
                
                if (!tempValidation.isMissingNode()) {
                    if (!compValidation.isMissingNode()) {
                        ((ObjectNode) validationObj).setAll((ObjectNode) tempValidation);
                        ((ObjectNode) validationObj).setAll((ObjectNode) compValidation);
                    }
                }

                // 构建新的组件对象
                ObjectNode newComp = objectMapper.createObjectNode();
                newComp.setAll((ObjectNode) comp);

                // 合并settings
                ObjectNode settings = objectMapper.createObjectNode();
                if (findTempComp.has("settings")) {
                    settings.setAll((ObjectNode) findTempComp.get("settings"));
                }
                if (comp.has("settings")) {
                    settings.setAll((ObjectNode) comp.get("settings"));
                }
                if (!validationObj.isEmpty()) {
                    settings.set("validation", validationObj);
                }
                newComp.set("settings", settings);

                // 设置name
                String name = comp.path("name").asText("null");
                if ("null".equals(name)) {
                    name = findTempComp.path("name").asText("null");
                    if ("null".equals(name)) {
                        name = comp.path("componentName").asText("");
                    }
                }
                newComp.put("name", name);
                newComp.put("componentName", findTempComp.path("componentName").asText(""));

                // 构建uiModel
                ObjectNode uiModel = objectMapper.createObjectNode();
                if (findTempComp.has("uiModel")) {
                    uiModel.setAll((ObjectNode) findTempComp.get("uiModel"));
                }
                if (comp.has("uiModel")) {
                    uiModel.setAll((ObjectNode) comp.get("uiModel"));
                }

                // 设置title
                ObjectNode title = objectMapper.createObjectNode();
                title.put("type", "Simple");
                String titleText = comp.path("uiModel").path("title").path("simple").asText("");
                if (titleText.isEmpty()) {
                    titleText = findTempComp.path("uiModel").path("title").path("simple").asText("");
                    if (titleText.isEmpty()) {
                        titleText = findTempComp.path("componentName").asText("");
                    }
                }
                title.put("simple", titleText);
                uiModel.set("title", title);

                // 设置status
                ObjectNode status = objectMapper.createObjectNode();
                status.put("type", "Simple");
                String statusText = comp.path("uiModel").path("status").path("simple").asText("");
                if (statusText.isEmpty()) {
                    statusText = findTempComp.path("uiModel").path("status").path("simple").asText("basic");
                }
                status.put("simple", statusText);
                uiModel.set("status", status);

                newComp.set("uiModel", uiModel);

                // 设置uiModelSchema
                if (findTempComp.has("uiModelSchema")) {
                    newComp.set("uiModelSchema", findTempComp.get("uiModelSchema"));
                }

                // 更新组件
                ((ObjectNode) childById).set(compId, newComp);
            }
        }

        // 单元格特殊处理
        if ("UdcGridCell".equals(comp.path("type").asText())) {
            ((ObjectNode) comp).remove("sourceSchema");
            if (fComp != null && fComp.isObject()) {
                ObjectNode extraConfig = fComp.has("extraConfig") ? 
                    (ObjectNode) fComp.get("extraConfig") : objectMapper.createObjectNode();
                
                ObjectNode cellConfig = objectMapper.createObjectNode();
                cellConfig.setAll((ObjectNode) comp);
                cellConfig.set("children", objectMapper.createArrayNode());
                extraConfig.set(compId, cellConfig);
                ((ObjectNode) fComp).set("extraConfig", extraConfig);

                ObjectNode extraChildren = fComp.has("extraChildren") ? 
                    (ObjectNode) fComp.get("extraChildren") : objectMapper.createObjectNode();
                ArrayNode children = comp.has("children") ? 
                    (ArrayNode) comp.get("children") : objectMapper.createArrayNode();
                extraChildren.set(compId, children);
                ((ObjectNode) fComp).set("extraChildren", extraChildren);
            }
        }

        // 去除模版数据源
        if (childById.has(compId)) {
            JsonNode updatedComp = childById.get(compId);
            if (updatedComp.has("sourceSchema")) {
                ((ObjectNode) updatedComp).remove("sourceSchema");
            }
        }
    }


    /**
     * 设置组件基本信息
     * @param index 索引
     * @return 处理后的组件数据
     */
    public static ObjectNode setComponentInfo(ObjectNode data, ObjectNode parentData, int index) {

        String type = data.get("type").asText();
        ObjectMapper objectMapper = new  ObjectMapper();
        // 删除跨行跨列属性
        data.remove("cellColRow");

        // 处理PAGE类型
        if ("page".equalsIgnoreCase(type)) {
            data.put("type", "page");
            return data;
        }


        // 设置标题属性
        ObjectNode settings = (ObjectNode) data.get("settings");
        if (settings != null) {
            String titleName = settings.get("titleName") != null ? settings.get("titleName").asText() : null;
            String title = settings.get("title") != null ? settings.get("title").asText() : null;
            if(StringUtils.isNotBlank(titleName)){
                title = titleName;
            }
            if (title != null) {
                ObjectNode uiModel = objectMapper.createObjectNode();
                ObjectNode titleObj = objectMapper.createObjectNode();
                titleObj.put("type", "Simple");
                titleObj.put("simple", title);
                uiModel.put("title", titleObj);
                data.put("uiModel", uiModel);
                data.put("name", title);
                settings.remove("title");
                settings.remove("titleName");
            }
        }

        // 处理只读态
        if (settings != null && settings.get("readOnly") != null && settings.get("readOnly").asBoolean()) {
            ObjectNode uiModel = (ObjectNode) data.get("uiModel");
            if (uiModel == null) {
                uiModel = objectMapper.createObjectNode();
                data.put("uiModel", uiModel);
            }
            ObjectNode status = objectMapper.createObjectNode();
            status.put("type", "Simple");
            status.put("simple", "readonly");
            uiModel.put("status", status);
            settings.remove("readOnly");
        }

        // 预置字段数据源填充
        String name = (data == null || !data.has("name")) ? null : data.get("name").asText();
        if (name != null && PresetFieldConstants.PRESET_NAME_SET.contains(name) && !data.has("dataSource")) {
            for (String dataField : PresetFieldConstants.PRESET_FIELD_MAP.keySet()) {
                if (PresetFieldConstants.PRESET_FIELD_MAP.get(dataField).equals(name)) {
                    ObjectNode dataSource = objectMapper.createObjectNode();
                    dataSource.put("dataField", dataField);
                    data.put("dataSource", dataSource);
                    break;
                }
            }
        }

        // 设置必填项
        if (settings != null && settings.has("validation")) {
            ObjectNode validation = (ObjectNode) settings.get("validation");
            if (validation.has("required") ) {
                ObjectNode newValidation = objectMapper.createObjectNode();
                ObjectNode required = objectMapper.createObjectNode();
                ObjectNode value = objectMapper.createObjectNode();
                value.put("type", "Simple");
                value.put("simple", validation.get("required").asBoolean());
                required.put("value", value);
                newValidation.put("required", required);
                settings.put("validation", newValidation);
            }
        }

        // 处理网格和重复表
        if ("UdcGrid".equals(data.get("type").asText()) || "UdcDataGrid".equals(data.get("type").asText())) {
            handleGridComponent(data);
        }

        // 容器组件兼容
        if ("UdcContainer".equals(data.get("type").asText())) {
            if (settings != null ) {
                if(settings.has("columns")) {
                    settings.put("flexWrap", "wrap");
                }
                settings.put("flexDirection","row" );

            }

        }

        // 处理占比值
        if (settings != null && settings.has("flexRowSize")) {
            ObjectNode flexRowSize = objectMapper.createObjectNode();
            flexRowSize.put("type", "basis");
            flexRowSize.put("basis", settings.get("flexRowSize"));
            settings.put("flexRowSize", flexRowSize);
        }

        // 处理特殊组件
        handleSpecialComponents(data);

        // 补齐formItem  和 父子关系构造
        setParentChildRelation(data, parentData, index);

        return data;
    }

    /**
     * 处理网格和重复表组件
     */
    public static void handleGridComponent(ObjectNode data) {
        if (data == null) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        // 获取settings
        JsonNode settingsNode = data.get("settings");
        if (settingsNode == null || !settingsNode.isObject()) {
            settingsNode = objectMapper.createObjectNode();
            data.set("settings", settingsNode);
        }
        ObjectNode settings = (ObjectNode) settingsNode;

        // 处理gridTemplateColumns
        JsonNode gridTemplateColumnsNode = settings.get("gridTemplateColumns");
        if (gridTemplateColumnsNode != null && gridTemplateColumnsNode.isArray()) {
            ArrayNode gridTemplateColumns = (ArrayNode) gridTemplateColumnsNode;
            int totalColumns = gridTemplateColumns.size();
            if (totalColumns > 0) {
                settings.put("columns",totalColumns);
//                double[] columnWidths = new double[totalColumns];
//                double totalWidth = 0;
//
//                // 计算总宽度和各列宽度
//                for (int i = 0; i < totalColumns; i++) {
//                    JsonNode columnNode = gridTemplateColumns.get(i);
//                    if (columnNode != null && columnNode.isNumber()) {
//                        columnWidths[i] = columnNode.asDouble();
//                        totalWidth += columnWidths[i];
//                    }
//                }

//                // 重新计算各列宽度比例
//                if (totalWidth > 0) {
//                    ArrayNode newGridTemplateColumns = objectMapper.createArrayNode();
//                    for (double width : columnWidths) {
//                        newGridTemplateColumns.add(width / totalWidth);
//                    }
//                    settings.set("gridTemplateColumns", newGridTemplateColumns);
//                }
            }
        }

        // 处理gridTemplateRows
        JsonNode gridTemplateRowsNode = settings.get("gridTemplateRows");
        if (gridTemplateRowsNode != null && gridTemplateRowsNode.isArray()) {
            ArrayNode gridTemplateRows = (ArrayNode) gridTemplateRowsNode;
            int totalRows = gridTemplateRows.size();
            if (totalRows > 0) {
                settings.put("rows",totalRows);
                ArrayNode newGridTemplateRows = objectMapper.createArrayNode();
                for (int i = 0; i < totalRows; i++) {
                    newGridTemplateRows.add(28); // 固定行高为28
                }
                settings.set("gridTemplateRows", newGridTemplateRows);
            }
        }
    }

    /**
     * 处理特殊组件
     */
    public static void handleSpecialComponents(ObjectNode data) {
        String type = data.get("type").asText();
        ObjectNode settings = (ObjectNode) data.get("settings");

        ObjectMapper objectMapper = new ObjectMapper();
        // 意见输入框默认设置
        if ("UiBusinessEdocOpinionBox".equals(type)) {
            if (settings == null) {
                settings = objectMapper.createObjectNode();
                data.set("settings", settings);
            }
            settings.put("contentHeight", 3);
            settings.put("inscribeSetting", "unifiedSettings");
            settings.remove("align");
            data.put("defaultDataType", objectMapper.getNodeFactory().textNode("OPINION"));
        }

        // 选人控件默认值
        if ("UiBusinessSelectPeople".equals(type)) {
            if (settings == null) {
                settings = objectMapper.createObjectNode();
                data.set("settings", settings);
            }
            settings.put("orgAccess", true);
            settings.put("manualEntry", true);

            ObjectNode validation = objectMapper.createObjectNode();
            ObjectNode maxLength = objectMapper.createObjectNode();
            maxLength.put("validate", true);
            maxLength.put("value", "10000");
            maxLength.putNull("message");
            validation.set("maxLength", maxLength);

            ObjectNode minLength = objectMapper.createObjectNode();
            minLength.put("validate", true);
            minLength.put("value", "0");
            minLength.putNull("message");
            validation.set("minLength", minLength);

            settings.set("validation", validation);
            settings.putNull("initialValue");
            settings.put("rows", 2);
            settings.put("autoGrow", "fixed");
            settings.put("lineLimited", "unlimited");
            settings.put("labelLength", 120);
            settings.put("labelPosition", "top");
            ArrayNode tabs = objectMapper.createArrayNode();
            for (String tab : Arrays.asList("department", "institution", "ocip_unit", "unit_team", "outside_unit")) {
                tabs.add(tab);
            }
            settings.set("tabs", tabs);
            ArrayNode selectTypes = objectMapper.createArrayNode();
            for (String selectTypeValue : Arrays.asList("DEPARTMENT", "INSTITUTION", "OCIP_ACCOUNT", "OCIP_DEPARTMENT",
                    "OUTSIDE_INSTITUTION", "OUTSIDE_DEPARTMENT", "UNIT_TEAM")) {
                selectTypes.add(selectTypeValue);
            }
            settings.set("selectType", selectTypes);
        }

        // 对齐方式
        if (settings != null && settings.has("align")) {
            settings.set("labelAlignment", objectMapper.getNodeFactory().textNode(settings.get("align").asText()));
            settings.remove("align");
        }

        // 日期设置
        if ("DatePicker".equals(type)) {
            if (settings == null) {
                settings = objectMapper.createObjectNode();
                data.set("settings", settings);
            }
            settings.put("displayFormate", "default");
            settings.set("presets", objectMapper.createArrayNode());
        }

        // 文本标签
        if (SchemaTransformerUtil.isLabelOrFieldTitle(type)) {
            if (settings == null) {
                settings = objectMapper.createObjectNode();
                data.set("settings", settings);
            }
            settings.set("contentSource", objectMapper.getNodeFactory().textNode("custom"));
            settings.set("iconPosition", objectMapper.getNodeFactory().textNode("front"));

            ObjectNode iconName = objectMapper.createObjectNode();
            iconName.set("type", objectMapper.getNodeFactory().textNode("Simple"));
            iconName.set("simple", objectMapper.getNodeFactory().textNode(""));
            settings.set("iconName", iconName);

            ObjectNode iconColor = objectMapper.createObjectNode();
            iconColor.set("type", objectMapper.getNodeFactory().textNode("Simple"));
            iconColor.set("simple", objectMapper.getNodeFactory().textNode(""));
            settings.set("iconColor", iconColor);

            ObjectNode iconStatus = objectMapper.createObjectNode();
            iconStatus.set("type", objectMapper.getNodeFactory().textNode("Simple"));
            iconStatus.set("simple", objectMapper.getNodeFactory().textNode("invisible"));
            settings.set("iconStatus", iconStatus);

            if (settings.has("labelAlignment")) {
                settings.set("alignment", settings.get("labelAlignment"));
                settings.remove("labelAlignment");
            }
            data.remove("dataSource");
        }
    }

    /**
     * 父子关系构造
     */
    public static void setParentChildRelation(ObjectNode data, ObjectNode parentData, int index) {
        if (data == null) {
            return;
        }

        // 检查是否需要设置 isFormItem (补齐formItem)
        String dataType = data.get("type").asText();
        if (!UDC_INLINE_COMP_TYPE.contains(dataType) &&
            data.has("dataSource") &&
            data.get("dataSource").size() > 0 &&
            (parentData == null || !"UdcDataGrid".equals(parentData.get("type").asText()))) {
            data.put("isFormItem", true);
        }

        //设置父子关系 (迁移到外面, 单独设置父子关系)
        String parentId;
        if (parentData == null ) {
            parentId = "page";
        } else {

            String type = parentData.has("type") ? parentData.get("type").asText() : null;
            if("page".equalsIgnoreCase(type)){
                parentId = "page";
            }else {
                parentId = parentData.has("id") ? parentData.get("id").asText() : parentData.get("type").asText();
            }
            // 处理 gridCell 的特殊情况
            if (parentId.contains("udcGridCell")) {
                parentId = parentData.get("parentId").asText();
            }
        }
        data.put("parentId", parentId);

        // 处理父组件的 children 数组
        if (parentData != null && !"UdcDataGrid".equals(parentData.get("type").asText())) {
            ArrayNode children;
            if (!parentData.has("children")) {
                children = new ObjectMapper().createArrayNode();
                parentData.set("children", children);
            } else {
                JsonNode existingChildren = parentData.get("children");
                if (existingChildren.isArray()) {
                    // 检查是否所有元素都不是字符串类型
                    boolean allNonString = true;
                    for (JsonNode child : existingChildren) {
                        if (child.isTextual()) {
                            allNonString = false;
                            break;
                        }
                    }
                    if (allNonString) {
                        children = new ObjectMapper().createArrayNode();
                        parentData.set("children", children);
                    } else {
                        children = (ArrayNode) existingChildren;
                    }
                } else {
                    children = new ObjectMapper().createArrayNode();
                    parentData.set("children", children);
                }
            }
            children.add(data.get("id").asText());
        }
    }


    /**
     * 设置数据源 (使用 JsonNode 结构)
     */
    @SuppressWarnings("unchecked")
    public static void setDataSourceWithJsonNode(String compId, ArrayNode entityLists, ArrayNode children,ObjectNode childById,
                                                 JsonNode tempSchemaChildById, JsonNode appInfo, boolean isDoc) {
        if (compId == null || children == null) {
            return;
        }

//        ObjectNode childById = (ObjectNode) children.get("childById");
        if (childById == null) {
            return;
        }

        JsonNode data = childById.get(compId);
        if (data == null || !(data instanceof ObjectNode)) {
            return;
        }

        // 设置默认模板配置
        setDefaultTempSettings(compId ,childById, tempSchemaChildById);
        //获取模版的对象
        data = childById.get(compId);
        // 检查是否有数据源
        JsonNode dataSourceNode = data.get("dataSource");
        if (dataSourceNode == null || !dataSourceNode.isObject() || dataSourceNode.size() == 0) {
            return;
        }

        String type = data.get("type").asText();
        ObjectMapper objectMapper = new ObjectMapper();

        // 处理Form和DataGrid
        if (type.contains("UdcForm") || type.contains("UdcDataGrid")) {
            String entityName = "";
            JsonNode findEntityData = null;

            if (isDoc) {
                if (entityLists.size() > 0) {
                    findEntityData = entityLists.get(0);
                    entityName = findEntityData.get("name").asText();
                }
            } else {
                entityName = dataSourceNode.get("entityName") != null ? dataSourceNode.get("entityName").asText() : "";
                for (JsonNode entity : entityLists) {
                    if (entity.get("name") != null && entityName.equals(entity.get("name").asText())) {
                        findEntityData = entity;
                        break;
                    }
                }
            }

            if (findEntityData != null) {
                String id = (findEntityData.get("id") != null && !findEntityData.get("id").isNull()) ? findEntityData.get("id").asText() : "";
                String name = (findEntityData.get("name") != null && !findEntityData.get("name").isNull()) ? findEntityData.get("name").asText() : "";
                String fullName = (findEntityData.get("fullName") != null && !findEntityData.get("fullName").isNull()) ? findEntityData.get("fullName").asText() : "";
                String caption = (findEntityData.get("caption") != null && !findEntityData.get("caption").isNull()) ? findEntityData.get("caption").asText() : "";

                if (entityName != null && !entityName.isEmpty()) {
                    if (type.contains("UdcForm")) {
                        ObjectNode newDataSource = objectMapper.createObjectNode();
                        newDataSource.setAll((ObjectNode)dataSourceNode);
                        newDataSource.put("type", "ENTITY");
                        newDataSource.put("subType", "NORMAL");
                        newDataSource.put("key", isDoc ? data.get("id").asText() + "." + fullName + "." + name : data.get("id").asText() + "-" + id);
                        newDataSource.put("isRoot", true);
                        newDataSource.put("isRef", isDoc);
                        newDataSource.put("dataShape", "SINGLE");

                        ObjectNode entity = objectMapper.createObjectNode();
                        entity.put("id", id.isEmpty() ? fullName : id);
                        entity.put("name", entityName.isEmpty() ? name : entityName);
                        entity.put("fullName", fullName);
                        entity.put("caption", caption);
                        entity.put("stereotype", StereotypeEnum.Bill.getValue());
                        entity.put("category", "ENTITY");
                        entity.put("supportOrderNo", true);
                        entity.put("appName", isDoc ? DslTransformConstant.DOC_APP_NAME : (appInfo.get("name") != null ? appInfo.get("name").asText() : ""));
                        entity.put("appId", isDoc ? DslTransformConstant.DOC_APP_NAME : (appInfo.get("id") != null ? appInfo.get("id").asText() : ""));

                        newDataSource.set("entity", entity);
                        newDataSource.set("fields", objectMapper.createArrayNode());
                        newDataSource.set("fieldBindMap", objectMapper.createObjectNode());
                        newDataSource.put("dataTarget", data.get("id").asText());
                        newDataSource.set("hiddenFields", objectMapper.createArrayNode());

                        if (isDoc) {
                            newDataSource.put("inParamCode", "entity");
                            newDataSource.put("dataFrom", "子页面入参");
                        }

                        ((ObjectNode)data).put("dataSource", newDataSource);
                    } else if (type.contains("UdcDataGrid")) {
                        // 查找Form组件
                        JsonNode formData = data;
                        Iterator<String> fieldNames = childById.fieldNames();
                        while (fieldNames.hasNext()) {
                            String compKey = fieldNames.next();
                            if (compKey.contains("udcForm")) {
                                formData = childById.get(compKey);
                                break;
                            }
                        }

                        if (formData != null && formData.has("dataSource")) {
                            JsonNode formDataSource = formData.get("dataSource");
                            ObjectNode newDataSource = objectMapper.createObjectNode();
                            newDataSource.setAll((ObjectNode)dataSourceNode);
                            
                            newDataSource.put("type", "ENTITY");
                            newDataSource.put("subType", "RELATION");
                            newDataSource.put("key", formDataSource.get("key").asText() + "." + entityName + "DtoList");
                            newDataSource.put("isRoot", false);
                            newDataSource.put("isRef", true);
                            newDataSource.put("dataShape", "LIST");

                            ObjectNode entity = objectMapper.createObjectNode();
                            entity.put("id", id.isEmpty() ? fullName : id);
                            entity.put("name", entityName.isEmpty() ? name : entityName);
                            entity.put("fullName", fullName);
                            entity.put("caption", caption);
                            entity.put("stereotype", StereotypeEnum.Bill.getValue());
                            entity.put("category", "ENTITY");

                            // 设置父实体
                            ObjectNode parent = objectMapper.createObjectNode();
                            parent.put("id", formDataSource.get("entity").get("id").asText());
                            parent.put("name", formDataSource.get("entity").get("name").asText());
                            parent.put("fullName", formDataSource.get("entity").get("fullName").asText());
                            parent.put("caption", formDataSource.get("entity").get("caption").asText());
                            parent.put("stereotype", StereotypeEnum.Bill.getValue());
                            parent.put("category", "ENTITY");
                            parent.put("supportOrderNo", true);
                            parent.put("appName", formDataSource.get("entity").get("appName").asText());
                            parent.put("appId", formDataSource.get("entity").get("appName").asText());
                            entity.set("parent", parent);

                            entity.put("supportOrderNo", true);
                            entity.put("appName", isDoc ? DslTransformConstant.DOC_APP_NAME : (appInfo.get("name") != null ? appInfo.get("name").asText() : ""));
                            entity.put("appId", isDoc ? DslTransformConstant.DOC_APP_NAME : (appInfo.get("id") != null ? appInfo.get("id").asText() : ""));

                            newDataSource.set("entity", entity);
                            newDataSource.set("fields", objectMapper.createArrayNode());
                            newDataSource.put("dataTarget", formData.get("id").asText());
                            newDataSource.put("dataPath", "");
                            ArrayNode fieldPath = objectMapper.createArrayNode();
                            fieldPath.add(id);
                            newDataSource.set("fieldPath", fieldPath);
                            newDataSource.put("dataField", entityName + "DtoList");
                            newDataSource.put("relatedCmp", formData.get("id").asText());

                            ((ObjectNode)data).put("dataSource", newDataSource);
                        }
                    }
                }
            }
        }
        else {
            // 处理其他控件字段数据源
            if (data.has("parentId")) {
                String parentId = data.get("parentId").asText();
                JsonNode parentData = childById.get(parentId);
                
                // 查找Form组件
                JsonNode formData = data;
                Iterator<String> fieldNames = childById.fieldNames();
                while (fieldNames.hasNext()) {
                    String compKey = fieldNames.next();
                    if (compKey.contains("udcForm")) {
                        formData = childById.get(compKey);
                        break;
                    }
                }

                boolean isUdcDataGrid = parentData != null && "UdcDataGrid".equals(parentData.get("type").asText());
                String fieldName = dataSourceNode.get("dataField") != null ? dataSourceNode.get("dataField").asText() : "";
                boolean isExtField = false;

                // 处理属性列表
                ArrayNode attributes = objectMapper.createArrayNode();
                String entityName = "";

                if (isDoc) {
                    if (fieldName.contains(".")) {
                        isExtField = true;
                        entityName = fieldName.split("\\.")[0];
                        String replaceEntityName = DslTransformUtil.capitalizeFirstLetter(entityName.replace("Dto", ""));
                        
                        // 查找对应实体的属性
                        for (JsonNode entity : entityLists) {
                            if (entity.get("name") != null && replaceEntityName.equals(entity.get("name").asText())) {
                                attributes = (ArrayNode) entity.get("attributes");
                                break;
                            }
                        }
                    } else if (entityLists.size() > 0) {
                        attributes = (ArrayNode) entityLists.get(0).get("attributes");
                    }
                } else {
                    JsonNode sourceEntity = (isUdcDataGrid ? parentData : formData).get("dataSource").get("entity");
                    if (sourceEntity != null) {
                        entityName = sourceEntity.get("name").asText();
                        // 查找对应实体的属性
                        for (JsonNode entity : entityLists) {
                            if (entity.get("name") != null && entityName.equals(entity.get("name").asText())) {
                                attributes = (ArrayNode) entity.get("attributes");
                                break;
                            }
                        }
                    }
                }

                if (attributes != null && attributes.size() > 0) {
                    String lastFieldName = fieldName.contains(".") ? fieldName.substring(fieldName.lastIndexOf(".") + 1) : fieldName;
                    JsonNode fieldData = null;
                    for (JsonNode attr : attributes) {
                        if (attr.get("name") != null && lastFieldName.equals(attr.get("name").asText())) {
                            fieldData = attr;
                            break;
                        }
                    }

                    if (fieldData != null) {
                        String id = (fieldData.get("id") != null && !fieldData.get("id").isNull() )  ? fieldData.get("id").asText() : "";
                        String fullName = (fieldData.get("attributeFullName") != null && !fieldData.get("attributeFullName").isNull() )  ?
                            fieldData.get("attributeFullName").asText() : 
                            (fieldData.get("fullName") != null ? fieldData.get("fullName").asText() : "");
                        String name =(fieldData.get("name") != null && !fieldData.get("name").isNull() )  ? fieldData.get("name").asText() : "";
                        
                        String relationApp = (fieldData.get("relationApp") != null && !fieldData.get("relationApp").isNull() ) ? fieldData.get("relationApp").asText() : null;
                        String relationEntity = (fieldData.get("relationEntity") != null && !fieldData.get("relationEntity").isNull() )   ? fieldData.get("relationEntity").asText() : "";
                        String relationEntityFullName = (fieldData.get("relationEntityFullName") != null && !fieldData.get("relationEntityFullName").isNull() ) ? fieldData.get("relationEntityFullName").asText() : null;
                        String relationType = (fieldData.get("relationType") != null && !fieldData.get("relationType").isNull() ) ? fieldData.get("relationType").asText() : "NONE";
                        String dataType = (fieldData.get("dataType") != null && !fieldData.get("dataType").isNull() )  ? fieldData.get("dataType").asText() : "";

                        ObjectNode newDataSource = objectMapper.createObjectNode();
                        if (isUdcDataGrid) {
                            // 重复表字段数据源
                            newDataSource.put("dataFieldId", id);
                            newDataSource.put("type", "INPUT");
                            newDataSource.put("dataField", fieldName != null ? fieldName : name);
                            newDataSource.put("fullName", fullName);
                            newDataSource.put("readonly", false);
                            newDataSource.put("key", parentData.get("dataSource").get("key").asText() + "." + name);
                            newDataSource.put("dataTarget", formData.get("id").asText());
                            
                            ArrayNode fieldPath = objectMapper.createArrayNode();
                            fieldPath.add(parentData.get("dataSource").get("entity").get("id").asText());
                            fieldPath.add(id);
                            newDataSource.set("fieldPath", fieldPath);
                            
                            newDataSource.put("dataPath", parentData.get("dataSource").get("entity").get("dataField").asText());
                            newDataSource.put("inColumn", true);
                            newDataSource.put("relationApp", relationApp);
                            newDataSource.put("relationEntity", relationEntity);
                            newDataSource.put("relationEntityFullName", relationEntityFullName);
                            newDataSource.put("relationType", relationType);
                            newDataSource.put("dataType", dataType);
                            newDataSource.putNull("enumOptions");
                            newDataSource.putNull("parentEnumAttributeId");
                            newDataSource.putNull("parentEnumAttributeFullName");

                            ((ObjectNode)data).set("dataSource", newDataSource);

                            // 设置必要属性
                            ObjectNode settings = data.has("settings") ? 
                                (ObjectNode)data.get("settings") : objectMapper.createObjectNode();
                            settings.put("titleDisplay", "none");
                            ((ObjectNode)data).set("settings", settings);

                            // 更新父级columns数据
                            String columnId = RandomStringUtils.random(6, true, true);
                            ObjectNode parentSettings = parentData.has("settings") ? 
                                (ObjectNode)parentData.get("settings") : objectMapper.createObjectNode();
                            ArrayNode columns = parentSettings.has("columns") ? 
                                (ArrayNode)parentSettings.get("columns") : objectMapper.createArrayNode();
                            
                            ArrayNode gridTemplateColumns = parentSettings.has("gridTemplateColumns") ? 
                                (ArrayNode)parentSettings.get("gridTemplateColumns") : null;
                            int currentColumnsLength = columns.size();

                            ObjectNode column = objectMapper.createObjectNode();
                            column.put("align", "left");
                            column.put("componentType", "Input");
                            column.put("dataFieldCaption", data.has("settings") ? 
                                data.get("settings").get("name").asText() : "");
                            column.put("dataI18n", false);
                            column.put("dataIndex", fieldName);
                            column.put("dataType", "STRING");
                            column.put("dataFieldId", id);
                            column.put("fullName", fullName);
                            
                            ArrayNode columnFieldPath = objectMapper.createArrayNode();
                            columnFieldPath.add(parentData.get("dataSource").get("entity").get("id").asText());
                            columnFieldPath.add(id);
                            column.set("fieldPath", columnFieldPath);
                            
                            column.put("parentId", parentData.get("id").asText());
                            column.put("columnId", columnId);
                            column.put("columnIndex", columns.size());
                            column.put("widthType", "pt");
                            if (gridTemplateColumns != null && gridTemplateColumns.size() > 0) {
                                column.put("widthValue", gridTemplateColumns.get(
                                    currentColumnsLength != 0 ? currentColumnsLength + 1 : 0).asDouble());
                            } else {
                                column.put("widthValue", 100);
                            }

                            columns.add(column);
                            parentSettings.set("columns", columns);
                            ((ObjectNode)parentData).set("settings", parentSettings);

                            // 更新父级fieldBindMap
                            ObjectNode parentDataSource = (ObjectNode)parentData.get("dataSource");
                            ObjectNode fieldBindMap = parentDataSource.has("fieldBindMap") ? 
                                (ObjectNode)parentDataSource.get("fieldBindMap") : objectMapper.createObjectNode();
                            fieldBindMap.put(columnId, fieldName);
                            parentDataSource.set("fieldBindMap", fieldBindMap);

                        } else {
                            // 单一记录字段数据源
                            newDataSource.put("dataFieldId", id);
                            newDataSource.put("type", "INPUT");
                            newDataSource.put("dataField", fieldName != null ? fieldName : name);
                            newDataSource.put("fullName", fullName);
                            newDataSource.put("readonly", false);
                            newDataSource.put("key", isDoc ? 
                                formData.get("id").asText() + "." + id : 
                                formData.get("dataSource").get("key").asText() + "." + name);
                            newDataSource.put("dataTarget", formData.get("id").asText());
                            
                            ArrayNode fieldPath = objectMapper.createArrayNode();
                            if (isExtField) {
                                fieldPath.add(formData.get("dataSource").get("entity").get("id").asText() + entityName);
                                fieldPath.add(id);
                            } else {
                                fieldPath.add(id);
                            }
                            newDataSource.set("fieldPath", fieldPath);
                            
                            newDataSource.put("dataPath", "");
                            newDataSource.put("relationApp", relationApp);
                            newDataSource.put("relationEntity", relationEntity);
                            newDataSource.put("relationEntityFullName", relationEntityFullName);
                            newDataSource.put("relationType", relationType);
                            newDataSource.put("dataType", dataType);
                            newDataSource.putNull("enumOptions");
                            newDataSource.putNull("parentEnumAttributeId");
                            newDataSource.putNull("parentEnumAttributeFullName");

                            ((ObjectNode)data).set("dataSource", newDataSource);

                            // 更新Form组件的fieldBindMap
                            ObjectNode formDataSource = (ObjectNode)formData.get("dataSource");
                            ObjectNode fieldBindMap = formDataSource.has("fieldBindMap") ? 
                                (ObjectNode)formDataSource.get("fieldBindMap") : objectMapper.createObjectNode();
                            fieldBindMap.put(data.get("id").asText(), fieldName != null ? fieldName : name);
                            formDataSource.set("fieldBindMap", fieldBindMap);
                        }

                        // 处理参照控件
                        if ("UdcReference".equals(type)) {
                            ObjectNode settings = data.has("settings") ? 
                                (ObjectNode)data.get("settings") : objectMapper.createObjectNode();
                            
                            ObjectNode dataReference = objectMapper.createObjectNode();
                            dataReference.putNull("id");
                            dataReference.putNull("starterName");
                            dataReference.put("appName", relationApp);
//                            dataReference.put("fullName", ""); // ===========从referceFullNameMap获取==============
                            dataReference.put("entityFullName", relationEntity);
                            
                            settings.set("dataReference", dataReference);
                            ((ObjectNode)data).set("settings", settings);
                        }

                        // 更新父级数据源Fields
                        JsonNode parentComponent = isUdcDataGrid ? parentData : formData;
                        ObjectNode parentDataSource = (ObjectNode)parentComponent.get("dataSource");
                        ArrayNode fields = parentDataSource.has("fields") ? 
                            (ArrayNode)parentDataSource.get("fields") : objectMapper.createArrayNode();
                        
                        ObjectNode field = objectMapper.createObjectNode();
                        field.put("id", id);
                        field.put("name", fieldName != null ? fieldName : name);
                        ArrayNode fieldPath = objectMapper.createArrayNode();
                        fieldPath.add(id);
                        field.set("fieldPath", fieldPath);
                        field.put("fullName", fullName);
                        field.put("parentId", parentData.get("id").asText());
                        field.put("readonly", false);
                        
                        fields.add(field);
                        parentDataSource.set("fields", fields);
                    }
                }
            }
        }
    }

}
