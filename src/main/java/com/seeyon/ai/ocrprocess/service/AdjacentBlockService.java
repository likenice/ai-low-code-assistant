package com.seeyon.ai.ocrprocess.service;


import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.CoordinatesDto;
import com.seeyon.ai.ocrprocess.form.TextBlockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AdjacentBlockService {

    @Autowired
    private CellsProcessorService cellsProcessorService;

    @Autowired
    private AiPromptSvcAppService aiPromptSvcAppService;

    private static int yAdjacentDistance = 20;
    private static int limitedTaskNumber = 3;

    public void process(List<Map<String, Object>> blocks, List<CellDto> cellsProcessor, String type, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        List<Map<Map<String, Object>, Map<String, Object>>> overlaps = findOverlaps(blocks);
        if (overlaps.size() > 0) {
            mergeOverlaps(overlaps, blocks,llmInitialAnalysisMap,llmConfidenceMap);
        }
        if (cellsProcessor != null && !cellsProcessor.isEmpty()) {
            mergeBlocksByCells(cellsProcessor, blocks, type,llmInitialAnalysisMap,llmConfidenceMap);
        }

        List<Map<String, Object>> separatedBlocks = new ArrayList<>();
        List<Map<String, Object>> isolatedBlocks = new ArrayList<>();
        List<Map<String, Object>> xIsolatedBlocks = new ArrayList<>();
        List<Map<String, Object>> yIsolatedBlocks = new ArrayList<>();
        Set<List<Integer>> visitedPairs = new HashSet<>();
        List<List<Object>> xAdjacentBlocks = new ArrayList<>();
        List<List<Object>> yAdjacentBlocks = new ArrayList<>();
        xAdjacentBlocks = findXAdjacentBlocks(cellsProcessor, blocks, isolatedBlocks, visitedPairs);
        while (mergeAdjacentPairs(xAdjacentBlocks, blocks, isolatedBlocks,llmInitialAnalysisMap,llmConfidenceMap)) {
            xAdjacentBlocks = findXAdjacentBlocks(cellsProcessor, blocks, isolatedBlocks, visitedPairs);
        }
        for (Map<String, Object> isolatedBlock : isolatedBlocks) {
            xIsolatedBlocks.add(isolatedBlock);
        }
        isolatedBlocks.clear();
        visitedPairs.clear();
        yAdjacentBlocks = findYAdjacentBlocks(cellsProcessor, blocks, isolatedBlocks, visitedPairs);
        while (mergeAdjacentPairs(yAdjacentBlocks, blocks, isolatedBlocks,llmInitialAnalysisMap,llmConfidenceMap)) {
            yAdjacentBlocks = findYAdjacentBlocks(cellsProcessor, blocks, isolatedBlocks, visitedPairs);
        }
        for (Map<String, Object> isolatedBlock : isolatedBlocks) {
            yIsolatedBlocks.add(isolatedBlock);
        }
        List<Map<String, Object>> tempList = new ArrayList<>(xIsolatedBlocks);
        tempList.retainAll(yIsolatedBlocks);  // retainAll保留交集
        isolatedBlocks = tempList;
//        getErrorMergedBlocks(blocks, separatedBlocks);
//        getSeparateBlocks(separatedBlocks, blocks);
        System.out.println(JSONUtil.toJsonStr(blocks));
    }

    private List<Map<Map<String, Object>, Map<String, Object>>> findOverlaps(List<Map<String, Object>> blocks) {
        int size = blocks.size();
        List<Map<Map<String, Object>, Map<String, Object>>> overlaps = new ArrayList<Map<Map<String, Object>, Map<String, Object>>>();
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (seen.contains(i)) {
                // 如果当前块已经处理过，跳过
                continue;
            }
            TextBlockDto block1 = TextBlockDto.initMap(blocks.get(i));
            seen.add(i);
            for (int j = i + 1; j < size; j++) {
                TextBlockDto block2 = TextBlockDto.initMap(blocks.get(j));
                if (areBlocksOverlapped(block1, block2)) {
                    Map<Map<String, Object>, Map<String, Object>> textBlockDtoMap = new HashMap<>();
                    textBlockDtoMap.put(block1.getBlock(), block2.getBlock());
                    overlaps.add(textBlockDtoMap);
                    break;
                }
            }
        }
        return overlaps;
    }

    // 是否有重叠
    private boolean areBlocksOverlapped(TextBlockDto block1, TextBlockDto block2) {
        Map<String, Integer> coordinate1 = CoordinatesDto.getparseCoordinates(block1.getCoordinates());
        Map<String, Integer> coordinate2 = CoordinatesDto.getparseCoordinates(block2.getCoordinates());
        Integer x1 = coordinate1.get("x1");
        Integer y1 = coordinate1.get("y1");
        Integer x2 = coordinate1.get("x2");
        Integer y2 = coordinate1.get("y2");
        Integer x3 = coordinate2.get("x1");
        Integer y3 = coordinate2.get("y1");
        Integer x4 = coordinate2.get("x2");
        Integer y4 = coordinate2.get("y2");
        if (((x1 <= x3 && x3 <= x2) || (x1 <= x4 && x4 <= x2) || (x3 <= x1 && x1 <= x4) || (x3 <= x2 && x2 <= x4)) && isApproxEqual(y4, y2, y3, 5, 0.0)) {
            return true;
        }
        if (((y1 <= y3 && y3 <= y2) || (y1 <= y4 && y4 <= y2) || (y3 <= y1 && y1 <= y4) || (y3 <= y2 && y2 <= y4)) && isApproxEqual(x4, x2, x3, 5, 0.0)) {
            return true;
        }
        return false;
    }

    private boolean isApproxEqual(int val1, int val2, int val3, int absTolerance, double relTolerance) {
        int absDiff = Math.abs(val1 - val2);
        double refDiff = (double) absDiff / Math.abs(val1 - val3);
        return absDiff < absTolerance || Double.compare(refDiff, relTolerance) == -1;

    }

    private void mergeBlocksByCells(List<CellDto> cellsProcessor, List<Map<String, Object>> blocks, String type,Map llmInitialAnalysisMap,Map llmConfidenceMap) {
        cellsProcessorService.populateCellContents(cellsProcessor, blocks);
//        for (CellDto cellDto : cellsProcessor) {
        for (int i = 0; i < cellsProcessor.size(); i++) {
            CellDto cellDto = cellsProcessor.get(i);
            List<String> processedData = new ArrayList<>();
            List<List<Integer>> processedcCoordinates = new ArrayList<>();
            String newText = "";
            List<Map<String, Object>> contents = cellDto.getContents();
            if (contents == null || contents.size() <= 1) {
                continue;
            }
            boolean excute = true;
            for (Map<String, Object> content : contents) {
                if (!blocks.contains(content)) {
                    excute = false;
                    break;
                }
                String value = TextBlockDto.initMap(content).getText();
                List coordinates = TextBlockDto.getParseCoordinatesList(TextBlockDto.initMap(content));
                processedData.add(value);
                processedcCoordinates.add(coordinates);
            }
            if (!excute || processedData == null || processedData.size() == 1) {
                continue;
            }
            Map<String, Object> promptParams = new HashMap<>();
            promptParams.put("batchSize", processedData.size());
//            promptParams.put("batch", JSONUtil.toJsonStr(processedData));
            AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
            if (type.equals("edoc")) {
                aiPromptSvcCallDto.setPromptCode("edoc_cellMerge");
            } else {
                aiPromptSvcCallDto.setPromptCode("cellMerge");
            }
            aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(processedData));
            aiPromptSvcCallDto.setPromptVarMap(promptParams);
            System.out.println(JSONUtil.toJsonStr(processedData));
            newText = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap));
            if (newText.equals("")) {
                continue;
            } else {
                newText = newText.replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            }
            System.out.println("tsr内容合并结果"+newText);
            if (!newText.equals("False")) {
                int x1 = 0;
                int y1 = 0;
                int x2 = 0;
                int y2 = 0;
                for (List<Integer> processedcCoordinate : processedcCoordinates) {
                    x1 = Math.min(x1, processedcCoordinate.get(0)) == 0 ? processedcCoordinate.get(0) : Math.min(x1, processedcCoordinate.get(0));
                    y1 = Math.min(y1, processedcCoordinate.get(1)) == 0 ? processedcCoordinate.get(1) : Math.min(y1, processedcCoordinate.get(1));
                    x2 = Math.max(x2, processedcCoordinate.get(2));
                    y2 = Math.max(y2, processedcCoordinate.get(3));
                }
//                List<Double> location = cellDto.getLocation();
//                double x1 = location.get(0);
//                double y1 = location.get(1);
//                double x2 = location.get(2);
//                double y2 = location.get(3);
                String key = String.format("[%d,%d,%d,%d]", Math.round(x1) - 1, Math.round(y1) - 1, Math.round(x2) - 1, Math.round(y2) - 1);
                Map<String, Object> newBlock = new LinkedHashMap<>();
                newBlock.put(key, newText);
                Map<String, Object> firstBlock = contents.get(0);
                int indexBlock = blocks.indexOf(firstBlock);
                // 插入 newBlock 到 blocks 中
                blocks.add(indexBlock, newBlock);
                // 遍历 contents 移除已经存在的块
                for (Map<String, Object> block : contents) {
                    if (blocks.contains(block)) {
                        while (true) {
                            boolean remove = blocks.remove(block);
                            if (!remove) {
                                break;
                            }

                        }
                    }
                }
            }
            cellsProcessorService.populateCellContents(cellsProcessor, blocks);

        }
    }

    // 合并重叠文本块
    private boolean mergeOverlaps(List<Map<Map<String, Object>, Map<String, Object>>> overlaps, List<Map<String, Object>> blocks,Map llmInitialAnalysisMap,Map llmConfidenceMap) {
        int limitLength = limitedTaskNumber;
        ObjectMapper objectMapper = new ObjectMapper();
        List<Object> processedData = new ArrayList<>();
        List<Object> result = new ArrayList<>();
        for (Map<Map<String, Object>, Map<String, Object>> overlap : overlaps) {
            for (Map.Entry<Map<String, Object>, Map<String, Object>> mapMapEntry : overlap.entrySet()) {
                Map<String, Object> block1 = mapMapEntry.getKey();
                Map<String, Object> block2 = mapMapEntry.getValue();
                List<Object> processedItem = new ArrayList<>();
                List<Map<String, Object>> li = new ArrayList<>();
                li.add(block1);
                li.add(block2);
                for (Map<String, Object> block : li) {
                    processedItem.addAll(block.values());  // 将当前 block 的所有值添加到 processedItem
                }
                processedData.add(processedItem);
            }
        }
        try {
            if (processedData != null) {
                if (processedData.size() < limitLength) {
                    Map<String, Object> promptParams = new HashMap<>();
                    promptParams.put("batchSize", processedData.size());
                    promptParams.put("batch", JSONUtil.toJsonStr(processedData));
                    AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                    aiPromptSvcCallDto.setPromptCode("overlapMerge");
                    aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(processedData));
                    aiPromptSvcCallDto.setPromptVarMap(promptParams);
                    String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                    result = objectMapper.readValue(promptResponse, new TypeReference<List<Object>>() {
                    });
//                result = JSONArray.parseArray(promptResponse);
                } else {
                    for (int i = 0; i < processedData.size(); i += limitLength) {
                        int end = Math.min(i + limitLength, processedData.size());
                        List<Object> batch = processedData.subList(i, end);
                        Map<String, Object> promptParams = new HashMap<>();
                        promptParams.put("batchSize", batch.size());
                        promptParams.put("batch", JSONUtil.toJsonStr(batch));
                        AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                        aiPromptSvcCallDto.setPromptCode("overlapMerge");
                        aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(batch));
                        aiPromptSvcCallDto.setPromptVarMap(promptParams);
                        String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                        if(promptResponse.contains("'")){
                            promptResponse= promptResponse.replaceAll("'","\"");
                        }
                        result.addAll(objectMapper.readValue(promptResponse, new TypeReference<List<Object>>() {
                        }));
                    }
                }
                log.info("合并重叠文本块:{},merge into:{}", JSONUtil.toJsonStr(processedData), JSONUtil.toJsonStr(result));
            }

            if (result.size() == 0) {
                return false;
            }

            for (int i = 0; i < overlaps.size(); i++) {
                Map<Map<String, Object>, Map<String, Object>> mapMapMap = overlaps.get(i);
                Object mergedValue = result.get(i);
                if (!(mergedValue instanceof String)) {
                    continue;
                }
                Map<String, Object> block1 = mapMapMap.keySet().iterator().next();
                Map<String, Object> block2 = mapMapMap.get(block1);
                String key1 = block1.keySet().iterator().next();
                String key2 = block2.keySet().iterator().next();
                TextBlockDto textBlockDto = new TextBlockDto();
                textBlockDto.setCoordinates(key1);
                List<Integer> coords1 = TextBlockDto.getParseCoordinatesList(textBlockDto);
                textBlockDto.setCoordinates(key2);
                List<Integer> coords2 = TextBlockDto.getParseCoordinatesList(textBlockDto);
                // 计算新的坐标
                int newX1 = Math.min(coords1.get(0), coords2.get(0));
                int newY1 = Math.min(coords1.get(1), coords2.get(1));
                int newX2 = Math.max(coords1.get(2), coords2.get(2));
                int newY2 = Math.max(coords1.get(3), coords2.get(3));
                String newCoords = "[" + newX1 + "," + newY1 + "," + newX2 + "," + newY2 + "]";
                // 创建新的 block
                Map<String, Object> newBlock = new LinkedHashMap<>();
                newBlock.put(newCoords, mergedValue);
                // 检查 block1 和 block2 是否在 blocks 中
                if (blocks.contains(block1) && blocks.contains(block2)) {
                    int indexBlock1 = blocks.indexOf(block1);
                    blocks.add(indexBlock1 + 1, newBlock);  // 插入新 block
                    blocks.remove(block1);  // 移除 block1
                    blocks.remove(block2);  // 移除 block2
                }
            }

        } catch (Exception e) {
            log.info("合并重叠文本块时发生错误：{}", e);
            return false;
        }
        return true;
    }

    private List<List<Object>> findXAdjacentBlocks(List<CellDto> cellsProcessor, List<Map<String, Object>> blocks, List<Map<String, Object>> isolatedBlocks, Set<List<Integer>> visitedPairs) {
        int n = blocks.size();
        Set<Integer> blocksInPair = new HashSet<>();
        List<List<Object>> adjacentPairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Object> block1 = TextBlockDto.initMap(blocks.get(i)).getBlock();
            // 如果 block1 已经作为 block2 被添加到 visitedPairs 中，跳过
            if (isVisited(i, visitedPairs) || isolatedBlocks.contains(block1)) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                Map<String, Object> block2 = TextBlockDto.initMap(blocks.get(i + 1)).getBlock();
                List<Integer> pair = new ArrayList<>();
                pair.add(i);
                pair.add(j);
                if (!visitedPairs.contains(pair) && areBlocksXAdjacent(TextBlockDto.initMap(blocks.get(i)), TextBlockDto.initMap(blocks.get(j)), cellsProcessor)) {
                    List<Object> list = new ArrayList<>();
                    list.add("x");
                    list.add(block1);
                    list.add(block2);
                    adjacentPairs.add(list);
                    visitedPairs.add(pair);
                    blocksInPair.add(i);
                    blocksInPair.add(j);
                }
            }
        }
        // 添加孤立的块
        if (isolatedBlocks.isEmpty()) {
            for (int i = 0; i < n; i++) {
                if (!blocksInPair.contains(i)) {
                    isolatedBlocks.add(blocks.get(i));
                }
            }
        }
        return adjacentPairs;
    }

    private List<List<Object>> findYAdjacentBlocks(List<CellDto> cellsProcessor, List<Map<String, Object>> blocks, List<Map<String, Object>> isolatedBlocks, Set<List<Integer>> visitedPairs) {
        int n = blocks.size();
        Set<Integer> blocksInPair = new HashSet<>();
        List<List<Object>> adjacentPairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Object> block1 = TextBlockDto.initMap(blocks.get(i)).getBlock();
            // 如果 block1 已经作为 block2 被添加到 visitedPairs 中，跳过
            if (isVisited(i, visitedPairs) || isolatedBlocks.contains(block1)) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                Map<String, Object> block2 = TextBlockDto.initMap(blocks.get(j)).getBlock();
                List<Integer> pair = new ArrayList<>();
                pair.add(i);
                pair.add(j);
                if (!visitedPairs.contains(pair) && areBlocksYAdjacent(TextBlockDto.initMap(blocks.get(i)), TextBlockDto.initMap(blocks.get(j)), cellsProcessor)) {
                    List<Object> list = new ArrayList<>();
                    list.add("y");
                    list.add(block1);
                    list.add(block2);
                    adjacentPairs.add(list);
                    visitedPairs.add(pair);
                    blocksInPair.add(i);
                    blocksInPair.add(j);
                }
            }
        }
        // 添加孤立的块
        if (isolatedBlocks.isEmpty()) {
            for (int i = 0; i < n; i++) {
                if (!blocksInPair.contains(i)) {
                    isolatedBlocks.add(blocks.get(i));
                }
            }
        }
        return adjacentPairs;
    }

    private boolean isVisited(int i, Set<List<Integer>> visitedPairs) {
        for (List<Integer> visitedPair : visitedPairs) {
            if (i == visitedPair.get(1)) {
                return true;
            }
        }
        return false;
    }

    private boolean areBlocksXAdjacent(TextBlockDto blockDto1, TextBlockDto blockDto2, List<CellDto> cellsProcessor) {
        List<Integer> coords1 = TextBlockDto.getParseCoordinatesList(blockDto1);
        List<Integer> coords2 = TextBlockDto.getParseCoordinatesList(blockDto2);
        Integer x1 = coords1.get(0);
        Integer y1 = coords1.get(1);
        Integer x2 = coords1.get(2);
        Integer y2 = coords1.get(3);
        Integer x3 = coords2.get(0);
        Integer y3 = coords2.get(1);
        Integer x4 = coords2.get(2);
        Integer y4 = coords2.get(3);
        boolean areDifferentCells = true;
        if (cellsProcessor != null && cellsProcessor.size() > 0) {
            CellDto cell1 = cellsProcessorService.findCell(cellsProcessor, blockDto1.getBlock());
            CellDto cell2 = cellsProcessorService.findCell(cellsProcessor, blockDto2.getBlock());
            areDifferentCells = (cell1 == null) != (cell2 == null) || (cell1 != null && cell2 != null && !cell1.getId().equals(cell2.getId()));
        }
        return isApproxEqual(x2, x3, x1, 5, 0.0) && isApproxEqual(y4, y2, y3, 5, 0.0) && !areDifferentCells;
    }

    private boolean areBlocksYAdjacent(TextBlockDto blockDto1, TextBlockDto blockDto2, List<CellDto> cellsProcessor) {
        List<Integer> coords1 = TextBlockDto.getParseCoordinatesList(blockDto1);
        List<Integer> coords2 = TextBlockDto.getParseCoordinatesList(blockDto2);
        Integer x1 = coords1.get(0);
        Integer y1 = coords1.get(1);
        Integer x2 = coords1.get(2);
        Integer y2 = coords1.get(3);
        Integer x3 = coords2.get(0);
        Integer y3 = coords2.get(1);
        Integer x4 = coords2.get(2);
        Integer y4 = coords2.get(3);
        boolean areDifferentCells = true;
        if (cellsProcessor != null && cellsProcessor.size() > 0) {
            CellDto cell1 = cellsProcessorService.findCell(cellsProcessor, blockDto1.getBlock());
            CellDto cell2 = cellsProcessorService.findCell(cellsProcessor, blockDto2.getBlock());
            areDifferentCells = (cell1 == null) != (cell2 == null) || (cell1 != null && cell2 != null && !cell1.getId().equals(cell2.getId()));
        }
        return isApproxEqual(y2, y3, y1, yAdjacentDistance, 0.1) && (Math.abs(x3 - x1) < 40 || Math.abs(x4 - x2) < 40) && !areDifferentCells;
    }

    private boolean mergeAdjacentPairs(List<List<Object>> xAdjacentBlocks, List<Map<String, Object>> blocks, List<Map<String, Object>> isolatedBlocks,Map llmInitialAnalysisMap,Map llmConfidenceMap    ) {
        int limitLength = limitedTaskNumber;
        List<Object> processedData = new ArrayList<>();
        List<Integer> decision = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (List<Object> xAdjacentBlock : xAdjacentBlocks) {
            Map<String, Object> block1 = (Map<String, Object>) xAdjacentBlock.get(1);
            Map<String, Object> block2 = (Map<String, Object>) xAdjacentBlock.get(2);
            List<Object> processedItem = new ArrayList<>();
            List<Map<String, Object>> li = new ArrayList<>();
            li.add(block1);
            li.add(block2);
            for (Map<String, Object> block : li) {
                processedItem.addAll(block.values());  // 将当前 block 的所有值添加到 processedItem
            }
            processedData.add(processedItem);
        }
        if (processedData.size() > 0) {
            if (processedData.size() < limitLength) {
                Map<String, Object> promptParams = new HashMap<>();
                promptParams.put("batchSize", processedData.size());
                promptParams.put("batch", JSONUtil.toJsonStr(processedData));
                AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                aiPromptSvcCallDto.setPromptCode("decidingWhetherToMergeOrNot");
                aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(processedData));
                aiPromptSvcCallDto.setPromptVarMap(promptParams);
                String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                try {
                    decision = objectMapper.readValue(promptResponse, new TypeReference<List<Integer>>() {
                    });
                } catch (Exception e) {
                    log.info("文本合并时提示词未能输出有效结果batch:{},promptResponse:{}", JSONUtil.toJsonStr(processedData), promptResponse);
                }
            } else {
                for (int i = 0; i < processedData.size(); i += limitLength) {
                    int end = Math.min(i + limitLength, processedData.size());
                    List<Object> batch = processedData.subList(i, end);
                    Map<String, Object> promptParams = new HashMap<>();
                    promptParams.put("batchSize", batch.size());
                    promptParams.put("batch", JSONUtil.toJsonStr(batch));
                    AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                    aiPromptSvcCallDto.setPromptCode("decidingWhetherToMergeOrNot");
                    aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(batch));
                    aiPromptSvcCallDto.setPromptVarMap(promptParams);
                    String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                    try {
                        decision.addAll(objectMapper.readValue(promptResponse, new TypeReference<List<Integer>>() {
                        }));
                    } catch (Exception e) {
                        log.info("文本合并时提示词未能输出有效结果batch:{},promptResponse:{}", JSONUtil.toJsonStr(batch), promptResponse);
                    }
                }
            }
        }
        if (decision.size() == 0) {
            return false;
        }
        for (int i = 0; i < xAdjacentBlocks.size(); i++) {
            String adjType = String.valueOf(xAdjacentBlocks.get(i).get(0));
            Map<String, Object> block1 = (Map<String, Object>) xAdjacentBlocks.get(i).get(1);
            Map<String, Object> block2 = (Map<String, Object>) xAdjacentBlocks.get(i).get(2);
            String key1 = TextBlockDto.initMap(block1).getCoordinates();
            String value1 = TextBlockDto.initMap(block1).getText();
            String key2 = TextBlockDto.initMap(block2).getCoordinates();
            String value2 = TextBlockDto.initMap(block2).getText();
            if (decision.get(i) == 1) {
                String newCoords = "";
                String mergedValue = value1 + value2;
                Map<String, Integer> coords1 = CoordinatesDto.getparseCoordinates(key1);
                Map<String, Integer> coords2 = CoordinatesDto.getparseCoordinates(key2);
                Integer x1 = coords1.get("x1");
                Integer y1 = coords1.get("y1");
                Integer x2 = coords1.get("x2");
                Integer y2 = coords1.get("y2");
                Integer x3 = coords2.get("x1");
                Integer y3 = coords2.get("y1");
                Integer x4 = coords2.get("x2");
                Integer y4 = coords2.get("y2");
                if (adjType.equals("x")) {
                    newCoords = "[" + x1 + "," + y1 + "," + x4 + "," + y2 + "]";
                } else if (adjType.equals("y")) {
                    newCoords = "[" + x1 + "," + y1 + "," + x2 + "," + y4 + "]";
                }
                // 创建新的 block
                Map<String, Object> newBlock = new LinkedHashMap<>();
                newBlock.put(newCoords, mergedValue);
                int indexBlock1 = blocks.indexOf(block1);
                blocks.add(indexBlock1 + 1, newBlock);  // 插入新 block
                blocks.remove(block1);  // 移除 block1
                blocks.remove(block2);  // 移除 block2
            } else {
                if (!isolatedBlocks.contains(block1)) {
                    isolatedBlocks.add(block1);
                }
            }
        }
        return true;
    }

    private void getErrorMergedBlocks(List<Map<String, Object>> blocks, List<Map<String, Object>> separatedBlocks) {
        for (Map<String, Object> block : blocks) {
            String value = TextBlockDto.initMap(block).getText();
            Map<String, Integer> rePattern = findRePattern(value);
            if (rePattern != null) {
                String strType = rePattern.keySet().iterator().next();
                if (!strType.equals("")) {
                    separatedBlocks.add(block);
                    continue;
                }
            }
        }

    }

    private Map<String, Integer> findRePattern(String value) {
        // Regular expressions for matching time, date, datetime, integers, and floats
        Pattern datetimePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})");
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2}:\\d{2})");
        Pattern integerPattern = Pattern.compile("(\\d+)");
        Pattern floatPattern = Pattern.compile("(\\d+\\.\\d+)");
        Pattern keyValuePattern = Pattern.compile(":\\D+|：\\D+");

        // Iterate over the patterns
        Pattern[] patterns = {datetimePattern, datePattern, timePattern, integerPattern, floatPattern, keyValuePattern};
        String[] patternTypes = {"datetime", "date", "time", "integer", "float", "key_value"};

        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(value);
            if (matcher.find()) {
                if ("key_value".equals(patternTypes[i])) {
                    Map<String, Integer> newMap = new LinkedHashMap<>();
                    newMap.put(patternTypes[i], matcher.start() + 1);
                    return newMap;
                } else {
                    Map<String, Integer> newMap = new LinkedHashMap<>();
                    newMap.put(patternTypes[i], matcher.start());
                    return newMap;
                }
            }
        }
        return null; // No match found
    }

