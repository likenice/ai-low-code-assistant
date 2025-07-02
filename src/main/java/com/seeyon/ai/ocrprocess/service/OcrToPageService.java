package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.enums.UdcStyleTypeEnum;
import com.seeyon.ai.ocrprocess.form.AttributeDto;
import com.seeyon.ai.ocrprocess.form.EntityDto;
import com.seeyon.ai.ocrprocess.form.FieldDto;
import com.seeyon.ai.ocrprocess.form.PageCellColRow;
import com.seeyon.ai.ocrprocess.form.PageComponentDto;
import com.seeyon.ai.ocrprocess.form.PageDataSourceDto;
import com.seeyon.ai.ocrprocess.form.PageGroupDto;
import com.seeyon.ai.ocrprocess.form.PageSettingDto;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponse;
import com.seeyon.ai.ocrprocess.form.response.PageDslResponse;
import com.seeyon.ai.ocrprocess.util.FilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class OcrToPageService {


    @Autowired
    private SimilarityProcessService similarityProcessService;
    //    @Value("${seeyon.ocr.pageProps:[\"发起人\",\"创建人\",\"创建时间\"]}")
//    private String pageProps;
//    @Value("${seeyon.ocr.keySize:5}")
//    private Integer keySize;
    @Autowired
    AppProperties appProperties;

    @Autowired
    private OcrProcessService ocrProcessService;


    public PageDslResponse transfer(String ocrJson, List<DataStandardResponse> dataStandardResponses) {
        ObjectMapper objectMapper = new ObjectMapper();
        LinkedHashMap<String, Object> ocrJsonMap;

        try {
            ocrJsonMap = objectMapper.readValue(ocrJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new PlatformException("解析 OCR JSON 失败");
        }

        String dslJson = JSONUtil.toJsonStr(ocrJsonMap.get("structure"));
        String layout = JSONUtil.toJsonStr(ocrJsonMap.get("layout"));

        Map<String, Object> dataMap;
        Map<String, Object> layoutMap;

        try {
            dataMap = objectMapper.readValue(dslJson, new TypeReference<Map<String, Object>>() {
            });
            layoutMap = objectMapper.readValue(layout, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new PlatformException("解析 DSL JSON 或布局 JSON 失败");
        }

        if (dataMap.isEmpty() || layoutMap.isEmpty()) {
            throw new PlatformException("ocr identify data null");
        }

        List<Map> groupPositionList;
        List<Map> rowPositionList;
        try {
            groupPositionList = objectMapper.readValue(objectMapper.writeValueAsString(layoutMap.get("group")), new TypeReference<List<Map>>() {
            });
            rowPositionList = objectMapper.readValue(objectMapper.writeValueAsString(layoutMap.get("sublist")), new TypeReference<List<Map>>() {
            });
        } catch (Exception e) {
            throw new PlatformException("解析布局 JSON 失败");
        }

        Map<String, Object> fieldsInfo = ocrProcessService.getEntityInfo(dataMap).getFieldsInfo();
        List<Map> structureList = new ArrayList<>();
        structureProcess(fieldsInfo, structureList, true);
        return process(
                ocrProcessService.getEntityInfo(dataMap).getTableName(),
                structureList,
                groupPositionList,
                rowPositionList,
                dataStandardResponses
        );
    }

    private PageDslResponse process(String tableName, List<Map> structureList, List<Map> groupPositionList, List<Map> rowPositionList, List<DataStandardResponse> dataStandardResponses) {
        for (DataStandardResponse dataStandardRespons : dataStandardResponses) {
            EntityDto createEntity = dataStandardRespons.getCreateEntity();
            String parentEntityId = createEntity.getParentEntityId();
            if (parentEntityId.equals("0")) {
                // 处理逻辑
            }
        }
        Map<String, Map<String, AttributeDto>> entityGroupMap = entityProcess(dataStandardResponses);
        PageDslResponse pageDslResponse = new PageDslResponse();
        pageDslResponse.setTitleName(tableName);

        int groupNum = 0;
        int subTableNum = 0;
        boolean b = false;

        for (int i = 0; i < structureList.size(); i++) {
            Map<String, Object> map = structureList.get(i);
            String groupName = map.keySet().iterator().next();
            Map<String, AttributeDto> attributeDtoMap = entityGroupMap.get(groupName);
            if(attributeDtoMap==null){
                attributeDtoMap = entityGroupMap.get(FilterUtil.filter(groupName));
            }
            Object o = map.get(groupName);

            log.info("ocrToPage_groupName: {}", FilterUtil.filter(groupName));

            try {
                if (o instanceof LinkedHashMap) {
                    if (i == 0) {
                        b = true;
                    }
                    LinkedHashMap<String, Object> group = (LinkedHashMap<String, Object>) o;
                    gridAssemble(
                            pageDslResponse.getGroups(),
                            UdcStyleTypeEnum.COLLAPSE.getType(),
                            (List<List<Integer>>) groupPositionList.get(groupNum).get("value"),
                            group,
                            groupName,
                            attributeDtoMap,
                            b
                    );
                    groupNum++;
                } else if (o instanceof List) {
                    List jsonArray = (List) o;
                    if (!jsonArray.isEmpty()) {
                        Object firstElement = jsonArray.get(0);
                        if (firstElement instanceof LinkedHashMap) {
                            LinkedHashMap<String, Object> subTable = (LinkedHashMap<String, Object>) firstElement;
                            subListAssemble(
                                    pageDslResponse.getGroups(),
                                    UdcStyleTypeEnum.DATAGRID.getType(),
                                    (List<List<Integer>>) rowPositionList.get(subTableNum).get("value"),
                                    subTable,
                                    groupName,
                                    attributeDtoMap
                            );
                            subTableNum++;
                        }
                    }
                } else {
                    log.info("错误类型，进行人工排查");
                }
                b = false;
            } catch (Exception e) {
                log.error("处理结构列表时出错: {}", e.getMessage(), e);
            }
        }

        return pageDslResponse;
    }

    private Integer getMaxX(List<List<Integer>> layout) {
        int maxX = 0;
        for (List<Integer> list : layout) {
            maxX = Math.max(maxX, list.get(2));
        }
        return maxX;
    }

    private void groupAssemble(List<PageGroupDto> pageGroups, String type, List<List<Integer>> layout, LinkedHashMap groupMap, String groupName, Map<String, AttributeDto> attributeDtoMap, boolean b) {
        Integer maxX = getMaxX(layout);
        PageGroupDto pageGroupDto = new PageGroupDto();
        pageGroups.add(pageGroupDto);
        pageGroupDto.setOcrRelationId(attributeDtoMap.get(attributeDtoMap.keySet().iterator().next()).getGroupOcrRelationId());
        pageGroupDto.setName(groupName);
        PageSettingDto pageSettingDto = new PageSettingDto();
        pageSettingDto.setTitleName(groupName);
        pageGroupDto.setSettings(pageSettingDto);
        List<PageComponentDto> components = new ArrayList<>();
        pageGroupDto.setComponents(components);
        Set set = groupMap.keySet();
        Iterator iterator = set.iterator();
        boolean boo = false;
        if (b && set.size() < appProperties.getKeySize() && groupName.contains("未命名分组")) {
            boo = true;
        }
        int index = 0;
        int width = 0;
        int maxFlex = 0;
        int flex = 1;
        int prevY = 0;
        // 字段最小宽度
        int minWidth = 0;
        int rowWidth = 0;
        int rowMaxWidth = 0;
        while (iterator.hasNext()) {
            PageComponentDto pageComponentDto = new PageComponentDto();
            components.add(pageComponentDto);
            String fieldName = String.valueOf(iterator.next());
            if (fieldName.contains("*")) {
                fieldName = fieldName.replaceAll("\\*", "");
            }
            if (fieldName.contains("※")) {
                fieldName = fieldName.replaceAll("※", "");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> pagePropsList = null;
            try {
                pagePropsList = objectMapper.readValue(appProperties.getPageProps(), new TypeReference<List<String>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (pagePropsList.contains(fieldName) && boo && groupMap.size() > 1) {
                type = "pageProps";
            }
            AttributeDto attributeDto = attributeDtoMap.get(fieldName);
            String dataType = attributeDto.getDataType();
            pageComponentDto.setType(getType(dataType));
            if (dataType.equals("CTPENUM") && attributeDto.getEnumArr()) {
                pageComponentDto.setType("radio");
            }
            PageSettingDto settings = pageComponentDto.getSettings();
            settings.setTitleName(fieldName);
            Map<String, Object> m = new HashMap<>();
            m.put("required", attributeDto.isNotNull());
            settings.setValidation(m);
            pageComponentDto.setOcrRelationId(attributeDto.getOcrRelationId());
            pageComponentDto.setDataSource(PageDataSourceDto.convert(fieldName));
            List<Integer> nameLayout = layout.get(index);
            int x1 = nameLayout.get(0);
            int y1 = nameLayout.get(1);
            int y2 = nameLayout.get(3);
            int x2 = 0;
            Integer nextKeyIndex = index + 2;
            // 最后一个元素
            if (index + 2 == layout.size()) {
                nextKeyIndex = index + 1;
                List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
//                x2 = nextKeyLayout.get(2);
                x2 = maxX;
            } else {
                List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
                int nextKeyY = nextKeyLayout.get(1);
                if (nextKeyY - y1 > 15) {
                    List<Integer> valueLayout = layout.get(index + 1);
//                    x2 = valueLayout.get(2);
                    x2 = maxX;
                } else {
                    x2 = nextKeyLayout.get(0);
                }
            }
            width = Math.abs(x2 - x1);
            pageComponentDto.setY(y1);
            pageComponentDto.setWidth(width);
            pageComponentDto.setHigh(y2 - y1);
            minWidth = Math.min(minWidth, width) == 0 ? width : Math.min(minWidth, width);
            index = index + 2;
        }
        for (PageComponentDto component : components) {
            Integer y = component.getY();
            Integer width1 = component.getWidth();
            if (prevY == 0) {
                rowWidth = width1;
            } else {
                if (y - prevY > 15) {
                    maxFlex = Math.max(flex, maxFlex);
                    rowMaxWidth = Math.max(rowWidth, rowMaxWidth);
                    flex = 1;
                    rowWidth = width1;
                } else {
                    flex++;
                    rowWidth = rowWidth + width1;
                }
            }
            prevY = y;

        }
        if (maxFlex == 0) {
            maxFlex = flex;
        }
        if (rowMaxWidth == 0) {
            rowMaxWidth = rowWidth;
        }
        pageGroupDto.setType(type);
        groupField(rowMaxWidth / maxFlex, components, pageGroupDto);
    }

    private void gridAssemble(List<PageGroupDto> pageGroups, String type, List<List<Integer>> layout, LinkedHashMap groupMap, String groupName, Map<String, AttributeDto> attributeDtoMap, boolean b) {


        Map<String, String> colMap = gridColAssemble(layout, groupMap);
        Integer maxX = getMaxX(layout);
        PageGroupDto pageGroupDto = new PageGroupDto();
        pageGroups.add(pageGroupDto);
        pageGroupDto.setOcrRelationId(attributeDtoMap.get(attributeDtoMap.keySet().iterator().next()).getGroupOcrRelationId());
        pageGroupDto.setName(groupName);
        PageSettingDto pageSettingDto = new PageSettingDto();
        pageSettingDto.setTitleName(groupName);
        pageGroupDto.setSettings(pageSettingDto);
        List<PageComponentDto> components = new ArrayList<>();
        pageGroupDto.setComponents(components);
        Set set = groupMap.keySet();
        Iterator iterator = set.iterator();
        boolean boo = false;
        if (b && set.size() < appProperties.getKeySize() && groupName.contains("未命名分组")) {
            boo = true;
        }
        int index = 0;
        int prevY = 0;
        // 字段最小宽度
        int minWidth = 0;
        int rowWidth = 0;
        int rowMaxWidth = 0;
        while (iterator.hasNext()) {
            String fieldName = String.valueOf(iterator.next());
            if (fieldName.contains("*")) {
                fieldName = fieldName.replaceAll("\\*", "");
            }
            if (fieldName.contains("※")) {
                fieldName = fieldName.replaceAll("※", "");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> pagePropsList = null;
            try {
                pagePropsList = objectMapper.readValue(appProperties.getPageProps(), new TypeReference<List<String>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (pagePropsList.contains(fieldName) && boo && groupMap.size() > 1) {
                type = "pageProps";
            }
            Integer width = layoutProcess(attributeDtoMap, fieldName, components, index, layout, maxX, "1");
            if (width != 0) {
                minWidth = Math.min(minWidth, width) == 0 ? width : Math.min(minWidth, width);
            }
            index = index + 2;
        }
        int maxFlex = 0;
        int flex = 1;
        for (PageComponentDto component : components) {
            Integer y = component.getY();
            Integer width1 = component.getWidth();
            if (prevY == 0) {
                rowWidth = width1;
            } else {
                if (y - prevY > 15) {
                    maxFlex = Math.max(flex, maxFlex);
                    rowMaxWidth = Math.max(rowWidth, rowMaxWidth);
                    flex = 1;
                    rowWidth = width1;
                } else {
                    flex++;
                    rowWidth = rowWidth + width1;
                }
            }
            prevY = y;

        }
        if (maxFlex == 0) {
            maxFlex = flex;
        }
        if (rowMaxWidth == 0) {
            rowMaxWidth = rowWidth;
        }
        pageGroupDto.setType(type);
        gridField(rowMaxWidth / maxFlex, components, pageGroupDto, colMap);
    }

    private Integer layoutProcess(Map<String, AttributeDto> attributeDtoMap, String fieldName, List<PageComponentDto> components
            , int index, List<List<Integer>> layout, int maxX, String type) {
        if (type.equals("1")) {
            PageComponentDto pageComponentDto = new PageComponentDto();
            AttributeDto attributeDto = attributeDtoMap.get(fieldName);
            if (attributeDto == null) {
                log.info("fieldName:,attributeDtoMap:{},{}", fieldName, JSONUtil.toJsonStr(attributeDtoMap));
                return 0;
            }
            components.add(pageComponentDto);
            String dataType = attributeDto.getDataType();
            pageComponentDto.setType(getType(dataType));
            if (dataType.equals("CTPENUM") && attributeDto.getEnumArr()) {
                pageComponentDto.setType("radio");
            }
            PageSettingDto settings = pageComponentDto.getSettings();
            settings.setTitleName(fieldName);
            Map<String, Object> m = new HashMap<>();
            m.put("required", attributeDto.isNotNull());
            settings.setValidation(m);
            pageComponentDto.setName(fieldName);
            pageComponentDto.setOcrRelationId(attributeDto.getOcrRelationId());
            pageComponentDto.setDataSource(PageDataSourceDto.convert(fieldName));
            List<Integer> nameLayout = layout.get(index);
            int x1 = nameLayout.get(0);
            int y1 = nameLayout.get(1);
            int x2 = 0;
            Integer nextKeyIndex = index + 2;
            // 最后一个元素
            if (index + 2 == layout.size()) {
                x2 = maxX;
            } else {
                List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
                int nextKeyY = nextKeyLayout.get(1);
                if (nextKeyY - y1 > 15) {
                    x2 = maxX;
                } else {
                    x2 = nextKeyLayout.get(0);
                }
            }
            int width = Math.abs(x2 - x1);
            pageComponentDto.setY(y1);
            pageComponentDto.setX1(nameLayout.get(0));
            pageComponentDto.setX2(nameLayout.get(2));
            pageComponentDto.setWidth(width);
            return width;
        } else {
            int minWidth = 0;
            for (int i = 0; i < 2; i++) {
                PageComponentDto pageComponentDto = new PageComponentDto();
                components.add(pageComponentDto);
                if (i == 0) {
                    pageComponentDto.setType("");
                    PageSettingDto settings = pageComponentDto.getSettings();
                    settings.setTitleName(fieldName);
                    pageComponentDto.setName(fieldName + "value");
                    pageComponentDto.setType("label");
                    pageComponentDto.setOcrRelationId("");
                    pageComponentDto.setDataSource(PageDataSourceDto.convert(""));
                } else {
                    AttributeDto attributeDto = attributeDtoMap.get(fieldName);
                    String dataType = attributeDto.getDataType();
                    pageComponentDto.setType(getType(dataType));
                    if (dataType.equals("CTPENUM") && attributeDto.getEnumArr()) {
                        pageComponentDto.setType("radio");
                    }
                    PageSettingDto settings = pageComponentDto.getSettings();
                    settings.setTitleName("");
                    pageComponentDto.setName(fieldName);
                    pageComponentDto.setOcrRelationId(attributeDto.getOcrRelationId());
                    pageComponentDto.setDataSource(PageDataSourceDto.convert(fieldName));
                }
                List<Integer> nameLayout = layout.get(index);
                int x1 = nameLayout.get(0);
                int y1 = nameLayout.get(1);
                int x2 = 0;
                Integer nextKeyIndex = index + 1;
                // 最后一个元素
                if (index + 1 == layout.size()) {
                    x2 = maxX;
                } else {
                    List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
                    int nextKeyY = nextKeyLayout.get(1);
                    if (nextKeyY - y1 > 15) {
                        x2 = maxX;
                    } else {
                        x2 = nextKeyLayout.get(0);
                    }
                }
                int width = Math.abs(x2 - x1);
                pageComponentDto.setY(y1);
                pageComponentDto.setX1(nameLayout.get(0));
                pageComponentDto.setX2(nameLayout.get(2));
                pageComponentDto.setWidth(width);
                minWidth = Math.min(minWidth, width) == 0 ? width : Math.min(minWidth, width);
                index = index + 1;
            }
            return minWidth;
        }
    }

    private Map<String, String> gridColAssemble(List<List<Integer>> layout, LinkedHashMap groupMap) {
        Set set = groupMap.keySet();
        Iterator iterator = set.iterator();
        int index = 0;
        int row = 1;
        int prevY = -1;
        Map<String, String> map = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            int prevY1 = -1;
            int flexRowSize = 1;
            String fieldName = String.valueOf(iterator.next());
            if (fieldName.contains("*")) {
                fieldName = fieldName.replaceAll("\\*", "");
            }
            if (fieldName.contains("※")) {
                fieldName = fieldName.replaceAll("※", "");
            }
            List<Integer> nameLayout = layout.get(index);
            int y1 = nameLayout.get(1);
            int y2 = nameLayout.get(3);
            if (prevY == -1) {
                prevY = y1;
            } else {
                if (y1 - prevY > 15) {
                    row++;
                    prevY = y1;
                }
            }
            for (int i = index; i < layout.size(); i = i + 2) {
                List<Integer> nextLayout = layout.get(i);
                int nextY1 = nextLayout.get(1);
                int nextY2 = nextLayout.get(3);
                // 一行数组值累加一次
                if (prevY1 == -1) {
                    if (nextY1 - y1 > 15 && Math.abs(nextY2 - y2) > 15) {
                        flexRowSize++;
                    }
                    prevY1 = nextY1;
                } else {
//                    if (Math.abs(nextY1 - prevY1) > 15 && nextY1 - y1 > 15 && Math.abs(nextY2 - y2) < 15) {
                    if (Math.abs(nextY1 - prevY1) > 15 && nextY1 - y1 > 15 && y2 - nextY1 > 15) {
                        flexRowSize++;
                    }
                    if (Math.abs(nextY1 - prevY1) > 15) {
                        prevY1 = nextY1;
                    }

                }
            }
            index = index + 2;
            map.put(fieldName, row + "," + flexRowSize);
            map.put(fieldName + "value", row + "," + flexRowSize);
        }
        map.put("maxRow", String.valueOf(row));
        return map;
    }

    private void subListAssemble(List<PageGroupDto> pageGroups, String type, List<List<Integer>> layout, LinkedHashMap groupMap, String groupName, Map<String, AttributeDto> attributeDtoMap) {
        PageGroupDto pageGroupDto = new PageGroupDto();
        pageGroups.add(pageGroupDto);
        pageGroupDto.setOcrRelationId(attributeDtoMap.get(attributeDtoMap.keySet().iterator().next()).getGroupOcrRelationId());
        pageGroupDto.setName(groupName);
        pageGroupDto.setType(type);
        PageSettingDto pageSettingDto = new PageSettingDto();
        pageSettingDto.setTitleName(groupName);
        pageGroupDto.setSettings(pageSettingDto);
        List<PageComponentDto> components = new ArrayList<>();
        pageGroupDto.setComponents(components);
        Iterator iterator = groupMap.keySet().iterator();
        int index = 0;
        int width = 0;
        // 字段最小宽度
        int allWidth = 0;
        while (iterator.hasNext()) {
            PageComponentDto pageComponentDto = new PageComponentDto();
            components.add(pageComponentDto);
            String fieldName = String.valueOf(iterator.next());
            if (fieldName.contains("*")) {
                fieldName = fieldName.replaceAll("\\*", "");
            }
            if (fieldName.contains("※")) {
                fieldName = fieldName.replaceAll("※", "");
            }
            AttributeDto attributeDto = attributeDtoMap.get(fieldName);
            String dataType = attributeDto.getDataType();
            pageComponentDto.setType(getType(dataType));
            PageSettingDto settings = pageComponentDto.getSettings();
            settings.setTitleName(fieldName);
            pageComponentDto.setOcrRelationId(attributeDto.getOcrRelationId());
            pageComponentDto.setDataSource(PageDataSourceDto.convert(fieldName));
            List<Integer> nameLayout = layout.get(index);
            int x1 = nameLayout.get(0);
            int y1 = nameLayout.get(1);
            int y2 = nameLayout.get(3);
            int x2 = 0;
            Integer nextKeyIndex = index + 2;
            if (index + 2 == layout.size()) {
                nextKeyIndex = index + 1;
                List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
                x2 = nextKeyLayout.get(2);

            } else {
                List<Integer> nextKeyLayout = layout.get(nextKeyIndex);
                int nextKeyY = nextKeyLayout.get(1);
                if (nextKeyY - y1 > 15) {
                    List<Integer> valueLayout = layout.get(index + 1);
                    x2 = valueLayout.get(2);
                } else {
                    x2 = nextKeyLayout.get(0);
                }
            }

            width = Math.abs(x2 - x1);
            pageComponentDto.setY(y1);
            pageComponentDto.setWidth(width);
            pageComponentDto.setHigh(y2 - y1);
//            minWidth = Math.min(minWidth, width) == 0 ? width : Math.min(minWidth, width);
            allWidth = allWidth + width;
            index = index + 2;
        }
        subListField(allWidth, components, pageGroupDto);
    }

    private void groupField(int minWidth, List<PageComponentDto> list, PageGroupDto pageGroupDto) {
        int colIndex = 0;
        int rowIndex = 1;
        int prevY = 0;
        int maxCol = 0;
        for (int i = 0; i < list.size(); i++) {
            PageComponentDto pageComponentDto = list.get(i);
            Integer fieldWidth = pageComponentDto.getWidth();
            int round = (int) Math.round(fieldWidth / (double) minWidth);
            if (round == 0) {
                round = 1;
            }
            Integer y = pageComponentDto.getY();
            if (colIndex == 0) {
                colIndex = 1;
            } else {
                if (Math.abs(prevY - y) > 15) {
                    colIndex = 1;
                    rowIndex++;
                }
            }
            boolean b = false;
            // 独占一行
            if (i + 1 == list.size() && Math.abs(prevY - y) > 15) {
                b = true;
            }
            if (i + 1 < list.size()) {
                int nextY = list.get(i + 1).getY();
                if (Math.abs(prevY - y) > 15 && Math.abs(nextY - y) > 15) {
                    b = true;
                }
                if (i == 0 && Math.abs(nextY - y) > 15) {
                    b = true;
                }
            }
            prevY = y;
            PageCellColRow pageCellColRow = new PageCellColRow();
            pageCellColRow.setRowIndex(rowIndex);
            pageCellColRow.setFlexRowSize(1);
            pageCellColRow.setColIndex(colIndex);
            if (b) {
                pageCellColRow.setFlexColSize(-1);
            } else {
                pageCellColRow.setFlexColSize(round);
            }
            colIndex = colIndex + round;
            maxCol = Math.max(maxCol, (colIndex - 1));
            pageComponentDto.setCellColRow(pageCellColRow);
        }
        PageSettingDto settingDto = pageGroupDto.getSettings();
        List<Double> gridTemplateColumns = new ArrayList<>();
        List<Double> gridTemplateRows = new ArrayList<>();
        for (int i = 0; i < maxCol; i++) {
            gridTemplateColumns.add(1.0);
        }
        for (int i = 0; i < rowIndex; i++) {
            gridTemplateRows.add(1.0);
        }
        settingDto.setGridTemplateColumns(gridTemplateColumns);
        settingDto.setGridTemplateRows(gridTemplateRows);
        pageGroupDto.setSettings(settingDto);
        pageGroupDto.setCol(maxCol);
//        int y = 0;
        int allFlexCloSize = 0;
        for (int i = 0; i < list.size(); i++) {
            PageComponentDto pageComponentDto = list.get(i);
            PageCellColRow cellColRow = pageComponentDto.getCellColRow();
            Integer flexColSize = cellColRow.getFlexColSize();
            int y = pageComponentDto.getY();
            if (flexColSize == -1) {
                cellColRow.setFlexColSize(maxCol);
                continue;
            }
            allFlexCloSize = allFlexCloSize + flexColSize;
            if (y == 0) {

            } else {
                if (i + 1 < list.size()) {
                    int nextY = list.get(i + 1).getY();
                    if (Math.abs(nextY - y) > 15) {
                        if (maxCol - allFlexCloSize > 0) {
                            cellColRow.setFlexColSize(maxCol - allFlexCloSize + flexColSize);
                        }
//                        y = nextY;
                        allFlexCloSize = 0;
                    }
                } else {
                    if (maxCol - allFlexCloSize > 0) {
                        cellColRow.setFlexColSize(maxCol - allFlexCloSize + flexColSize);
                    }
                    allFlexCloSize = 0;
                }
            }

        }
    }

    private void gridField(int minWidth, List<PageComponentDto> list, PageGroupDto pageGroupDto, Map<String, String> colMap) {
        int colIndex = 0;
        int prevY = 0;
        int maxCol = 0;
        // x1 x2
        List<Map<String, String>> layoutList = new ArrayList<>();
        List<Map<String, String>> useLayoutList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            PageComponentDto pageComponentDto = list.get(i);
            String fieldName = pageComponentDto.getName();
            Integer x1 = pageComponentDto.getX1();
            Integer x2 = pageComponentDto.getX2();
            int rowIndex = Integer.parseInt(colMap.get(fieldName).split(",")[0]);
            int flexRowSize = Integer.parseInt(colMap.get(fieldName).split(",")[1]);
            int fieldWidth = pageComponentDto.getWidth();
            int newFieldWidth = 0;
            int newColIndex = 0;
            boolean newWidth = false;
            Integer y = pageComponentDto.getY();
            if (Math.abs(prevY - y) > 15) {
                useLayoutList.clear();
            }
            if (rowIndex != 1 && layoutList.size() > 0) {
                for (int m = layoutList.size() - 1; m >= 0; m--) {
                    Map<String, String> map = layoutList.get(m);
                    String rowFlex = map.get("rowFlex");
                    String layout = map.get("layout");
                    String colFlex = map.get("colFlex");
                    int row = Integer.parseInt(rowFlex.split(",")[0]);
                    int flexRow = Integer.parseInt(rowFlex.split(",")[1]);
                    int mergeX1 = Integer.parseInt(layout.split(",")[0]);
                    int mergeX2 = Integer.parseInt(layout.split(",")[1]);
                    int col = Integer.parseInt(colFlex.split(",")[0]);
                    int flexCol = Integer.parseInt(colFlex.split(",")[1]);
                    int nowRow = row + (flexRow - 1);
                    if (nowRow < rowIndex) {
                        continue;
                    }
                    if (Math.abs(prevY - y) < 15) {
                        if (useLayoutList.contains(map)) {
                            continue;
                        }
                    }
                    // 合并单元格在当前单元格之前 需要修改colIndex
                    if (x1 >= mergeX2) {
                        if (newColIndex == 0) {
                            newColIndex = col + flexCol;
                        } else {
                            newColIndex = Math.max(newColIndex, (col + flexCol));
                        }
                        useLayoutList.add(map);
                    }
                    // 合并单元格在单元格之后 需要修改width
                    else if (mergeX1 >= x2) {
                        if (i + 1 < list.size()) {
                            PageComponentDto nextPageComponentDto = list.get(i + 1);
                            Integer nextX1 = nextPageComponentDto.getX1();
                            Integer nextY = nextPageComponentDto.getY();
                            if (nextX1 < mergeX1 && Math.abs(nextY - y) <= 15) {
                                continue;
                            }
                        }

                        if (newFieldWidth == 0) {
                            newWidth = true;
                            newFieldWidth = mergeX1 - x1;
                        } else {
                            newWidth = true;
                            newFieldWidth = Math.min(newFieldWidth, (mergeX1 - x1));
                        }
//                        useLayoutList.add(map);
                    }
                }
            }
            if (newFieldWidth != 0) {
                fieldWidth = newFieldWidth;
                pageComponentDto.setGridProcess(true);
            }
            int round = (int) Math.round(fieldWidth / (double) minWidth);
            if (round == 0) {
                round = 1;
            }

            if (colIndex == 0) {
                colIndex = 1;
            } else {
                if (Math.abs(prevY - y) > 15) {
                    colIndex = 1;

                }
            }
            boolean b = false;
            // 独占一行
            if (i + 1 == list.size() && Math.abs(prevY - y) > 15) {
                b = true;
            }
            if (i + 1 < list.size()) {
                int nextY = list.get(i + 1).getY();
                if (Math.abs(prevY - y) > 15 && Math.abs(nextY - y) > 15 && !newWidth) {
                    b = true;
                }
                if (i == 0 && Math.abs(nextY - y) > 15) {
                    b = true;
                }
            }
            if (newColIndex != 0) {
                colIndex = newColIndex;
            }
            prevY = y;
            PageCellColRow pageCellColRow = new PageCellColRow();
            pageCellColRow.setRowIndex(rowIndex);
            pageCellColRow.setFlexRowSize(flexRowSize);
            pageCellColRow.setColIndex(colIndex);
            if (b) {
                pageCellColRow.setFlexColSize(-1);
            } else {
                pageCellColRow.setFlexColSize(round);
            }
            if (flexRowSize > 1) {
                Map<String, String> map = new HashMap<>();
                map.put("rowFlex", rowIndex + "," + flexRowSize);
                map.put("layout", x1 + "," + x2);
                map.put("colFlex", colIndex + "," + round);
                layoutList.add(map);
            }
            colIndex = colIndex + round;
            maxCol = Math.max(maxCol, (colIndex - 1));
            pageComponentDto.setCellColRow(pageCellColRow);

        }
        PageSettingDto settingDto = pageGroupDto.getSettings();
        List<Double> gridTemplateColumns = new ArrayList<>();
        List<Double> gridTemplateRows = new ArrayList<>();
        for (int i = 0; i < maxCol; i++) {
            gridTemplateColumns.add(1.0);
        }
        int rowIndex = Integer.parseInt(colMap.get("maxRow"));
        for (int i = 0; i < rowIndex; i++) {
            gridTemplateRows.add(1.0);
        }
        settingDto.setGridTemplateColumns(gridTemplateColumns);
        settingDto.setGridTemplateRows(gridTemplateRows);
        pageGroupDto.setSettings(settingDto);
        pageGroupDto.setCol(maxCol);
        int allFlexCloSize = 0;
        for (int i = 0; i < list.size(); i++) {
            PageComponentDto pageComponentDto = list.get(i);
            String name = pageComponentDto.getName();
            PageCellColRow cellColRow = pageComponentDto.getCellColRow();
            Integer flexColSize = cellColRow.getFlexColSize();
            Integer colIndex1 = cellColRow.getColIndex();

            int y = pageComponentDto.getY();
            if (flexColSize == -1) {
                cellColRow.setFlexColSize(maxCol);
                continue;
            }
            if (pageComponentDto.getGridProcess()) {
                continue;
            }
//            allFlexCloSize = allFlexCloSize + colIndex1 + flexColSize - 1;
            if (allFlexCloSize == 0) {
                if (colIndex1 != 1) {
                    flexColSize = colIndex1 - 1 + flexColSize;
                }
            }

            if (y == 0) {

            } else {
                if (i + 1 < list.size()) {
                    int nextY = list.get(i + 1).getY();
                    if (Math.abs(nextY - y) > 15) {
                        if (maxCol - allFlexCloSize > 0) {
//                            cellColRow.setFlexColSize(maxCol - allFlexCloSize + colIndex1 + flexColSize - 1);
                            cellColRow.setFlexColSize(maxCol - allFlexCloSize);
                        }
                        allFlexCloSize = 0;
                    } else {
                        allFlexCloSize = allFlexCloSize + flexColSize;
                    }
                } else {
                    if (maxCol - allFlexCloSize > 0) {
                        cellColRow.setFlexColSize(maxCol - allFlexCloSize);
//                        cellColRow.setFlexColSize(maxCol - allFlexCloSize + flexColSize);
                    }
                    allFlexCloSize = 0;
                }
            }

        }
    }

    private void subListField(int allMidth, List<PageComponentDto> list, PageGroupDto pageGroupDto) {
        int colIndex = 1;
        int rowIndex = 1;
        List<Double> gridTemplateColumns = new ArrayList<>();
        double oneWidth = (double) allMidth / list.size();
        for (int i = 0; i < list.size(); i++) {
            PageComponentDto pageComponentDto = list.get(i);
            Integer fieldWidth = pageComponentDto.getWidth();
            double round = fieldWidth / oneWidth;
            BigDecimal bd = new BigDecimal(round).setScale(2, RoundingMode.HALF_UP); // 保留两位小数
            round = bd.doubleValue();
            if (round == 0) {
                round = 1;
            }
            gridTemplateColumns.add(round);
//            round = 1;
            PageCellColRow pageCellColRow = new PageCellColRow();
            pageCellColRow.setRowIndex(rowIndex);
            pageCellColRow.setFlexRowSize(1);
            pageCellColRow.setColIndex(colIndex);
            pageCellColRow.setFlexColSize(1);
            colIndex++;
            pageComponentDto.setCellColRow(pageCellColRow);
        }
        PageSettingDto settingDto = pageGroupDto.getSettings();
        List<Double> gridTemplateRows = new ArrayList<>();
        gridTemplateRows.add(1.0);
        settingDto.setGridTemplateColumns(gridTemplateColumns);
        settingDto.setGridTemplateRows(gridTemplateRows);
        pageGroupDto.setSettings(settingDto);
    }

    /**
     * 拉平ocr结构数据保证顺序
     *
     * @param dataMap
     * @param structureList
     * @param c
     */

    private void structureProcess(Map dataMap, List<Map> structureList, boolean c) {
        ObjectMapper objectMapper = new ObjectMapper();
        boolean b = true;
        Map defMap = new LinkedHashMap();
        Map def = new LinkedHashMap();
        defMap.put("未命名组" + structureList.size(), def);
        Iterator iteratored = dataMap.keySet().iterator();

        while (iteratored.hasNext()) {
            Map map = new LinkedHashMap();
            String key = String.valueOf(iteratored.next());
            if (key.equals("logo组")) {
                continue;
            }
            Object field = dataMap.get(key);
            try {
                if (field instanceof LinkedHashMap) {
                    map.put(key, field);
                    structureList.add(map);
                    structureProcess((LinkedHashMap) field, structureList, false);
                } else if (field instanceof List) {
                    List jsonArray = (List) field;
                    if (!jsonArray.isEmpty()) {
                        Object subField = jsonArray.get(0);
                        if (subField instanceof LinkedHashMap) {
                            map.put(key, field);
                            structureList.add(map);
                        } else {
                            if (c) {
                                def.put(key, field);
                                if (b) {
                                    structureList.add(defMap);
                                    b = false;
                                }
                            }
                        }
                    }
                } else {
                    if (c) {
                        def.put(key, field);
                        if (b) {
                            structureList.add(defMap);
                            b = false;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("类型处理异常：{},字段：{},字段值：{}", e.getMessage(), key, field);
            }
        }
    }

    private String getType(String dateType) {
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
            default:
                return "";
        }
    }


    /**
     * 将实体字段信息转为map，所有字段名提取出
     *
     * @param dataStandardResponseList
     * @return
     */
    private Map<String, Map<String, AttributeDto>> entityProcess(List<DataStandardResponse> dataStandardResponseList) {
        Map<String, Map<String, AttributeDto>> map = new HashMap<>();
        Map<String, AttributeDto> fieldMap = new HashMap<>();
        List<String> fieldNames = new ArrayList<>();
        for (DataStandardResponse dataStandardResponse : dataStandardResponseList) {
            EntityDto createEntity = dataStandardResponse.getCreateEntity();
            String parentEntityId = createEntity.getParentEntityId();
            if (parentEntityId.equals("0")) {
                List<FieldDto> operateEntity = dataStandardResponse.getOperateEntity();
                for (FieldDto fieldDto : operateEntity) {
                    String caption = fieldDto.getCaption();
                    String ocrRelationId = fieldDto.getOcrRelationId();
                    List<AttributeDto> attributeDtoList = fieldDto.getAttributeDtoList();
                    Map<String, AttributeDto> attributeDtoMap = new HashMap<>();
                    map.put(caption, attributeDtoMap);
                    for (AttributeDto attributeDto : attributeDtoList) {
                        attributeDto.setGroupOcrRelationId(ocrRelationId);
                        attributeDtoMap.put(attributeDto.getCaption(), attributeDto);
                    }
                }
            } else {
                List<FieldDto> operateEntity = dataStandardResponse.getOperateEntity();
                for (FieldDto fieldDto : operateEntity) {
                    String ocrRelationId = createEntity.getOcrRelationId();
                    List<AttributeDto> attributeDtoList = fieldDto.getAttributeDtoList();
                    Map<String, AttributeDto> attributeDtoMap = new HashMap<>();
                    map.put(createEntity.getCaption(), attributeDtoMap);
                    for (AttributeDto attributeDto : attributeDtoList) {
                        attributeDto.setGroupOcrRelationId(ocrRelationId);
                        attributeDtoMap.put(attributeDto.getCaption(), attributeDto);
                    }
                }
            }
        }

        return map;
    }
}
