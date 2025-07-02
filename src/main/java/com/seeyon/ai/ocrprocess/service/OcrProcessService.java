package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.dao.UserUseFormInfoDao;
import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.enums.AiformFlowEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTaskStatusEnum;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.DataJsonDto;
import com.seeyon.ai.ocrprocess.form.EdocEntityDto;
import com.seeyon.ai.ocrprocess.form.EdocTableStructureDto;
import com.seeyon.ai.ocrprocess.form.FileDto;
import com.seeyon.ai.ocrprocess.form.LayoutDto;
import com.seeyon.ai.ocrprocess.form.UploadRequestDto;
import com.seeyon.ai.ocrprocess.form.request.OcrProcessRequest;
import com.seeyon.ai.ocrprocess.util.StringSimilarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OcrProcessService {
    @Autowired
    private EditingAreaDetectorService editingAreaDetectorService;
    @Autowired
    private RepeatedSetDetectorService repeatedSetDetectorService;
    @Autowired
    private AdjacentBlockService adjacentBlockService;
    @Autowired
    private AiPromptSvcAppService aiPromptSvcAppService;
    @Autowired
    private RowDataBuilderService rowDataBuilderService;
    @Autowired
    private SpecialItemService specialItemService;
    @Autowired
    private EdocPageDslTransferService edocPageDslTransferService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private UserUseFormInfoDao userUseFormInfoDao;
    @Autowired
    private FileService fileService;

    public String process(OcrProcessRequest testRequest, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap,String apiKey) {
        String blocks = testRequest.getOcr();
        log.info("blocks：{}", blocks);
        String cellsProcessor = testRequest.getTsr();

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> list = null;
        try {
            list = objectMapper.readValue(blocks, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<CellDto> cellDtos = null;
        try {
            cellDtos = objectMapper.readValue(cellsProcessor, new TypeReference<List<CellDto>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        adjacentBlockService.process(list, cellDtos, "ocr", llmInitialAnalysisMap, llmConfidenceMap);
        log.info("cellDtos：{}", JSONUtil.toJsonStr(cellDtos));
        log.info("文本拆分合并后数据：{}", JSONUtil.toJsonStr(list));
        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("ocr", blocks);
        ocrResult.put("tsr", cellDtos);
        userUseFormInfo.setOcrResult(writeTxt(JSONUtil.toJsonStr(ocrResult),apiKey));
        // 特殊字符识别
        AiFormFLowInfo aiFormFLowInfo = testRequest.getAiFormFLowInfo();
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.specialItem.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.specialItem.code());
        cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));

        // 特殊字符校验
        Map<String, Object> items = specialItemService.findItem(list);
        log.info("识别到的特殊字符：{}", JSONUtil.toJsonStr(items));

        List rows = null;
        try {
            rows = rowDataBuilderService.process(list, cellDtos);
        } catch (Exception e) {
            handleError(e,userUseFormInfo,aiFormFLowInfo,llmInitialAnalysisMap, llmConfidenceMap,apiKey);
            throw new RuntimeException(e);
        }
        log.info("行数据组装后数据：{}", JSONUtil.toJsonStr(rows));

        String editingAreaDefinitions = "[\n" +
                "        {\"keywords\": [\"BIUAABC\", \"BIU AABC\"], \"type\": \"keyword\", \"order\": 1, \"weight\": 0.2, \"match_method\": \"levenshtein\"},\n" +
                "        {\"keywords\": [\"字体\", \"font\", \"微软雅黑\"], \"type\": \"keyword\", \"order\": 2, \"weight\": 0.4, \"match_method\": \"cosine\"},\n" +
                "        {\"keywords\": [\"字号\", \"size\", \"12px\"], \"type\": \"keyword\", \"order\": 3, \"weight\": 0.3, \"match_method\": \"levenshtein\"},\n" +
                "        {\"keywords\": [\"段落\", \"paragraph\"], \"type\": \"keyword\", \"order\": 4, \"weight\": 0.1, \"match_method\": \"direct\"}\n" +
                "    ]";

        // 富文本识别
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.editingAreaDetector.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.editingAreaDetector.code());
        cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));

        editingAreaDetectorService.process(rows, new LinkedHashMap<>(), editingAreaDefinitions);
        log.info("富文本识别后行数据：{},富文本识别信息：{}", JSONUtil.toJsonStr(rows), JSONUtil.toJsonStr(new LinkedHashMap<>()));

        repeatedSetDetectorService.process(rows, new LinkedHashMap<>(),llmInitialAnalysisMap, llmConfidenceMap);
        log.info("重复节识别后行数据：{},重复节识别信息：{}", JSONUtil.toJsonStr(rows), JSONUtil.toJsonStr(new LinkedHashMap<>()));
        Map<String, String> processStructures = processStructures(new LinkedHashMap<>());
        String supplements = processSupplements(processStructures);
        log.info("特殊结构提示词组装：{}", supplements);
        Map<String, Object> promptParams = new HashMap<>();
        promptParams.put("supplements", supplements);
        AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
        aiPromptSvcCallDto.setPromptCode("tableStructureGenerate");
        aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(rows));
        aiPromptSvcCallDto.setPromptVarMap(promptParams);
        log.info("提示词结构生成入参：{}", JSONUtil.toJsonStr(aiPromptSvcCallDto));

        aiFormFLowInfo.setHandleStage(AiformFlowEnum.tableStructureGenerate.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.tableStructureGenerate.code());
        cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));
        String response = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
        int num = 0;
        while (num < 5) {
            try {
                LinkedHashMap<String, Object> tableStructureMap = objectMapper.readValue(response, new TypeReference<LinkedHashMap<String, Object>>() {
                });
                break;
            } catch (Exception e) {
                response = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                num++;
            }
        }
        log.info("提示词结构生成返回值：{}", response);
        Map dataMap = toMap(response);
        DataJsonDto entityInfo = getEntityInfo(dataMap);
        Map<String, Object> noNestJson = new LinkedHashMap<>();
        jsonNestHandle(entityInfo.getFieldsInfo(), noNestJson, true);
        Map newDateMap = new LinkedHashMap<>();
        newDateMap = noNestJson;

        LayoutDto layoutDto = new LayoutDto();
        Map<String, Object> entityLayout = new LinkedHashMap<>();
        entityLayout.put("name", entityInfo.getTableName());
        List prevPosition = new ArrayList<>();
        prevPosition.add(0);
        prevPosition.add(0);
        prevPosition.add(0);
        prevPosition.add(0);
        entityLayout.put("position", getPosition(entityInfo.getTableName(), rows, prevPosition));
        layoutDto.setEntity(entityLayout);
        layoutHandle(newDateMap, layoutDto, rows);
        Map<String, Object> structureMap = new LinkedHashMap<>();
        boolean replace = layoutProcess(layoutDto, cellDtos);
        Map map = new LinkedHashMap<>();
        if (replace) {
            map = dataProcess(layoutDto, newDateMap);
            structureMap.put(entityInfo.getTableName(), map);
        } else {
            structureMap.put(entityInfo.getTableName(), newDateMap);
        }
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("structure", structureMap);
        responseMap.put("layout", layoutDto);
        return JSONUtil.toJsonStr(responseMap);
    }

    public String processToEdoc(OcrProcessRequest request, UserUseFormInfo userUseFormInfo,Map llmInitialAnalysisMap,Map llmConfidenceMap,String apiKey) {
        String blocks = request.getOcr();
        log.info("blocks：{}", blocks);
        String cellsProcessor = request.getTsr();

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> list = null;
        try {
            list = objectMapper.readValue(blocks, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> items = specialItemService.findItem(list);
        log.info("识别到的特殊字符：{}", JSONUtil.toJsonStr(items));
        List<CellDto> cellDtos = null;
        try {
            cellDtos = objectMapper.readValue(cellsProcessor, new TypeReference<List<CellDto>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("ocr", blocks);
        ocrResult.put("tsr", cellDtos);
        userUseFormInfo.setOcrResult(writeTxt(JSONUtil.toJsonStr(ocrResult),apiKey));
        adjacentBlockService.process(list, cellDtos, "edoc", llmInitialAnalysisMap, llmConfidenceMap);
        log.info("cellDtos：{}", JSONUtil.toJsonStr(cellDtos));
        log.info("文本拆分合并后数据：{}", JSONUtil.toJsonStr(list));

        AiFormFLowInfo aiFormFLowInfo = request.getAiFormFLowInfo();
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.specialItem.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.specialItem.code());
        cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));

        List rows = null;
        try {
            rows = rowDataBuilderService.process(list, cellDtos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> map = cellLayoutReplace(rows, cellDtos);
        String cellRows = String.valueOf(map.get("rows"));
        Map<String, String> layoutMapping = (Map<String, String>) map.get("mapping");
        log.info("行数据组装后数据：{}", JSONUtil.toJsonStr(cellRows));

        AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
        aiPromptSvcCallDto.setPromptCode("edoc_table_structure_generate");
        aiPromptSvcCallDto.setInput(cellRows);
        aiPromptSvcCallDto.setPromptVarMap(new HashMap<>());
        log.info("提示词结构生成入参：{}", JSONUtil.toJsonStr(aiPromptSvcCallDto));

        aiFormFLowInfo.setHandleStage(AiformFlowEnum.tableStructureGenerate.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.tableStructureGenerate.code());
        cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));
        String tableStructure = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
        int num = 0;
        while (num < 5) {
            try {
                LinkedHashMap<String, Object> tableStructureMap = objectMapper.readValue(tableStructure, new TypeReference<LinkedHashMap<String, Object>>() {
                });
                break;
            } catch (Exception e) {
                tableStructure = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                num++;
            }
        }
        log.info("提示词结构生成返回值：{}", tableStructure);
        return edocPageDslProcess(tableStructure, request.getEdocEntityDtos(), aiFormFLowInfo, cellDtos, layoutMapping, request.getResize(),userUseFormInfo,llmInitialAnalysisMap,llmConfidenceMap);
    }

    public String edocPageDslProcess(String tableStructure, List<EdocEntityDto> entityDtos, AiFormFLowInfo aiFormFLowInfo, List<CellDto> cellDtos, Map<String, String> layoutMapping, String resize, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        LinkedHashMap<String, Object> map = null;
        try {
            map = objectMapper.readValue(tableStructure, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("解析 tableStructure 失败: {}", tableStructure, e);
            throw new RuntimeException("解析 JSON 失败");
        }

        String table = JSONUtil.toJsonStr(map.get("table"));
        LinkedHashMap<String, Object> tableMap = null;
        try {
            tableMap = objectMapper.readValue(table, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("解析 table 失败: {}", table, e);
            throw new RuntimeException("解析 JSON 失败");
        }

        String tableName = tableMap.keySet().iterator().next();
        String tableValue = JSONUtil.toJsonStr(tableMap.get(tableName));

        List<EdocTableStructureDto> edocTableStructureDtos = null;
        try {
            edocTableStructureDtos = objectMapper.readValue(tableValue, new TypeReference<List<EdocTableStructureDto>>() {
            });
        } catch (Exception e) {
            log.error("解析 tableValue 失败: {}", tableValue, e);
            throw new RuntimeException("解析 JSON 失败");
        }

        List<EdocTableStructureDto> tableStructureList = new ArrayList<>();
        List<List<Integer>> nLayoutList = new ArrayList<>();

        for (EdocTableStructureDto edocTableStructureDto : edocTableStructureDtos) {
            String nLayout = edocTableStructureDto.getNLayout().replaceAll(" ", "");
            List<Integer> list = Arrays.stream(nLayout.substring(1, nLayout.length() - 1).split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            nLayoutList.add(list);
        }

        Collections.sort(nLayoutList, (a, b) -> {
            if (Math.abs(a.get(1) - b.get(1)) < 14) {
                return Integer.compare(a.get(0), b.get(0));
            } else {
                return Integer.compare(a.get(1), b.get(1));
            }
        });

        for (List<Integer> layout : nLayoutList) {
            for (EdocTableStructureDto edocTableStructureDto : edocTableStructureDtos) {
                String nLayout = edocTableStructureDto.getNLayout().replaceAll(" ", "");
                List<Integer> list = Arrays.stream(nLayout.substring(1, nLayout.length() - 1).split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                if (layout.equals(list)) {
                    tableStructureList.add(edocTableStructureDto);
                    break;
                }
            }
        }

        return edocPageDslTransferService.transfer(tableName, tableStructureList, entityDtos, aiFormFLowInfo, cellDtos, layoutMapping, resize,userUseFormInfo,llmInitialAnalysisMap,llmConfidenceMap);
    }


    // 坐标替换为tsr
    private Map<String, Object> cellLayoutReplace(List rows, List<CellDto> cellDtos) {
        Map<String, Object> mapReturn = new HashMap<>();
        List newRows = new ArrayList();
        Map<String, String> layoutMapping = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            List<Map<String, String>> row = (List<Map<String, String>>) rows.get(i);
            List<Map<String, String>> newRow = new ArrayList<>();
            for (Map<String, String> map : row) {
                Map<String, String> newMap = new LinkedHashMap<>();
                Iterator<String> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String layout = key.replace(" ", "");
                    List<Integer> collect = Arrays.stream(layout.substring(1, layout.length() - 1).split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    String value = map.get(key);
                    List<Integer> layoutList = new ArrayList<>();
                    boolean replace = false;
                    for (CellDto cellDto : cellDtos) {
                        List<Map<String, Object>> contents = cellDto.getContents();
                        List<Double> location = cellDto.getLocation();
                        int prevY = 0;
                        boolean line = true;
                        int size = contents.size();
                        for (Map<String, Object> content : contents) {
                            Set<String> set = content.keySet();
                            Iterator<String> cellKeys = set.iterator();
                            while (cellKeys.hasNext()) {
                                String next = cellKeys.next();
                                if (next.equals("白") || next.equals("与") || next.equals("4") || next.equals("品") || next.equals("国")) {
                                    size = size - 1;
                                }
                                String nextLayout = next.replace(" ", "");
                                List<Integer> nextCollect = Arrays.stream(nextLayout.substring(1, nextLayout.length() - 1).split(","))
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList());
                                /*if (prevY == 0) {
                                    prevY = nextCollect.get(1);
                                } else {
                                    if (Math.abs(nextCollect.get(1) - prevY) > 15) {
                                        line = false;
                                    }
                                }*/
                                int x1 = (int) Math.round(location.get(0));
                                int y1 = (int) Math.round(location.get(1));
                                int x2 = (int) Math.round(location.get(2));
                                int y2 = (int) Math.round(location.get(3));
                                if (key.replace(" ", "").equals(next.replaceAll(" ", ""))) {
                                    layoutList.add(x1);
                                    layoutList.add(y1);
                                    layoutList.add(x2);
                                    layoutList.add(y2);
                                    replace = true;
                                }
                            }

                            if (size > 1 && replace) {
                                for (Map<String, Object> stringObjectMap : contents) {
                                    Set<String> strings = stringObjectMap.keySet();
                                    Iterator<String> cellKeys2 = strings.iterator();
                                    while (cellKeys2.hasNext()) {
                                        String next = cellKeys2.next();

                                        String nextLayout = next.replace(" ", "");
                                        List<Integer> nextCollect = Arrays.stream(nextLayout.substring(1, nextLayout.length() - 1).split(","))
                                                .map(Integer::parseInt)
                                                .collect(Collectors.toList());
                                        if (prevY == 0) {
                                            prevY = nextCollect.get(1);
                                        } else {
                                            if (Math.abs(nextCollect.get(1) - prevY) > 15) {
                                                line = false;
                                            }
                                        }
                                    }
                                }

                                if (line) {
                                    layoutList.set(0, collect.get(0));
                                    layoutList.set(2, collect.get(2));
                                } else {
                                    layoutList.set(0, collect.get(0));
                                    layoutList.set(1, collect.get(1));
                                    layoutList.set(2, collect.get(2));
                                    layoutList.set(3, collect.get(3));
                                }
                            }
                            if (replace) {
                                break;
                            }
                        }
                        if (replace) {
                            break;
                        }
                    }
                    if (replace) {
                        newMap.put(layoutList.toString(), value);
                        layoutMapping.put(layoutList.toString(), key);
                    } else {
                        newMap.put(key, value);
                    }
                }
                newRow.add(newMap);
            }
            newRows.add(newRow);
        }
        mapReturn.put("rows", newRows.toString());
        mapReturn.put("mapping", layoutMapping);
        return mapReturn;
    }

    private Map<String, Object> dataProcess(LayoutDto layoutDto, Map<String, Object> map) {
        Iterator<String> iterator = map.keySet().iterator();
        List<Map<String, Object>> group = layoutDto.getGroup();
        int i = 0;
        Map<String, Object> newMap = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            String next = iterator.next();
            Object o = map.get(next);
            if (o instanceof JSONObject) {
                List<List<Integer>> layoutList = (List<List<Integer>>) group.get(i).get("value");
                List<List<Integer>> layoutListOld = new ArrayList<>();
                layoutListOld.addAll(layoutList);
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                newMap.put(next, fieldMap);
                List<String> keySet = new ArrayList<>(((JSONObject) o).keySet());
                Collections.sort(layoutList, new Comparator<List<Integer>>() {
                    @Override
                    public int compare(List<Integer> a, List<Integer> b) {
                        if (Math.abs(a.get(1) - b.get(1)) <= 15) {
                            return Integer.compare(a.get(0), b.get(0));
                        } else {
                            return Integer.compare(a.get(1), b.get(1));
                        }
                    }
                });
                for (int m = 0; m < layoutList.size(); m = m + 2) {
                    List<Integer> list = layoutList.get(m);
                    for (int a = 0; a < layoutListOld.size(); a++) {
                        List<Integer> list1 = layoutListOld.get(a);
                        if (list1.equals(list)) {
                            String key = keySet.get(a / 2);
                            Object value = ((JSONObject) o).get(key);
                            fieldMap.put(key, value);
                            break;
                        }
                    }

                }
                i++;
            } else if (o instanceof LinkedHashMap) {
                List<List<Integer>> layoutList = (List<List<Integer>>) group.get(i).get("value");
                List<List<Integer>> layoutListOld = new ArrayList<>();
                layoutListOld.addAll(layoutList);
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                newMap.put(next, fieldMap);
                List<String> keySet = new ArrayList<>(((LinkedHashMap) o).keySet());
                Collections.sort(layoutList, new Comparator<List<Integer>>() {
                    @Override
                    public int compare(List<Integer> a, List<Integer> b) {
                        if (Math.abs(a.get(1) - b.get(1)) <= 15) {
                            return Integer.compare(a.get(0), b.get(0));
                        } else {
                            return Integer.compare(a.get(1), b.get(1));
                        }
                    }
                });
                for (int m = 0; m < layoutList.size(); m = m + 2) {
                    List<Integer> list = layoutList.get(m);
                    for (int a = 0; a < layoutListOld.size(); a++) {
                        List<Integer> list1 = layoutListOld.get(a);
                        if (list1.equals(list)) {
                            String key = keySet.get(a / 2);
                            Object value = ((LinkedHashMap) o).get(key);
                            fieldMap.put(key, value);
                            break;
                        }
                    }

                }
                i++;
            } else if (o instanceof JSONArray || o instanceof ArrayList) {
                newMap.put(next, o);
            }

        }
        return newMap;

    }

    private boolean layoutProcess(LayoutDto layoutDto, List<CellDto> cellDtos) {
        boolean b = false;
        List<Map<String, Object>> groupLayout = layoutDto.getGroup();
        List<Map<String, Object>> groupLayoutNew = new ArrayList<>();
        for (int i = 0; i < groupLayout.size(); i++) {
            int a = 0;
            List<List<Integer>> layoutListNew = new ArrayList<>();
            Map<String, Object> layoutMap = groupLayout.get(i);
            Map<String, Object> layoutMapNew = new LinkedHashMap<>();
            layoutMapNew.put("name", layoutMap.get("name"));
            layoutMapNew.put("position", layoutMap.get("position"));
            layoutMapNew.put("value", layoutListNew);
            List<List<Integer>> layoutList = (List<List<Integer>>) layoutMap.get("value");
            // ocr坐标
            List<Integer> prevLayout = new ArrayList<>();
            // tsr坐标
            List<Integer> prevCell = new ArrayList<>();
            boolean prevReplace = false;

            for (int m = 0; m < layoutList.size(); m++) {
                boolean replace = false;
                List<Integer> list = layoutList.get(m);
//                System.out.println("ocr数据" + list.toString());
                List<Integer> listnew = new ArrayList<>();
                for (CellDto cellDto : cellDtos) {
                    List<Map<String, Object>> contents = cellDto.getContents();
                    List<Double> location = cellDto.getLocation();
                    for (Map<String, Object> content : contents) {
//                        System.out.println("cell数据" + JSONUtil.toJsonStr(content));
                        Iterator<String> iterator = content.keySet().iterator();
                        while (iterator.hasNext()) {
                            String next = iterator.next();
                            if (String.valueOf(list).replace(" ", "").equals(next.replaceAll(" ", ""))) {
//                                System.out.println("cell坐标" + location.toString());
                                listnew.add((int) Math.round(location.get(0)));
                                listnew.add((int) Math.round(location.get(1)));
                                listnew.add((int) Math.round(location.get(2)));
                                listnew.add((int) Math.round(location.get(3)));
                                replace = true;
                                break;
                            }
                        }
                        if (replace) {
                            break;
                        }
                    }
                    if (replace) {
                        break;
                    }
                }

                if (m % 2 == 0 && replace) {
                    prevReplace = true;
                    prevCell.clear();
                    prevCell.addAll(listnew);
                }
                // key没匹配上 value匹配上了 忽略此情况
                if (m % 2 != 0 && replace) {
                    if (!prevReplace) {
                        listnew.clear();
                    }
                }
                // value没有找到tsr信息 并且key的找到了 手动计算坐标
                if (m % 2 != 0 && prevReplace) {
                    if (!replace) {
                        Integer integer = list.get(2) - list.get(0);
                        listnew.add(prevCell.get(2));
                        listnew.add(prevCell.get(1));
                        listnew.add(prevCell.get(2) + integer);
                        listnew.add(prevCell.get(3));
                        prevReplace = false;
                    } else {
                        if (Math.abs(listnew.get(1) - prevCell.get(1)) > 15) {
                            Integer integer = list.get(2) - list.get(0);
                            listnew.clear();
                            listnew.add(prevCell.get(2));
                            listnew.add(prevCell.get(1));
                            listnew.add(prevCell.get(2) + integer);
                            listnew.add(prevCell.get(3));
                            prevReplace = false;
                        }
                    }

                }
                // key没匹配到
                if (m % 2 == 0 && !replace && m != 0 && prevCell.size() > 0) {
                    if (Math.abs(prevLayout.get(1) - list.get(1)) <= 15) {
                        listnew.add(list.get(0));
                        listnew.add(prevCell.get(1));
                        listnew.add(list.get(2));
                        listnew.add(prevCell.get(3));
                        prevReplace = true;
                        prevCell.clear();
                        prevCell.addAll(listnew);

                    } else {
                        prevReplace = false;
                        prevCell.clear();
                    }
                }
                if (listnew.size() == 0) {
                    listnew.addAll(list);
                    a++;
                }
                prevLayout.clear();
                prevLayout.addAll(list);
                if (layoutListNew.size() > 0) {
                    List<Integer> lastList = layoutListNew.get(layoutListNew.size() - 1);
                    if (lastList.equals(listnew)) {
                        listnew.set(0, listnew.get(0) + 1);
                    }
                }
                layoutListNew.add(listnew);
            }
            double proportion = (layoutList.size() - a) / (double) layoutList.size();
            if (Double.compare(proportion, 0.74) == 1) {
                groupLayoutNew.add(layoutMapNew);
                b = true;
            } else {
                groupLayoutNew.add(layoutMap);
            }
        }

        layoutDto.setGroup(groupLayoutNew);
        return b;
    }

    private Map<String, String> processStructures(Map<String, Object> structures) {
        Map<String, String> supplements = new HashMap<>();
        int idx = 1;

        if (structures != null && structures.size() > 0) {
            for (Map.Entry<String, Object> entry : structures.entrySet()) {
                String structureName = entry.getKey();
                Object content = entry.getValue();

                if (content instanceof List) {
                    List<Object> contentList = (List<Object>) content;
                    StringBuilder strContent = new StringBuilder();
                    for (int i = 0; i < contentList.size(); i++) {
                        Object item = contentList.get(i);
                        strContent.setLength(0); // 清空 StringBuilder
                        strContent.append(JSONUtil.toJsonStr(item)); // 假设 item 是一个可以转成 JSON 的对象
                        String newText = "";
                        if ("repeated_set".equals(structureName)) {
                            newText = String.format("%d. %s是已经解析到的%s，" +
                                            "不要修改，直接添加到生成的JSON数据中。如果输入数据中有相关的block，请忽略。\n",
                                    idx, strContent.toString(), "sublist");
                        } else if ("editing_area".equals(structureName)) {
                            newText = String.format("%d. %s是代表文本编辑区的一行，在转译时完成以下操作：1)找到文本编辑区对应的唯一字段，" +
                                            "该字段一般为“签字意见”、“详细内容”、“补充说明”、“工作介绍”等名称； 2) 将字段值设为\"The text editing area for a content field.\" 3) 忽略文本编辑区的整行数据。\n",
                                    idx, strContent.toString());
                            if (i == contentList.size() - 1) {
                                newText += String.format("总共存在%d个文本编辑区，所以对应%d个字段，如果修改的字段数量不是%d，放弃所有修改，重新查找文本编辑区对应的字段，并进行修改。\n",
                                        contentList.size(), contentList.size(), contentList.size());
                            }
                        }
                        // 如果 supplements 中没有该 structureName，初始化一个新的值，否则追加内容
                        if (supplements.get(structureName) == null) {
                            supplements.putIfAbsent(structureName, newText);
                        } else {
                            supplements.put(structureName, supplements.get(structureName) + newText);
                        }
                        idx++;
                    }
                }
            }
        }

        return supplements;
    }

    private String processSupplements(Map<String, String> data) {
        StringBuilder strSupplements = new StringBuilder();

        // 如果 data 为空或没有内容
        if (data == null || data.isEmpty()) {
            strSupplements.append("无");
        } else {
            // 遍历 data 中的每个项，并拼接到 strSupplements
            for (String content : data.values()) {
                strSupplements.append(content);
            }
        }

        return strSupplements.toString();
    }

    public LinkedHashMap toMap(String str) {
        ObjectMapper objectMapper = new ObjectMapper(); // 创建 ObjectMapper 实例
        try {
            // 确保使用有序的 Map（例如 LinkedHashMap）
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            objectMapper.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            return objectMapper.readValue(str, new TypeReference<LinkedHashMap>() {
            });
        } catch (Exception e) {
            log.error("json转换失败：{},json数据:{}", e.getMessage(), str);
            throw new PlatformException("json transfer error");
        }
    }


    /**
     * 获取表数据
     *
     * @param map
     * @return
     */
    public DataJsonDto getEntityInfo(Map map) {
        Iterator iterator = map.keySet().iterator();
        String tableName = "";
        if (iterator.hasNext()) {
            tableName = String.valueOf(iterator.next());
        }
        Map fieldsInfoMap = new LinkedHashMap();
        try {
            fieldsInfoMap = (Map) map.get(tableName);
        } catch (Exception e) {
            log.debug("ocr识别json格式非标准,手动添加tableName,json:{}", JSONUtil.toJsonStr(map));
            fieldsInfoMap = map;
            tableName = UUID.randomUUID().toString();
        }
        DataJsonDto dataJsonDto = new DataJsonDto();
        dataJsonDto.setTableName(tableName);
        dataJsonDto.setFieldsInfo(fieldsInfoMap);
        return dataJsonDto;
    }

    public void jsonNestHandle(Map map, Map newMap, boolean b) {
        ObjectMapper objectMapper = new ObjectMapper();
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Map<String, Object> valMap = new LinkedHashMap<>();
            String key = iterator.next();
            Object o = map.get(key);
            try {
                if (o instanceof LinkedHashMap) {
                    jsonNestHandle((LinkedHashMap) o, valMap, false);
                    if (b) {
                        newMap.put(key, valMap);
                    } else {
                        newMap.putAll(valMap);
                    }
                } else if (o instanceof List) {
                    List jsonArray = (List) o;
                    if (jsonArray.isEmpty()) {
                        continue;
                    }
                    Object firstElement = jsonArray.get(0);
                    if (firstElement instanceof LinkedHashMap && !b) {
                        jsonNestHandle((LinkedHashMap) firstElement, valMap, false);
                        newMap.putAll(valMap);
                    } else {
                        newMap.put(key, o);
                    }
                } else {
                    newMap.put(key, o);
                }
            } catch (Exception e) {
                log.error("解析 JSON 数据失败: {}", e.getMessage(), e);
                newMap.put(key, o);
            }
        }
    }

    public void continuouslyUngroupedHandle(Map map, boolean b, Map newMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String next = String.valueOf(iterator.next());
            Object nextValue = map.get(next);
            Map<String, Object> value = new LinkedHashMap<>();
            try {
                if (nextValue instanceof LinkedHashMap) {
                    value = objectMapper.readValue(objectMapper.writeValueAsString(nextValue), new TypeReference<LinkedHashMap<String, Object>>() {
                    });
                    newMap.put(next, value);
                } else if (nextValue instanceof List) {
                    newMap.put(next, nextValue);
                }
            } catch (Exception e) {
                log.error("解析 JSON 数据失败: {}", e.getMessage(), e);
                newMap.put(next, nextValue);
            }

            if (next.contains("未命名组")) {
                if (iterator.hasNext()) {
                    String nextNext = String.valueOf(iterator.next());
                    Object nextNextValue = map.get(nextNext);
                    try {
                        if (nextNextValue instanceof LinkedHashMap) {
                            if (nextNext.contains("未命名组")) {
                                value.putAll(objectMapper.readValue(objectMapper.writeValueAsString(nextNextValue), new TypeReference<LinkedHashMap<String, Object>>() {
                                }));
                                b = true;
                            } else {
                                newMap.put(nextNext, nextNextValue);
                            }
                        } else if (nextNextValue instanceof List) {
                            newMap.put(nextNext, nextNextValue);
                        }
                    } catch (Exception e) {
                        log.error("解析 JSON 数据失败: {}", e.getMessage(), e);
                        newMap.put(nextNext, nextNextValue);
                    }
                }
            }
        }
        if (b) {
            map.clear();
            map.putAll(newMap);
            newMap.clear();
            continuouslyUngroupedHandle(map, false, newMap);
        }
    }

    private List getPosition(String name, List<List<Map<String, String>>> list, List prevPosition) {
        List positionList = new ArrayList();
        if (name.equals("")) {
            int length = name.length();
            int offset = 15 * length == 0 ? 40 : 15 * length;
            int x2 = offset + (int) prevPosition.get(2);
            positionList.add(prevPosition.get(2));
            positionList.add(prevPosition.get(1));
            positionList.add(x2);
            positionList.add(prevPosition.get(3));
            return positionList;
        }
        boolean b = false;
        for (List<Map<String, String>> maps : list) {
            for (Map<String, String> map : maps) {
                Iterator<String> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    String val = map.get(next);
                    if (Double.compare(StringSimilarity.similarity(name, val), 0.6) == 1 || val.startsWith(name)) {
                        next = next.replaceAll("[\\[\\]]", "");
                        positionList = Arrays.stream(next.split(","))
                                .map(s -> Integer.parseInt(s.trim()))  // 去除空格并转换为 Integer
                                .collect(Collectors.toList());
                        ;
                        maps.remove(map);
                        b = true;
                        break;
                    }
                }
                if (b) {
                    break;
                }
            }
            if (b) {
                break;
            }
        }
        if (!b) {
            int length = name.length();
            int offset = 15 * length == 0 ? 40 : 15 * length;
            int x2 = offset + (int) prevPosition.get(2);
            positionList.add(prevPosition.get(2));
            positionList.add(prevPosition.get(1));
            positionList.add(x2);
            positionList.add(prevPosition.get(3));
        }
        return positionList;
    }


    private void layoutHandle(Map dataMap, LayoutDto layoutDto, List<List<Map<String, String>>> list) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map def = new LinkedHashMap();
        Iterator iteratored = dataMap.keySet().iterator();
        List<Map<String, Object>> group = new LinkedList<>();
        List<Map<String, Object>> sublist = new LinkedList<>();

        while (iteratored.hasNext()) {
            Map<String, Object> groupMap = new LinkedHashMap<>();
            Map<String, Object> sublistMap = new LinkedHashMap<>();
            List<List<Integer>> valuePositionList = new LinkedList<>();
            Map map = new LinkedHashMap<>();
            String key = String.valueOf(iteratored.next());
            Object field = dataMap.get(key);
            List prevPosition = new ArrayList<>();
            prevPosition.add(0);
            prevPosition.add(0);
            prevPosition.add(0);
            prevPosition.add(0);

            try {
                if (field instanceof LinkedHashMap) {
                    groupMap.put("name", key);
                    List position = getPosition(key, list, prevPosition);
                    groupMap.put("position", position);
                    Iterator<String> iterator = ((LinkedHashMap<String, Object>) field).keySet().iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        String val = String.valueOf(((LinkedHashMap<String, Object>) field).get(next));
                        List keyPosition = getPosition(next, list, prevPosition);
                        valuePositionList.add(keyPosition);
                        List valuePosition = getPosition(val, list, keyPosition);
                        valuePositionList.add(valuePosition);
                        prevPosition = valuePosition;
                    }
                    groupMap.put("value", valuePositionList);
                } else if (field instanceof List) {
                    sublistMap.put("name", key);
                    sublistMap.put("position", getPosition(key, list, prevPosition));
                    List jsonArray = (List) field;
                    if (!jsonArray.isEmpty()) {
                        for (Object o : jsonArray) {
                            if (o instanceof LinkedHashMap) {
                                LinkedHashMap jsonObject = (LinkedHashMap) o;
                                Iterator<String> iterator = jsonObject.keySet().iterator();
                                while (iterator.hasNext()) {
                                    String next = iterator.next();
                                    String val = String.valueOf(jsonObject.get(next));
                                    List keyPosition = getPosition(next, list, prevPosition);
                                    valuePositionList.add(keyPosition);
                                    List valuePosition = getPosition(val, list, keyPosition);
                                    valuePositionList.add(valuePosition);
                                    prevPosition = valuePosition;
                                }
                            }
                        }
                    }
                    sublistMap.put("value", valuePositionList);
                }
                if (!groupMap.isEmpty()) {
                    group.add(groupMap);
                }
                if (!sublistMap.isEmpty()) {
                    sublist.add(sublistMap);
                }
            } catch (Exception e) {
                log.error("解析 JSON 数据失败: {}", e.getMessage(), e);
            }
        }

        layoutDto.setSublist(sublist);
        layoutDto.setGroup(group);
    }

    private String writeTxt(String str,String apiKey) {
        UploadRequestDto uploadRequestDto = new UploadRequestDto();
        uploadRequestDto.setAppName("ai-from");
        uploadRequestDto.setFileName(UUID.randomUUID().toString() + ".txt");
        uploadRequestDto.setApiKey(apiKey);
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        FileDto upload = fileService.upload(inputStream, uploadRequestDto);
        return upload.getStorageKey();
    }
    private void handleError(Exception e,UserUseFormInfo userUseFormInfo,AiFormFLowInfo aiFormFLowInfo,Map llmInitialAnalysisMap,Map llmConfidenceMap,String apiKey){
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrProcessError.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrProcessError.code());
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo, aiFormFLowInfo.getId());
        userUseFormInfo.setErrorMessageStorageKey(writeTxt(JSONUtil.toJsonStr(e),apiKey));
        if(llmInitialAnalysisMap!=null){
            userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
        }        if(llmConfidenceMap!=null){
            userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
        }
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.ERROR);
        userUseFormInfo.setEndTime(new Date());
        userUseFormInfoDao.create(userUseFormInfo);
        log.error("ocr识别失败：{}", e);
    }
    // 辅助方法 - 保存到缓存
    private void saveToCache(AiFormFLowInfo aiFormFLowInfo, Long requestId) {
        try {
            // 替换JSONUtil.toJsonStr
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            cacheService.saveToCache(String.valueOf(requestId), objectMapper.writeValueAsString(aiFormFLowInfo));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AiFormFLowInfo", e);
        }
    }

}