//    private boolean getSeparateBlocks(List<Map<String, Object>> separatedBlocks, List<Map<String, Object>> blocks) {
//        int limitLength = limitedTaskNumber;
//        List<Object> processedData = new ArrayList<>();
//        List<Integer> decision = new ArrayList<>();
//        ObjectMapper objectMapper = new ObjectMapper();
//        for (Map<String, Object> separatedBlock : separatedBlocks) {
//            String value = String.valueOf(separatedBlock.get(separatedBlock.keySet().iterator().next()));
//            processedData.add(value);
//        }
//        if (processedData.size() > 0) {
//            if (processedData.size() < limitLength) {
//                Map<String, Object> promptParams = new HashMap<>();
//                promptParams.put("batchSize", processedData.size());
//                promptParams.put("batch", JSONUtil.toJsonStr(processedData));
//                AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
//                aiPromptSvcCallDto.setPromptCode("decidingWhetherToSeparate");
//                aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(processedData));
//                aiPromptSvcCallDto.setPromptVarMap(promptParams);
//                String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
//                try {
//                    decision = objectMapper.readValue(promptResponse, new TypeReference<List<Integer>>() {
//                    });
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            } else {
//                for (int i = 0; i < processedData.size(); i += limitLength) {
//                    int end = Math.min(i + limitLength, processedData.size());
//                    List<Object> batch = processedData.subList(i, end);
//                    Map<String, Object> promptParams = new HashMap<>();
//                    promptParams.put("batchSize", batch.size());
//                    promptParams.put("batch", JSONUtil.toJsonStr(batch));
//                    AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
//                    aiPromptSvcCallDto.setPromptCode("decidingWhetherToSeparate");
//                    aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(batch));
//                    aiPromptSvcCallDto.setPromptVarMap(promptParams);
//                    String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
//                    try {
//                        decision.addAll(objectMapper.readValue(promptResponse, new TypeReference<List<Integer>>() {
//                        }));
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                }
//            }
//        }
//        if (decision.size() == 0) {
//            return false;
//        }
//        for (int i = 0; i < separatedBlocks.size(); i++) {
//            Map<String, Object> block = separatedBlocks.get(i);
//            String key = block.keySet().iterator().next();
//            String value = String.valueOf(block.get(key));
//            if (decision.get(i) == 1) {
//                Map<String, Integer> rePattern = findRePattern(value);
//                if (rePattern != null) {
//                    String strType = rePattern.keySet().iterator().next();
//                    Integer startPos = rePattern.get(strType);
//                    String newValue1 = value.substring(0, startPos).trim();
//                    String newValue2 = value.substring(startPos).trim();
//                    double rate1 = (double) startPos / value.length();
//                    double rate2 = 1 - rate1;
//                    Map<String, Integer> coords = CoordinatesDto.getparseCoordinates(key);
//                    Integer x1 = coords.get("x1");
//                    Integer y1 = coords.get("y1");
//                    Integer x2 = coords.get("x2");
//                    Integer y2 = coords.get("y2");
//                    String newKey1 = "[" + x1 + "," + y1 + "," + (x1 + (int) ((x2 - x1) * rate1)) + "," + y2 + "]";
//                    String newKey2 = "[" + (x2 - (int) ((x2 - x1) * rate2)) + "," + y1 + "," + x2 + "," + y2 + "]";
//                    Map<String, Object> newBlock1 = new LinkedHashMap<>();
//                    newBlock1.put(newKey1, newValue1);
//                    Map<String, Object> newBlock2 = new LinkedHashMap<>();
//                    newBlock2.put(newKey2, newValue2);
//                    int indexBlock1 = blocks.indexOf(block);
//                    blocks.add(indexBlock1 + 1, newBlock1);  // 插入新 block
//                    blocks.add(indexBlock1 + 2, newBlock2);  // 插入新 block
//                    blocks.remove(block);
//                }
//
//            }
//        }
//        return true;
//    }


}
