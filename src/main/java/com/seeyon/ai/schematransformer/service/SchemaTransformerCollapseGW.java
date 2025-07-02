package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.dto.SchemaTransformGridTemplateDTO;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaTransformerCollapseGW {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 基于componentNode 和 gridTemplateDTO 生成组件结构
     * @param componentNode ocr组件节点
     * @param gridTemplateDTO 样式结构模板
     * @return 转换后的结构
     */
    public static ObjectNode generateComponentStructure(JsonNode componentNode, SchemaTransformGridTemplateDTO gridTemplateDTO) {
        //生成node
        ObjectNode templateNode = null;

        JsonNode levelNode = componentNode.get("level");
        JsonNode informationNode = componentNode.get("information");
        JsonNode dataSourceNode = componentNode.get("dataSource");
        String type = componentNode.get("type").asText();
        //按照 dataField去关联模版.
        Map<String, JsonNode> componentNodeMap = gridTemplateDTO.getDataFieldTemplateMap();
        Map<String, JsonNode> typeTemplateMap = gridTemplateDTO.getTypeTemplateMap();

        JsonNode titleNode = gridTemplateDTO.getTitleNode();
        JsonNode descNode = gridTemplateDTO.getDescNode();
        JsonNode componentDefaultNode = gridTemplateDTO.getComponentDefaultNode();

        JsonNode containerDefaultNode = gridTemplateDTO.getContainerDefaultNode();

        JsonNode labelDefaultNode = gridTemplateDTO.getLabelDefaultNode();

        if (dataSourceNode != null && dataSourceNode.has("dataField")) {
            String dataField = dataSourceNode.get("dataField").asText();
            if (componentNodeMap != null && componentNodeMap.get(dataField) != null) {
                //根据dataField去关联模版
                templateNode = (ObjectNode) componentNodeMap.get(dataField).deepCopy();
            }
            if (levelNode != null) {
                String text = levelNode.asText();
                ((ObjectNode)dataSourceNode).put("level", text);
            }
            if (informationNode != null) {
                String text = informationNode.asText();
                ((ObjectNode)dataSourceNode).put("information", text);
            }

        }

        //根据referGroup->groupType 来获取模版
        JsonNode referGroupNode = componentNode.get("referGroup");
        if (referGroupNode != null) {
            if (referGroupNode.has("groupType")) {
                String groupType = referGroupNode.get("groupType").asText();
                if ("title".equals(groupType) && titleNode != null) {
                    templateNode = (ObjectNode)titleNode.deepCopy();
                } else if ("desc".equals(groupType) && descNode != null) {
                    templateNode = (ObjectNode)descNode.deepCopy();
                }
            }
        }


        if(templateNode == null) {
            //根据type去关联模版
            templateNode = (ObjectNode) typeTemplateMap.get(type);

            if(templateNode == null ) {
                if("container".equalsIgnoreCase(type) && containerDefaultNode != null){
                    templateNode = (ObjectNode) containerDefaultNode.deepCopy();
                }if(SchemaTransformerUtil.isLabelOrFieldTitle(type) && labelDefaultNode != null){
                    templateNode = (ObjectNode) labelDefaultNode.deepCopy();
                }else if(componentDefaultNode != null) {
                    //根据默认属性去关联
                    templateNode = (ObjectNode) componentDefaultNode.deepCopy();
                }

            }
        }
        if(templateNode == null) {
            log.error("无效的模板节点");
            return null;
        }
        //生成随机id
        templateNode.put("id", SchemaTransformerUtil.generateShortUUID());

        //拷贝属性到templateNode
        List<String> propsNameList = Arrays.asList("type","dataSource","referGroup");
        copyNodeProps(componentNode,templateNode,propsNameList,type);

        //拷贝Settings属性到templateNode
        copySettingsOcrDsl2TemplateDsl(componentNode,templateNode,type);

        return templateNode == null ? null : templateNode.deepCopy();

    }



    /**
     *
     *  模版渲染gridCell结构
     * @param componentNode 组件结构
     * @param gridTemplateDTO 样式结构模板
     * @return 转换后的结构
     */
    public static ObjectNode generateGridCellStructure(JsonNode componentNode, SchemaTransformGridTemplateDTO gridTemplateDTO,int gridColumns) {
        ObjectNode targetGridNode = JsonNodeFactory.instance.objectNode();
        JsonNode templateNode = null;
        JsonNode templateNodeCopy = null;
        String  templateNodeId = null;
        JsonNode referGroupNode = componentNode.get("referGroup");
        //根据referGroup类型获取模板节点
        if (referGroupNode != null && !referGroupNode.isNull()) {
            if (referGroupNode.has("groupType")) {
                String groupType = referGroupNode.get("groupType").asText();
                if ("title".equals(groupType)) {
                    templateNode = gridTemplateDTO.getGridCellTitleNode();
                } else if ("component".equals(groupType)) {
                    templateNode = gridTemplateDTO.getGridCellComponentNode();
                } else if ("desc".equals(groupType)) {
                    templateNode = gridTemplateDTO.getGridCellDescNode();
                } else {
                    throw new RuntimeException("groupType:"+groupType+" 不是标准类型!");
                }

            }
        } else {
            //没有referGroup类型 , 根据gridCell中的 组件type=label来判断.
            String componentType = componentNode.get("type").asText();
            if(SchemaTransformerUtil.isLabelOrFieldTitle(componentType)){
                templateNode = gridTemplateDTO.getGridCellTitleNode();
            } else {
                templateNode = gridTemplateDTO.getGridCellComponentNode();
            }
        }

        if (templateNode == null || !templateNode.isObject()) {
            log.info("未找到gridCell模板节点");
            return null;
        }

        if(templateNode.has("sourceSchema") && templateNode.get("sourceSchema").has("id")){
            templateNodeId = templateNode.get("sourceSchema").get("id").asText();
        }else{
            log.error(templateNode.get("id")+": not find sourceSchema->id");
        }

        templateNodeCopy = templateNode.deepCopy();
        //生成gridCell 节点.
        targetGridNode.put("id",SchemaTransformerUtil.generateShortUUID());
        targetGridNode.put("type", "gridCell");
        if(StringUtils.isNotBlank(templateNodeId)) {
            targetGridNode.putObject("sourceSchema").put("id", templateNodeId);
        }

        //ocr网格cell坐标 替换到模版文件中
        updateGridCellCellColRow(componentNode,(ObjectNode) templateNodeCopy);


        ObjectNode targetGridNodeSettings = targetGridNode.putObject("settings");
        ObjectNode targetSettingsStylesNode = null;
        if(targetGridNodeSettings.has("styles")) {
            targetSettingsStylesNode = (ObjectNode) targetGridNodeSettings.get("styles");
        } else {
            targetSettingsStylesNode = targetGridNodeSettings.putObject("styles");
        }

        //模版样式 拷贝到 生成的 节点中
        List<String> propsNameList = Arrays.asList("bgColor","borderBottom","borderLeft","borderRight","borderTop","display","gridColumn","gridRow");
        JsonNode templateStylesNode = templateNodeCopy.get("settings").get("styles");
        copyNodeProps(templateStylesNode,(ObjectNode)targetSettingsStylesNode,propsNameList,"gridCell");


        //补全settings->styles
        //如果GridCellTitle中左边框没有, 则表示表格的左右边框都是没有的.
        boolean leftAndRightBorderIsNull = false;
        if(gridTemplateDTO.getGridCellTitleNode() != null){
            if(gridTemplateDTO.getGridCellTitleNode().get("settings").get("styles") !=null && ! gridTemplateDTO.getGridCellTitleNode().get("settings").get("styles").has("borderLeft")){
                leftAndRightBorderIsNull = true;
            }
        }

        fixGridCellStyles(targetGridNode,gridColumns,leftAndRightBorderIsNull);

        return targetGridNode.deepCopy();

    }


    public static void fixGridCellStyles(ObjectNode targetGridNode,int gridColumns,boolean leftAndRightBorderIsNull){
        if(targetGridNode.get("settings").has("styles")){
            JsonNode stylesNode = targetGridNode.get("settings").get("styles");
            JsonNode stylesBorderDefault = null;
            if(stylesNode.has("borderLeft")){
                stylesBorderDefault = stylesNode.get("borderLeft");
            } else if (stylesNode.has("borderRight")) {
                stylesBorderDefault = stylesNode.get("borderRight");
            } else if (stylesNode.has("borderTop")) {
                stylesBorderDefault = stylesNode.get("borderTop");
            } else if (stylesNode.has("borderBottom")) {
                stylesBorderDefault = stylesNode.get("borderBottom");
            }

            if(stylesBorderDefault != null) {
                if (!stylesNode.has("borderLeft")) {
                    ((ObjectNode)stylesNode).put("borderLeft",stylesBorderDefault);
                }
                if (!stylesNode.has("borderRight")) {
                    ((ObjectNode)stylesNode).put("borderRight",stylesBorderDefault);
                }
                if (!stylesNode.has("borderBottom")) {
                    ((ObjectNode)stylesNode).put("borderBottom",stylesBorderDefault);
                }
                if (!stylesNode.has("borderTop")) {
                    ((ObjectNode)stylesNode).put("borderTop",stylesBorderDefault);
                }
            }
            
            if(leftAndRightBorderIsNull) {
                if (stylesNode.has("gridColumn") && gridColumns > 0) {
                    String gridColumn = stylesNode.get("gridColumn").asText();
                    try {
                        // 验证gridColumn格式
                        if(gridColumn.contains("/")) {
                            String[] parts = gridColumn.split("/");
                            if(parts.length == 2) {
                                int start = Integer.parseInt(parts[0]);
                                int end = Integer.parseInt(parts[1]);
                                
                                if(start == 1) {
                                    ((ObjectNode) stylesNode).remove("borderLeft");
                                }
                                if(end == gridColumns + 1) {
                                    ((ObjectNode) stylesNode).remove("borderRight");
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.error("Invalid gridColumn format: " + gridColumn, e);
                    }
                }
            }
        }
    }

    /**
     * ocr网格cell坐标 转换为 页面dsl的cell坐标
     * @param ocrComponentNode
     * @param gridCellNode
     */
    public static void updateGridCellCellColRow(JsonNode ocrComponentNode,ObjectNode gridCellNode) {

        JsonNode cellColRow = ocrComponentNode.get("cellColRow");
        if (cellColRow != null) {
            int rowIndex = cellColRow.get("rowIndex").asInt();
            int flexRowSize = cellColRow.get("flexRowSize").asInt();
            int colIndex = cellColRow.get("colIndex").asInt();
            int flexColSize = cellColRow.get("flexColSize").asInt();

            int colStart = colIndex;
            int colEnd = colIndex + flexColSize;
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
            ObjectNode settingsNode = (ObjectNode) gridCellNode.get("settings");
            ObjectNode gridCellSettingsStyles = null;
            if (settingsNode.has("styles")) {
                gridCellSettingsStyles = (ObjectNode) settingsNode.get("styles");
            }else{
                gridCellSettingsStyles = settingsNode.putObject("styles");
            }

            if (isDisplay) {
                gridCellSettingsStyles.put("display", "flex");
            } else {
                gridCellSettingsStyles.put("display", "none");
            }

            gridCellSettingsStyles.put("gridRow", rowStart + "/" + rowEnd);
            gridCellSettingsStyles.put("gridColumn", colStart + "/" + colEnd);

        }
    }


    /**
     * 将sourceNode 中copyProps属性 拷贝到targetNode
     * @param ocrDsl ocrDslNode
     * @param templateDsl templateDslNode
     * @param copyProps  需要ocrDslNode拷贝的属性,拷贝到templateDslNode中
     * @param nodeType  ocr节点类别.
     */
    public static void copyNodeProps(JsonNode ocrDsl,ObjectNode templateDsl,List<String> copyProps ,String nodeType) {

        for (String copyProp : copyProps) {

            if(ocrDsl.has(copyProp)){
                JsonNode copyPropNode = ocrDsl.get(copyProp);
                if (copyPropNode != null) {

                    if("grid".equalsIgnoreCase(nodeType) && "titleName".equalsIgnoreCase(copyProp)){
                        //网格grid类别
                        templateDsl.put("title", copyPropNode.deepCopy());
                    } else if(SchemaTransformerUtil.isLabelOrFieldTitle(nodeType) && "titleName".equalsIgnoreCase(copyProp)){
                        //label格式兼容
                        templateDsl.put("content", copyPropNode.deepCopy());
                    }else {
                        templateDsl.put(copyProp, copyPropNode.deepCopy());
                    }
                }
            }
        }
    }

    /**
     * 将ocrDsl->settings 中copyProps属性 拷贝到templateDsl->settings
     * @param ocrDsl ocrDslNode
     * @param templateDsl templateDslNode
     */
    public static void copySettingsOcrDsl2TemplateDsl(JsonNode ocrDsl,ObjectNode templateDsl ,String type) {
        if(!ocrDsl.has("settings")){
            throw new RuntimeException("ocrDslNode 中不存在settings属性");
        }
        JsonNode ocrDslSettings = ocrDsl.get("settings");
        ObjectNode templateDslSettings = null;
        if(templateDsl.has("settings")){
            templateDslSettings = (ObjectNode)templateDsl.get("settings");
        }else{
            templateDslSettings = templateDsl.putObject("settings");
        }
        //获取ocrDslSettings节点所有属性名称
        Iterator<String> stringIterator = ocrDslSettings.fieldNames();

        List<String> propsNameList = new ArrayList<>();
        //stringIterator转List
        while (stringIterator.hasNext()) {
            propsNameList.add(stringIterator.next());
        }
        copyNodeProps(ocrDslSettings,templateDslSettings,propsNameList,type);
    }




    /**
     * 处理多行组件，为flexRowSize>1的节点插入额外行
     * @param components 需要处理的组件数组
     */
    public static void handleMultiRowComponents(ArrayNode components) {
        List<JsonNode> nodesToAdd = new ArrayList<>();

        for (JsonNode component : components) {
            JsonNode cellColRow = component.get("cellColRow");

            if (cellColRow != null && cellColRow.has("flexRowSize") && cellColRow.has("flexColSize")) {
                int flexColSize = cellColRow.get("flexColSize").asInt();
                int flexRowSize = cellColRow.get("flexRowSize").asInt();
                int rowIndex = cellColRow.get("rowIndex").asInt();
                int colIndex = cellColRow.get("colIndex").asInt();
                
                // 只为非起始位置的单元格创建新节点
                for(int i = 0; i < flexRowSize; i++) {
                    for(int j = 0; j < flexColSize; j++) {
                        // 跳过原始单元格位置
                        if(i == 0 && j == 0) {
                            continue;
                        }
                        
                        ObjectNode newNode = OBJECT_MAPPER.createObjectNode();
                        // 复制基本结构
                        newNode.put("type", "gridCell");
                        newNode.putObject("settings");

                        JsonNode levelNode = component.get("level");
                        JsonNode informationNode = component.get("information");
                        // 复制dataSource
                        if (component.has("dataSource")) {
                            ObjectNode dataSourceNode = component.get("dataSource").deepCopy();
                            if (levelNode != null) {
                                String text = levelNode.asText();
                                dataSourceNode.put("level", text);
                            }
                            if (informationNode != null) {
                                String text = informationNode.asText();
                                dataSourceNode.put("information", text);
                            }
                            newNode.set("dataSource", dataSourceNode);



                        }

                        // 创建cellColRow
                        ObjectNode newCellColRow = newNode.putObject("cellColRow");
                        newCellColRow.put("display","none");
                        newCellColRow.put("rowIndex", rowIndex + i);
                        newCellColRow.put("flexRowSize", 1);
                        newCellColRow.put("colIndex", colIndex + j);
                        newCellColRow.put("flexColSize", 1);

                        // 收集需要添加的节点
                        nodesToAdd.add(newNode);
                    }
                }
            }
        }

        // 将收集到的所有新节点添加到原始数组中
        for (JsonNode node : nodesToAdd) {
            components.add(node);
        }
    }


}
