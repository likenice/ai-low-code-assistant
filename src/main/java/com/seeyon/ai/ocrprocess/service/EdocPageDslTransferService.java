package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.enums.UdcStyleTypeEnum;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.EdocComponentsDslDto;
import com.seeyon.ai.ocrprocess.form.EdocDataSourceDto;
import com.seeyon.ai.ocrprocess.form.EdocEntityDto;
import com.seeyon.ai.ocrprocess.form.EdocGroupDto;
import com.seeyon.ai.ocrprocess.form.EdocGroupsDslDto;
import com.seeyon.ai.ocrprocess.form.EdocReferGroupDto;
import com.seeyon.ai.ocrprocess.form.EdocSettingDslDto;
import com.seeyon.ai.ocrprocess.form.EdocTableDto;
import com.seeyon.ai.ocrprocess.form.EdocTableStructureDto;
import com.seeyon.ai.ocrprocess.form.PageCellColRow;
import com.seeyon.ai.ocrprocess.form.response.EdocPageDslResponse;
import com.seeyon.boot.util.id.Ids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EdocPageDslTransferService {
    @Autowired
    private AiPromptSvcAppService aiPromptSvcAppService;
    @Autowired
    private EdocTableProcessService edocTableProcessService;
    private static final int lineDiff = 14;


    public String transfer(String tableName, List<EdocTableStructureDto> tableStructure, List<EdocEntityDto> entityDtos, AiFormFLowInfo aiFormFLowInfo, List<CellDto> cellDtos, Map<String, String> layoutMapping, String resize, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        String entityFilePath = "D:\\WX\\WeChat Files\\wxid_jhttztnj8zum22\\FileStorage\\File\\2025-04\\新建文件(3).txt";
        if (entityDtos == null || entityDtos.isEmpty()) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(entityFilePath));
                String entity = new String(bytes);
                ObjectMapper objectMapper = new ObjectMapper();
                entityDtos = objectMapper.readValue(entity, new TypeReference<List<EdocEntityDto>>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Map<String, EdocDataSourceDto> matchField = fieldMatchToAi(tableStructure, entityDtos, llmInitialAnalysisMap, llmConfidenceMap);
        userUseFormInfo.setGeneratedElementCount("{\"fields\":" + matchField.size() + "}");
        System.out.println("matchField: " + matchField);
        EdocPageDslResponse edocPageDslResponse = new EdocPageDslResponse();
        edocPageDslResponse.setTitleName(tableName);
        List<EdocGroupsDslDto> groups = edocPageDslResponse.getGroups();
        Integer maxX = getMaxX(tableStructure).get("maxX");
        Integer minX = getMaxX(tableStructure).get("minX");
        String[] split = resize.split(",");
        resize = Math.max(maxX, Integer.parseInt(split[0])) + "," + split[1];
        edocPageDslResponse.setResize(resize);
        Map<String, Object> map = edocTableProcessService.groupIdentify(tableStructure, cellDtos, layoutMapping, maxX);
        List<EdocTableDto> edocTableDtos = (List<EdocTableDto>) map.get("groups");
//        int maxX = (int) map.get("maxX");
        System.out.println("maxX: " + maxX);
        int containerIndex = 1;
        int gridIndex = 1;
        for (int i = 0; i < edocTableDtos.size(); i++) {
            EdocTableDto edocTableDto = edocTableDtos.get(i);
            EdocGroupsDslDto edocGroupsDslDto = new EdocGroupsDslDto();
            String type = edocTableDto.getType();
            edocGroupsDslDto.setId(Ids.gidLong());
            List<EdocGroupDto> edocGroupDtos = edocTableDto.getEdocGroupDtos();
            if (type.equals("grid")) {
                String name = "未命名网格" + gridIndex;
                edocGroupsDslDto.setName(name);
                edocGroupsDslDto.setType("grid");
                List<Integer> colList = getFieldColInfoByGrid(edocGroupDtos, maxX);
                Map<String, String> fieldRowInfo = getFieldRowInfoByGrid(edocGroupDtos, colList);
                log.info("grid-edocTableDto:{}", JSONUtil.toJsonStr(edocTableDto));
                log.info("grid-colList:{}", JSONUtil.toJsonStr(colList));
                log.info("grid-fieldRowInfo:{}", JSONUtil.toJsonStr(fieldRowInfo));
                girdField(edocGroupsDslDto, edocGroupDtos, colList, fieldRowInfo, matchField, name, maxX);
                gridIndex++;
            } else {
                boolean first = false;
                if (i == 0) {
                    first = true;
                }
                String name = "未命名组" + containerIndex;
                edocGroupsDslDto.setName(name);
                edocGroupsDslDto.setType("container");
                Map<String, Integer> fieldRowAndColInfo = getFieldRowAndColInfo(edocGroupDtos);
                log.info("container-edocTableDto:{}", JSONUtil.toJsonStr(edocTableDto));
                log.info("container-fieldRowAndColInfo:{}", JSONUtil.toJsonStr(fieldRowAndColInfo));
                int minWidth = (maxX - fieldRowAndColInfo.get("minX")) / fieldRowAndColInfo.get("maxCol");
                groupField(minWidth, edocGroupsDslDto, edocGroupDtos, fieldRowAndColInfo, maxX, matchField, name, minX, false);
                containerIndex++;
            }
            groups.add(edocGroupsDslDto);
        }
        edocPageDslResponse.setGroups(groups);
        log.info("dsl:{}", JSONUtil.toJsonStr(edocPageDslResponse));
        System.out.println(JSONUtil.toJsonStr(edocPageDslResponse));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(edocPageDslResponse);
        } catch (Exception e) {
            log.error("将对象转换为 JSON 字符串时出错", e);
            // 返回默认值或抛出自定义异常
            throw new PlatformException("将对象转换为 JSON 字符串时出错");
        }
    }

    public Map<String, Integer> getMaxX(List<EdocTableStructureDto> tableStructure) {
        int maxX = -1;
        int minX = -1;
        for (EdocTableStructureDto edocTableStructureDto : tableStructure) {
            String nLayout = edocTableStructureDto.getNLayout();
            String vLayout = edocTableStructureDto.getVLayout();
            maxX = Math.max(layoutStrToList(nLayout).get(2), maxX);
            minX = minX == -1 ? layoutStrToList(nLayout).get(0) : Math.min(layoutStrToList(nLayout).get(0), minX);
            if (vLayout != null && !vLayout.equals("")) {
                maxX = Math.max(layoutStrToList(vLayout).get(2), maxX);
                minX = minX == -1 ? layoutStrToList(vLayout).get(0) : Math.min(layoutStrToList(vLayout).get(0), minX);
            }
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("minX", minX);
        map.put("maxX", maxX);
        return map;
    }

    private List<Integer> getFieldColInfoByGrid(List<EdocGroupDto> edocGroupDtos, int maxX) {
        List<Integer> colList = new ArrayList<>();
        for (EdocGroupDto edocGroupDto : edocGroupDtos) {
            Boolean tsr = edocGroupDto.getTsr();
            if (tsr) {
                List<Integer> layout = edocGroupDto.getLayout();
                int x1 = layout.get(0);
                int x2 = layout.get(2);
                if (x2 == 999999) {
                    x2 = maxX;
                    layout.set(2, x2);
                }
                int endX = x2;
                if (colList.size() == 0) {
                    colList.add(x1);
                    colList.add(endX);
                } else {
                    boolean x1HasCloseValue = colList.stream()
                            .anyMatch(num -> Math.abs(num - x1) < 10);
                    boolean x2HasCloseValue = colList.stream()
                            .anyMatch(num -> Math.abs(num - endX) < 10);
                    if (!x1HasCloseValue) {
                        colList.add(x1);
                    }
                    if (!x2HasCloseValue) {
                        colList.add(endX);
                    }
                }
            }
        }
        boolean x1HasCloseValue = colList.stream()
                .anyMatch(num -> Math.abs(num - maxX) < 10);
        if (!x1HasCloseValue) {
            colList.add(maxX);
        }
        colList = colList.stream().sorted().collect(Collectors.toList());
        return colList;
    }

    private Map<String, String> getFieldRowInfoByGrid(List<EdocGroupDto> edocGroupDtos, List<Integer> colList) {
        Map<String, String> map = new LinkedHashMap<>();
        List<List<Integer>> mergeGrid = new ArrayList<>();
        int row = 1;
        int prevY = -1;
        int index = 1;
        for (EdocGroupDto edocGroupDto : edocGroupDtos) {
            String name = edocGroupDto.getName();
            Boolean tsr = edocGroupDto.getTsr();
            int nextPrevY = 0;
            int flexRowSize = 1;
            List<Integer> layout = edocGroupDto.getLayout();
            if (layout == null) {
                continue;
            }
            int y1 = layout.get(1);
            int y2 = layout.get(3);
            if (prevY == -1) {
                prevY = y1;
            }
            // true 换行
            boolean line = y1 - prevY > lineDiff;
            ;
            // 为画出线的x坐标补全
            if (!tsr) {
                // 每行的第一个元素 x坐标取最小的
                if (line || index == 1) {
                    layout.set(0, colList.get(0));
                } else {
                    EdocGroupDto prevEdocGroupDto = edocGroupDtos.get(index - 2);
                    List<Integer> prevLayout = prevEdocGroupDto.getLayout();
                    layout.set(0, prevLayout.get(2));
                }
                // 最后一个元素
                if (index == edocGroupDtos.size()) {
                    layout.set(2, colList.get(colList.size() - 1));
                } else {
                    EdocGroupDto nextEdocGroupDto = edocGroupDtos.get(index);
                    List<Integer> nextLayout = nextEdocGroupDto.getLayout();
                    // 下一个元素换行
                    if (Math.abs(y1 - nextLayout.get(1)) > lineDiff) {
                        layout.set(2, colList.get(colList.size() - 1));
                    } else {
                        if (nextEdocGroupDto.getTsr()) {
                            layout.set(2, nextLayout.get(0));
                        } else {
                            Integer nextX1 = nextLayout.get(0);
                            Integer x2 = layout.get(2);
                            List<Integer> inRangeWithMargin = findInRangeWithMargin(colList, nextX1, x2);
                            if (inRangeWithMargin.size() > 0) {
                                Collections.sort(inRangeWithMargin);
                                int size = inRangeWithMargin.size();
                                if (size % 2 == 1) {
                                    // 奇数情况，直接返回中间的数
                                    layout.set(2, inRangeWithMargin.get(size / 2));

                                } else {
                                    layout.set(2, inRangeWithMargin.get(size / 2 - 1));
                                }
                            } else {
                                layout.set(2, Math.max(nextX1, x2));
                                colList.add(Math.max(nextX1, x2));
                                Collections.sort(colList);
                            }
                        }
                    }
                }
                edocGroupDto.setLayout(layout);
            }

            // 行处理
            if (line) {
                row++;
                prevY = y1;
            }

            for (int i = index; i < edocGroupDtos.size(); i++) {
                EdocGroupDto nextEdocGroupDto = edocGroupDtos.get(i);
                List<Integer> nextLayout = nextEdocGroupDto.getLayout();
                if (layout == null) {
                    continue;
                }
                int nextY1 = nextLayout.get(1);
                int nextY2 = nextLayout.get(3);
                if (nextPrevY == 0) {
                    nextPrevY = y1;
                }
                if (Math.abs(nextY1 - nextPrevY) > lineDiff && nextY1 - y1 > lineDiff && y2 - nextY1 > lineDiff) {
                    flexRowSize++;
                }
                if (Math.abs(nextY1 - nextPrevY) > lineDiff) {
                    nextPrevY = nextY1;
                }
            }
            edocGroupDto.setRowInfo(row + "," + flexRowSize);
            if (flexRowSize > 1) {
                List<Integer> list = new ArrayList<>();
                list.add(row);
                list.add(flexRowSize);
                if (!mergeGrid.contains(list)) {
                    mergeGrid.add(list);
                }
            }
            index++;
        }
        // 总行数
        map.put("maxRow", String.valueOf(row));
        // 合并单元格信息
        map.put("mergeGrid", JSONUtil.toJsonStr(mergeGrid));
        return map;
    }


    public List<Integer> findInRangeWithMargin(List<Integer> list, int a, int b) {
        // 确定区间的最小值和最大值
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        // 扩展区间范围，允许误差为10
        int lower = min - 10;
        int upper = max + 10;

        // 筛选出在扩展区间内的元素
        return list.stream()
                .filter(num -> num >= lower && num <= upper)
                .collect(Collectors.toList());
    }

    private void girdField(EdocGroupsDslDto edocGroupsDslDto, List<EdocGroupDto> edocGroupDtos, List<Integer> colList,
                           Map<String, String> fieldRowInfo, Map<String, EdocDataSourceDto> matchField, String titleName, int maxX) {
        int maxRow = Integer.parseInt(fieldRowInfo.get("maxRow"));
        // 容器信息
        EdocSettingDslDto settings = new EdocSettingDslDto();
        List<EdocComponentsDslDto> components = edocGroupsDslDto.getComponents();
        edocGroupsDslDto.setComponents(components);
        edocGroupsDslDto.setSettings(settings);
        for (int i = 0; i < edocGroupDtos.size(); i++) {
            EdocGroupDto edocGroupDto = edocGroupDtos.get(i);
            List<EdocGroupDto> edocGroupDtosGrid = edocGroupDto.getEdocGroupDtos();
            String name = edocGroupDto.getName();
            List<Integer> layout = edocGroupDto.getLayout();
            Long relationId = edocGroupDto.getRelationId();
            String relationName = edocGroupDto.getRelationName();
            String rowIndex = edocGroupDto.getRowInfo().split(",")[0];
            String flexRowSize = edocGroupDto.getRowInfo().split(",")[1];
            int x1 = layout.get(0);
            int x2 = layout.get(2);
            int y = layout.get(1);
            int y2 = layout.get(3);
            String dataType = edocGroupDto.getDataType();
            // 字段settings
            if (dataType != null && dataType.equals("field") && edocGroupDto.getValueLayout() != null) {
                x2 = edocGroupDto.getValueLayout().get(2);
            }
            // 特殊情况处理
            if (i + 1 < edocGroupDtos.size()) {
                EdocGroupDto nextEdocGroupDto = edocGroupDtos.get(i + 1);
                List<Integer> nextLayout = nextEdocGroupDto.getLayout();
                if (Math.abs(nextLayout.get(1) - y) > lineDiff && maxX - x2 < 55) {
                    x2 = maxX;
                }
            } else {
                if (maxX - x2 < 55) {
                    x2 = maxX;
                }
            }
            List<Integer> inRangeWithMargin = findInRangeWithMargin(colList, x1, x2);
            if (inRangeWithMargin.size() == 0) {
                log.error("未判断出列数据：{},坐标：{},列坐标集合：{}", JSONUtil.toJsonStr(edocGroupDto), JSONUtil.toJsonStr(layout), JSONUtil.toJsonStr(colList));
                continue;
            }
            int colIndex = 0;
            int flexColSize = inRangeWithMargin.size() - 1;
            for (int m = 0; m < colList.size(); m++) {
                if (colList.get(m) == inRangeWithMargin.get(0)) {
                    colIndex = m + 1;
                }
            }
            // comment-settings
            EdocSettingDslDto componentsSettings = new EdocSettingDslDto();
            List<Integer> lastLayout = new ArrayList<>();
            lastLayout.add(x1);
            lastLayout.add(y);
            lastLayout.add(x2);
            lastLayout.add(y2);
            componentsSettings.setLayout(lastLayout);
            // components
            EdocComponentsDslDto edocComponentsDslDto = new EdocComponentsDslDto();
            // 字段信息
            edocComponentsDslDto.setId(Ids.gidLong());
            // compoents----y
            edocComponentsDslDto.setY(y);
            // components————settings
            edocComponentsDslDto.setSettings(componentsSettings);
            components.add(edocComponentsDslDto);
            // 字段占比信息
            PageCellColRow pageCellColRow = new PageCellColRow();
            pageCellColRow.setRowIndex(Integer.parseInt(rowIndex));
            pageCellColRow.setFlexRowSize(Integer.parseInt(flexRowSize));
            pageCellColRow.setColIndex(colIndex);
            pageCellColRow.setFlexColSize(flexColSize);
            // components————cellColRow
            edocComponentsDslDto.setCellColRow(pageCellColRow);
            // components-groups
            if (edocGroupDtosGrid != null && edocGroupDtosGrid.size() > 0) {
                edocComponentsDslDto.setType("gridCell");
                componentsSettings.setTitleName("");
                List<EdocGroupsDslDto> gridList = new ArrayList<>();
                EdocGroupsDslDto edocGroupsDslDtoGrid = new EdocGroupsDslDto();
                gridList.add(edocGroupsDslDtoGrid);
                edocComponentsDslDto.setGroups(gridList);
                edocGroupsDslDtoGrid.setType("container");
                Map<String, Integer> fieldRowAndColInfo = getFieldRowAndColInfo(edocGroupDtosGrid);
                int minWidth = (x2 - fieldRowAndColInfo.get("minX")) / fieldRowAndColInfo.get("maxCol");
                groupField(minWidth, edocGroupsDslDtoGrid, edocGroupDtosGrid, fieldRowAndColInfo, x2, matchField, "", 0, true);
                continue;
            }
            componentsSettings.setAlign("left");
            String settingTitleName = relationName.replaceFirst("^\\*", "")  // 去除开头的*
                    .replaceFirst(":$", "");   // 去除结尾的:
            componentsSettings.setTitleName(settingTitleName);
            if (name.contains("*")) {
                Map<String, Boolean> m = new HashMap<>();
                m.put("required", true);
                componentsSettings.setValidation(m);
            } else {
                Map<String, Boolean> m = new HashMap<>();
                m.put("required", false);
                componentsSettings.setValidation(m);
            }
            // 字段关联组信息
            EdocReferGroupDto edocReferGroupDto = new EdocReferGroupDto();
            edocReferGroupDto.setGroupId(relationId);
            edocReferGroupDto.setGroupName(relationName);

            // components————type
            if (SchemaTransformerUtil.isLabelOrFieldTitle(dataType)) {
                edocComponentsDslDto.setType("label");
                edocReferGroupDto.setGroupType("title");
                componentsSettings.setTitleDisplay("left");
            } else if (dataType.equals("component")) {
                String value = edocGroupDto.getValue();
                List<Integer> valueLayout = edocGroupDto.getValueLayout();
                EdocDataSourceDto edocDataSourceDto = matchField.get(relationName);
                componentsSettings.setTitleDisplay("none");
                for (EdocComponentsDslDto component : components) {
                    if (component.getReferGroup() == null) {
                        continue;
                    }
                    Long groupId = component.getReferGroup().getGroupId();
                    if (relationId == groupId) {
                        if (name.equals("") && edocDataSourceDto != null) {
                            String componentType = edocDataSourceDto.getComponentType();
                            if (componentType != null) {
                                if (componentType.equals(UdcStyleTypeEnum.UIBUSSINESSEDOCOPINIONBOX.getType())) {
                                    component.getSettings().setTitleDisplay("top");
                                } else {
                                    component.getSettings().setTitleDisplay("left");
                                }
                            } else {
                                component.getSettings().setTitleDisplay("left");

                            }
                        } else {
                            int vY = component.getY();
                            if (Math.abs(y - vY) > lineDiff) {
                                component.getSettings().setTitleDisplay("top");
                            } else {
                                component.getSettings().setTitleDisplay("left");
                            }

                        }
                    }
                }
                // components————dataSource
                if (edocDataSourceDto != null) {
                    edocComponentsDslDto.setType(edocDataSourceDto.getComponentType());
                    edocComponentsDslDto.setDataSource(edocDataSourceDto);
                    edocComponentsDslDto.setLevel(edocDataSourceDto.getLevel());
                    edocComponentsDslDto.setInformation(edocDataSourceDto.getInformation());
                } else {
                    edocComponentsDslDto.setType("input");
                    edocComponentsDslDto.setDataSource(new EdocDataSourceDto());
                    log.info("未匹配到数据源数据：{}", JSONUtil.toJsonStr(edocGroupDto));
                }
                edocReferGroupDto.setGroupType("component");
            } else {
                String value = edocGroupDto.getValue();
                List<Integer> valueLayout = edocGroupDto.getValueLayout();
                EdocDataSourceDto edocDataSourceDto = matchField.get(relationName);
                if (name.startsWith("[{")) {
                    if (value.equals("")) {
                        componentsSettings.setTitleDisplay("none");
                    }
                } else {
                    if ((value == null || (value != null && value.equals(""))) && edocDataSourceDto != null) {
                        String componentType = edocDataSourceDto.getComponentType();
                        if (componentType != null) {
                            if (componentType.equals(UdcStyleTypeEnum.UIBUSSINESSEDOCOPINIONBOX.getType())) {
                                componentsSettings.setTitleDisplay("top");
                            } else {
                                componentsSettings.setTitleDisplay("left");

                            }
                        } else {
                            componentsSettings.setTitleDisplay("left");
                        }

                    } else {
                        int vY = valueLayout.get(1);
                        if (Math.abs(y - vY) > lineDiff) {
                            componentsSettings.setTitleDisplay("top");
                        } else {
                            componentsSettings.setTitleDisplay("left");
                        }
                    }
                }
                // components————dataSource
                if (edocDataSourceDto != null) {
                    edocComponentsDslDto.setType(edocDataSourceDto.getComponentType());
                    edocComponentsDslDto.setDataSource(edocDataSourceDto);
                    edocComponentsDslDto.setLevel(edocDataSourceDto.getLevel());
                    edocComponentsDslDto.setInformation(edocDataSourceDto.getInformation());
                } else {
                    edocComponentsDslDto.setType("input");
                    edocComponentsDslDto.setDataSource(new EdocDataSourceDto());
                    log.info("未匹配到数据源数据：{}", JSONUtil.toJsonStr(edocGroupDto));
                }
                edocReferGroupDto.setGroupType("component");
            }
            // components————referGroup
            edocComponentsDslDto.setReferGroup(edocReferGroupDto);
        }
        settings.setTitleName(titleName);
        settings.setGridType("complex");
        List<Double> gridTemplateColumns = new ArrayList<>();
        List<Integer> gridTemplateRows = new ArrayList<>();
        for (int i = 0; i < maxRow; i++) {
            gridTemplateRows.add(28);
        }
        int allWidth = colList.get(colList.size() - 1) - colList.get(0);
        int oneColWidth = allWidth / (colList.size() - 1);
        for (int i = 0; i < colList.size(); i++) {
            if (i == colList.size() - 1) {
                break;
            }
            Integer start = colList.get(i);
            Integer end = colList.get(i + 1);
            int width = end - start;
            double result = (double) width / oneColWidth;
            DecimalFormat df = new DecimalFormat("#.##");
            gridTemplateColumns.add(Double.parseDouble(df.format(result)));
        }
        settings.setGridTemplateColumns(gridTemplateColumns);
        settings.setGridTemplateRows(gridTemplateRows);

    }

    private void groupField(int minWidth, EdocGroupsDslDto edocGroupsDslDto, List<EdocGroupDto> edocGroupDtos,
                            Map<String, Integer> fieldRowAndColInfo, int maxX, Map<String, EdocDataSourceDto> matchField, String titleName, int minX, boolean gridGroup) {
        int colIndex = 0;
        int prevY = 0;
        int maxCol = 0;
        Integer firstCol = fieldRowAndColInfo.get("maxCol");
        // 容器信息
        EdocSettingDslDto settings = new EdocSettingDslDto();
        settings.setGap("10px 10px");
        List<EdocComponentsDslDto> components = edocGroupsDslDto.getComponents();
        edocGroupsDslDto.setComponents(components);
        edocGroupsDslDto.setSettings(settings);
        boolean center = false;
        List<Integer> rowFirstField = new ArrayList<>();
        boolean newLine = false;
        Integer leftGap = 0;
        Integer rightGap = 0;
        for (int i = 0; i < edocGroupDtos.size(); i++) {
            EdocGroupDto edocGroupDto = edocGroupDtos.get(i);
            String name = edocGroupDto.getName();
            List<Integer> layout = edocGroupDto.getLayout();
            List<Integer> valueLayout = edocGroupDto.getLayout();
            Long relationId = edocGroupDto.getRelationId();
            String relationName = edocGroupDto.getRelationName();
            String rowIndex = edocGroupDto.getRowInfo().split(",")[0];
            String flexRowSize = edocGroupDto.getRowInfo().split(",")[1];
            String dataType = edocGroupDto.getDataType();
            int y = layout.get(1);
            int x = layout.get(0);
            int x1 = layout.get(2);
            int fieldWidth = 0;
            if (rowFirstField.size() == 0) {
                rowFirstField.addAll(layout);
                leftGap = x - minX;
            } else {
                if (newLine) {
                    rowFirstField.clear();
                    rowFirstField.addAll(layout);
                    leftGap = x - minX;
                    newLine = false;
                }
            }
            // 最后一个元素
            if (i + 1 == edocGroupDtos.size()) {
                x1 = valueLayout.get(2);
                fieldWidth = valueLayout.get(2) - x;
                rightGap = maxX - (valueLayout.get(2) + Math.round(maxX * 0.15f));
            } else {
                if (i + 1 < edocGroupDtos.size()) {
                    EdocGroupDto next = edocGroupDtos.get(i + 1);
                    List<Integer> nextLayout = next.getLayout();
                    int nextX = nextLayout.get(0);
                    int nextY = nextLayout.get(1);
                    if (Math.abs(nextY - y) > lineDiff) {
                        x1 = maxX;
                        fieldWidth = maxX - x;
                        newLine = true;
                    } else {
                        x1 = nextX;
                        fieldWidth = nextX - x;
                    }
                } else {
                    x1 = maxX;
                    fieldWidth = maxX - x;
                }
            }
            int round = (int) Math.round(fieldWidth / (double) minWidth);
            if (round == 0) {
                round = 1;
            }
            if (colIndex == 0) {
                colIndex = 1;
            } else {
                if (Math.abs(prevY - y) > lineDiff) {
                    colIndex = 1;
                }
            }
            boolean b = false;
            // 独占一行
//            if (i + 1 == edocGroupDtos.size() && Math.abs(prevY - y) > lineDiff) {
//                b = true;
//            }
            if (i + 1 < edocGroupDtos.size()) {
                EdocGroupDto next = edocGroupDtos.get(i + 1);
                List<Integer> nextLayout = next.getLayout();
                Integer nextY = nextLayout.get(1);
                if (i == 0 && Math.abs(nextY - y) > lineDiff) {
                    b = true;
                }
                if (Math.abs(prevY - y) > lineDiff && Math.abs(nextY - y) > lineDiff) {
                    b = true;
                }

            }
            prevY = y;
            String settingTitleName = name.replaceFirst("^\\*", "")  // 去除开头的*
                    .replaceFirst(":$", "");
            // 字段settings
            EdocSettingDslDto componentsSettings = new EdocSettingDslDto();
            layout.set(2, x1);
            componentsSettings.setLayout(layout);
            componentsSettings.setTitleName(settingTitleName);
            componentsSettings.setAlign("left");
            // setting————flexRowSize
            if (b) {
                componentsSettings.setFlexRowSize(-1);
            } else {
                componentsSettings.setFlexRowSize(round);
            }
            Map<String, Boolean> m = new HashMap<>();
            if (name.contains("*")) {
                m.put("required", true);
                componentsSettings.setValidation(m);
            } else {
                m.put("required", false);
                componentsSettings.setValidation(m);
            }

            // 字段关联组信息
            EdocReferGroupDto edocReferGroupDto = new EdocReferGroupDto();
            edocReferGroupDto.setGroupId(relationId);
            edocReferGroupDto.setGroupName(relationName);
            // 字段信息
            EdocComponentsDslDto edocComponentsDslDto = new EdocComponentsDslDto();
            edocComponentsDslDto.setId(Ids.gidLong());
            // components————settings titleDisplay
            // components----y
            edocComponentsDslDto.setY(y);
            // components————settings
            edocComponentsDslDto.setSettings(componentsSettings);
            // components————type
            if (SchemaTransformerUtil.isLabelOrFieldTitle(dataType)) {
                edocComponentsDslDto.setType("label");
                edocReferGroupDto.setGroupType("title");
                componentsSettings.setTitleDisplay("left");
            } else {
                String value = edocGroupDto.getValue();
//                List<Integer> valueLayout = edocGroupDto.getValueLayout();
                EdocDataSourceDto edocDataSourceDto = matchField.get(relationName);
                if (name.startsWith("[{")) {
                    if (value == null || value.equals("")) {
                        componentsSettings.setTitleDisplay("none");
                    }
                } else {
                    if ((value == null || (value != null && value.equals(""))) && edocDataSourceDto != null) {
                        String componentType = edocDataSourceDto.getComponentType();
                        if (componentType != null) {
                            if (componentType.equals(UdcStyleTypeEnum.UIBUSSINESSEDOCOPINIONBOX.getType())) {
                                componentsSettings.setTitleDisplay("top");
                            } else {
                                componentsSettings.setTitleDisplay("left");
                            }
                        } else {
                            componentsSettings.setTitleDisplay("left");
                        }

                    } else {
                        if (valueLayout != null) {
                            int vY = valueLayout.get(1);
                            if (Math.abs(y - vY) > lineDiff) {
                                componentsSettings.setTitleDisplay("top");
                            } else {
                                componentsSettings.setTitleDisplay("left");
                            }
                        } else {
                            componentsSettings.setTitleDisplay("left");
                        }

                    }
                }
                // components————dataSource
                if (edocDataSourceDto != null) {
                    edocComponentsDslDto.setType(edocDataSourceDto.getComponentType());
                    edocComponentsDslDto.setDataSource(edocDataSourceDto);
                    edocComponentsDslDto.setLevel(edocDataSourceDto.getLevel());
                    edocComponentsDslDto.setInformation(edocDataSourceDto.getInformation());
                } else {
                    edocComponentsDslDto.setType("input");
                    edocComponentsDslDto.setDataSource(new EdocDataSourceDto());
                    log.info("未匹配到数据源数据：{}", JSONUtil.toJsonStr(edocGroupDto));
                }
                edocReferGroupDto.setGroupType("component");
            }

            // components————referGroup
            edocComponentsDslDto.setReferGroup(edocReferGroupDto);
            components.add(edocComponentsDslDto);
            colIndex = colIndex + round;
            maxCol = Math.max(maxCol, (colIndex - 1));
        }
        boolean processJustifyContent = false;
        if (!gridGroup) {
            double wInput = 0.17 * maxX;
            if (Math.abs(leftGap - rightGap) <= wInput && leftGap > wInput) {
                maxCol = maxCol * 2 + 1;
                settings.setJustifyContent("center");
                processJustifyContent = true;
            } else if (rightGap <= wInput && Math.abs(leftGap - rightGap) > 0.5 * maxX) {
                maxCol = maxCol * 2 + 1;
                processJustifyContent = true;
                settings.setJustifyContent("flex-end");
            }
        }
        if (!processJustifyContent) {
            EdocComponentsDslDto edocComponentsDslDto = components.get(components.size() - 1);
            EdocSettingDslDto settings1 = edocComponentsDslDto.getSettings();
            List<Integer> layout = settings1.getLayout();
            layout.set(2, maxX);
            settings1.setLayout(layout);
        }
        settings.setTitleName(titleName);
        settings.setGridType("container");
//        if (firstCol == 1) {
//            settings.setFlexDirection("column");
//            settings.setAlignItems("stretch");
//        } else {
        settings.setFlexWrap("wrap");
        settings.setColumns(maxCol);
//        }
        int allFlexCloSize = 0;
        for (int i = 0; i < components.size(); i++) {
            EdocComponentsDslDto edocComponentsDslDto = components.get(i);
            EdocSettingDslDto settings1 = edocComponentsDslDto.getSettings();
            Integer flexColSize = settings1.getFlexRowSize();
            int y = edocComponentsDslDto.getY();
            if (flexColSize == -1) {
                settings1.setFlexRowSize(maxCol);
                continue;
            }
            allFlexCloSize = allFlexCloSize + flexColSize;
            if (y == 0) {

            } else {
                if (i + 1 < components.size()) {
                    EdocComponentsDslDto next = components.get(i + 1);
                    int nextY = next.getY();
                    if (Math.abs(nextY - y) > lineDiff) {
                        if (maxCol - allFlexCloSize > 0) {
                            settings1.setFlexRowSize(maxCol - allFlexCloSize + flexColSize);
                        }
//                        y = nextY;
                        allFlexCloSize = 0;
                    }
                } else {
//                    if (maxCol - allFlexCloSize > 0) {
//                        settings1.setFlexRowSize(maxCol - allFlexCloSize + flexColSize);
//                    }
                    allFlexCloSize = 0;
                }
            }

        }
    }

    public static void main(String[] args) {
/*        int leftGap = 477;
        int rightGap = 49;
        int maxX = 707;
        String name = "发文单居右2";*/
/*        int leftGap = 156 ;
        int rightGap = 58;
        int maxX = 624 ;
        String name = "发文单多个居中";*/
//        int leftGap = 408;
//        int rightGap = 656 ;
//        int maxX = 2005 ;
//        String name = "发文单居中清晰";
        int leftGap = 89;
        int rightGap = 722;
        int maxX = 1029;
        String name = "收文居左";
        double wInput = 0.17 * maxX;
        if (Math.abs(leftGap - rightGap) <= wInput && leftGap > wInput) {
            System.out.println("center");
        } else if (rightGap <= wInput && Math.abs(leftGap - rightGap) > 0.5 * maxX) {
            System.out.println("flex-end");
        } else {
            System.out.println("left");
        }

    }

    /**
     * 获取字段的行列信息
     *
     * @param edocGroupDtos
     * @return
     */
    private Map<String, Integer> getFieldRowAndColInfo(List<EdocGroupDto> edocGroupDtos) {
        Map<String, Integer> map = new LinkedHashMap<>();
        int row = 1;
        int prevY = 0;
        int col = 1;
        int maxCol = 0;
        int minX = 0;
        for (EdocGroupDto edocGroupDto : edocGroupDtos) {
            List<Integer> layout = edocGroupDto.getLayout();
            int x1 = layout.get(0);
            int y1 = layout.get(1);
            if (minX == 0) {
                minX = x1;
            } else {
                minX = Math.min(minX, x1);
            }
            if (prevY == 0) {
                prevY = y1;
            } else {
                if (y1 - prevY > lineDiff) {
                    row++;
                    prevY = y1;
                    maxCol = maxCol == 0 ? col : (col * maxCol) / gcd(col, maxCol);
                    col = 1;
                } else {
                    col++;
                }
            }
            edocGroupDto.setRowInfo(row + "," + 1);
        }
        if (maxCol == 0) {
            maxCol = col;
        }
        map.put("maxRow", row);
        map.put("maxCol", maxCol);
        map.put("minX", minX);
        return map;
    }


    private List<Integer> layoutStrToList(String layout) {
        layout = layout.replaceAll(" ", "");
        return Arrays.stream(layout.substring(1, layout.length() - 1).split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }


    public int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    //大模型数据匹配
    private Map<String, EdocDataSourceDto> fieldMatchToAi
    (List<EdocTableStructureDto> fieldList, List<EdocEntityDto> entityDtos, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        // 实体预处理
        Map<String, EdocEntityDto> entityDtoMap = new HashMap<>();
        List<String> entityNameList = new ArrayList<>();
        List<String> noMatchField = new ArrayList<>();
        for (EdocEntityDto entityDto : entityDtos) {
            String caption = entityDto.getCaption();
            if (caption.contains("废弃")) {
                continue;
            }
            EdocEntityDto edocEntityDto = entityDtoMap.get(caption);
            if (edocEntityDto != null) {
                String relationApp = edocEntityDto.getRelationApp();
                if (relationApp == null || relationApp.equals("null") || relationApp.startsWith("edoc")) {
                    continue;
                }
            }
            entityNameList.add(caption);
            entityDtoMap.put(caption, entityDto);
        }
        Map<String, EdocDataSourceDto> map = new HashMap<>();
        for (EdocTableStructureDto fieldMap : fieldList) {
            String fieldName = fieldMap.getFn();
            String fieldType = fieldMap.getType();
            String fieldValue = "";
            String oldFieldValue = "";
            if (fieldType.equals("f")) {
                oldFieldValue = fieldMap.getFv();
                fieldValue = oldFieldValue.replaceAll("\\[\\{", "").replaceAll("}]", "");
            } else {
                continue;
            }
            if (fieldName.contains("文号") || fieldValue.contains("文号")) {
                EdocEntityDto edocEntityDto = entityDtoMap.get("文号");
                if (edocEntityDto == null) {
                    EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                    eodcDataSourceDto.setLevel(2);
                    eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                    eodcDataSourceDto.setComponentType("input");
                    map.put(fieldName, eodcDataSourceDto);
                    continue;
                }
                String componentType = getType("UIBUSINESSEDOCMARK");
                EdocDataSourceDto eodcDataSourceDto = EdocDataSourceDto.convertToEdocEntity(edocEntityDto, componentType, 0, "");
                map.put(fieldName, eodcDataSourceDto);
                continue;
            }
            if (fieldName.contains("密级") || fieldValue.contains("密级")) {
                EdocEntityDto edocEntityDto = entityDtoMap.get("密级");
                if (edocEntityDto == null) {
                    EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                    eodcDataSourceDto.setLevel(2);
                    eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                    eodcDataSourceDto.setComponentType("input");
                    map.put(fieldName, eodcDataSourceDto);
                    continue;
                }
                String componentType = getType(edocEntityDto.getDataType());
                EdocDataSourceDto eodcDataSourceDto = EdocDataSourceDto.convertToEdocEntity(edocEntityDto, componentType, 0, "");
                map.put(fieldName, eodcDataSourceDto);
                continue;
            }
            if (entityNameList.contains(fieldName)) {
                EdocEntityDto edocEntityDto = entityDtoMap.get(fieldName);
                if (edocEntityDto == null) {
                    EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                    eodcDataSourceDto.setLevel(2);
                    eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                    eodcDataSourceDto.setComponentType("input");
                    map.put(fieldName, eodcDataSourceDto);
                    continue;
                }
                String componentType = "";
                if (edocEntityDto.getCaption().contains("意见")) {
                    componentType = getType("UIBUSSINESSEDOCOPINIONBOX");
                } else if (edocEntityDto.getCaption().equals("主送机关") || edocEntityDto.getCaption().equals("抄送机关")) {
                    componentType = getType("UIBUSINESSSELECTPEOPLE");
                } else {
                    componentType = getType(edocEntityDto.getDataType());
                }
                EdocDataSourceDto eodcDataSourceDto = EdocDataSourceDto.convertToEdocEntity(edocEntityDto, componentType, 0, "");
                map.put(fieldName, eodcDataSourceDto);
            } else if (entityNameList.contains(fieldValue)) {
                EdocEntityDto edocEntityDto = entityDtoMap.get(fieldValue);
                if (edocEntityDto == null) {
                    EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                    eodcDataSourceDto.setLevel(2);
                    eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                    eodcDataSourceDto.setComponentType("input");
                    map.put(fieldName, eodcDataSourceDto);
                    continue;
                }
                String componentType = "";
                if (edocEntityDto.getCaption().contains("意见")) {
                    componentType = getType("UIBUSSINESSEDOCOPINIONBOX");
                } else if (edocEntityDto.getCaption().equals("主送机关") || edocEntityDto.getCaption().equals("抄送机关")) {
                    componentType = getType("UIBUSINESSSELECTPEOPLE");
                } else {
                    componentType = getType(edocEntityDto.getDataType());
                }
                EdocDataSourceDto eodcDataSourceDto = EdocDataSourceDto.convertToEdocEntity(edocEntityDto, componentType, 0, "");
                map.put(fieldName, eodcDataSourceDto);
            } else {
                noMatchField.add(fieldName);
            }
        }
        try {
            if (noMatchField.size() > 0) {
                AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("matched", noMatchField.toString());
                requestMap.put("matching", entityNameList.toString());
                aiPromptSvcCallDto.setPromptCode("edoc_entity_match");
                aiPromptSvcCallDto.setInput("按照我的要求运行");
                aiPromptSvcCallDto.setPromptVarMap(requestMap);
                String response = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
                });
                Iterator iterator = responseMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = String.valueOf(iterator.next());
                    String value = JSONUtil.toJsonStr(responseMap.get(key));
                    JsonNode jsonArray = objectMapper.readTree(value);
                    List<Map<String, Object>> desList = new ArrayList<>();
                    String finalMatchName = "";
                    double maxConfidence = 0.0;
                    String componentType = "";
                    for (JsonNode o : jsonArray) {
                        Map<String, Object> desMap = new HashMap<>();
                        String matchName = o.get("value").asText();
                        double confidence = o.get("confidence").asDouble();
                        if (finalMatchName.equals("")) {
                            maxConfidence = confidence;
                            finalMatchName = matchName;
                        } else {
                            int compare = Double.compare(maxConfidence, confidence);
                            if (compare == -1) {
                                finalMatchName = matchName;
                                maxConfidence = confidence;
                            }
                        }
                        desMap.put("matchName", matchName);
                        desMap.put("confidence", confidence);
                        desList.add(desMap);
                    }
                    if (finalMatchName.equals("")) {
                        EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                        eodcDataSourceDto.setLevel(2);
                        eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                        eodcDataSourceDto.setComponentType("input");
                        map.put(key, eodcDataSourceDto);
                    } else {
                        EdocEntityDto edocEntityDto = entityDtoMap.get(finalMatchName);
                        if (edocEntityDto == null) {
                            EdocDataSourceDto eodcDataSourceDto = new EdocDataSourceDto();
                            eodcDataSourceDto.setLevel(2);
                            eodcDataSourceDto.setInformation("未匹配到实体字段，请自行匹配");
                            eodcDataSourceDto.setComponentType("input");
                            map.put(key, eodcDataSourceDto);
                            continue;
                        }
                        if (edocEntityDto.getCaption().contains("意见")) {
                            componentType = getType("UIBUSSINESSEDOCOPINIONBOX");
                        } else if (edocEntityDto.getCaption().equals("主送机关") || edocEntityDto.getCaption().equals("抄送机关")) {
                            componentType = getType("UIBUSINESSSELECTPEOPLE");
                        } else if (edocEntityDto.getCaption().equals("文号")) {
                            componentType = getType("UIBUSINESSEDOCMARK");
                        } else {
                            componentType = getType(edocEntityDto.getDataType());
                        }
                        int level = 0;
                        String information = "";
                        if (desList.size() > 1) {
                            level = 1;
                            for (Map<String, Object> stringObjectMap : desList) {
                                String matchName = String.valueOf(stringObjectMap.get("matchName"));
                                double confidence = Double.parseDouble(String.valueOf(stringObjectMap.get("confidence")));
                                if (Double.compare(maxConfidence, confidence) == -1) {
                                    maxConfidence = confidence;
                                }
                                information = information + "关联信息：" + matchName + ",置信度：" + confidence + ";";
                            }
                        } else {
                            if ((Double.compare(maxConfidence, 0.8) == -1 && Double.compare(maxConfidence, 0.3) == 1)) {
                                // 一般错误
                                level = 1;
                                information = "匹配字段:" + finalMatchName + ",置信度：" + maxConfidence + ";";

                            } else if (Double.compare(maxConfidence, 0.4) == -1 && Double.compare(maxConfidence, 0) == 1) {
                                //  严重错误
                                level = 2;
                                information = "匹配字段:" + finalMatchName + ",置信度：" + maxConfidence + ";";
                            } else {
                                level = 0;
                            }
                        }
                        EdocDataSourceDto eodcDataSourceDto = EdocDataSourceDto.convertToEdocEntity(edocEntityDto, componentType, level, information);
                        map.put(key, eodcDataSourceDto);

                    }

                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private static String getType(String dateType) {
        switch (dateType) {
            case "STRING":
                return UdcStyleTypeEnum.INPUT.getType();
            case "Serial":
                return UdcStyleTypeEnum.INPUT.getType();
            case "INTEGER":
                return UdcStyleTypeEnum.INPUTNUMBER.getType();
            case "BIGINTEGER":
                return UdcStyleTypeEnum.INPUTNUMBER.getType();
            case "DECIMAL":
                return UdcStyleTypeEnum.INPUTNUMBER.getType();
            case "CURRENCY":
                return UdcStyleTypeEnum.CURRENCY.getType();
            case "DATE":
                return UdcStyleTypeEnum.DATEPICKER.getType();
            case "DATETIME":
                return UdcStyleTypeEnum.DATEPICKER.getType();
            case "TIME":
                return UdcStyleTypeEnum.TIMEPICKER.getType();
            case "BOOLEAN":
                return UdcStyleTypeEnum.SWITCH.getType();
            case "CTPENUM":
                return UdcStyleTypeEnum.TREESELECT.getType();
            case "ATTACHMENT":
                return UdcStyleTypeEnum.ATTACHMENT.getType();
            case "ENTITY":
                return UdcStyleTypeEnum.REFERENCE.getType();
            case "MULTILINESTRING":
                return UdcStyleTypeEnum.TEXTAREA.getType();
            case "CONTENT":
                return UdcStyleTypeEnum.UIBUSINESSCONTENT.getType();
            case "GRID":
                return UdcStyleTypeEnum.GRID.getType();
            case "DATAGRID":
                return UdcStyleTypeEnum.DATAGRID.getType();
            case "DATAGRIDVIEW":
                return UdcStyleTypeEnum.DATAGRIDVIEW.getType();
            case "UIBUSINESSEDOCMARK":
                return UdcStyleTypeEnum.UIBUSINESSEDOCMARK.getType();
            case "UIBUSSINESSEDOCOPINIONBOX":
                return UdcStyleTypeEnum.UIBUSSINESSEDOCOPINIONBOX.getType();
            case "UIBUSINESSSELECTPEOPLE":
                return UdcStyleTypeEnum.UIBUSINESSSELECTPEOPLE.getType();
            default:
                return "";
        }
    }


}
