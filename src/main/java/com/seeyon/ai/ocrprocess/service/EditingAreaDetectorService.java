package com.seeyon.ai.ocrprocess.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.ocrprocess.form.CoordinatesDto;
import com.seeyon.ai.ocrprocess.form.EditingAreaDefinitionsDto;
import com.seeyon.ai.ocrprocess.form.TextBlockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EditingAreaDetectorService {
    @Autowired
    private SimilarityProcessService similarityProcessService;
    private static double threshold = 0.5;


    /**
     * 富文本识别处理
     *
     * @param rows                   行数据
     * @param specialStructures      特殊结构map
     * @param editingAreaDefinitions 富文本说明
     */
    public void process(List rows, Map<String, Object> specialStructures, String editingAreaDefinitions) {
        List<Map<String, Object>> matches = sequentialWeightedKeywordMatch(rows, editingAreaDefinitions);
        Map<String, Object> patterns = getPatterns(matches, "editing_area");
        if (patterns != null && !patterns.isEmpty()) {
            addPatterns(specialStructures, patterns);
        }
        for (Map<String, Object> match : matches) {
            Object row = match.get("row");
            rows.remove(row);
        }
    }

    public void addPatterns(Map<String, Object> structures, Map<String, Object> patterns) {
        String name = (String) patterns.get("name"); // 获取 patterns 中的 "name"
        List<List<Map<String, Object>>> content = (List<List<Map<String, Object>>>) patterns.get("content"); // 获取 patterns 中的 "content"
        // 如果 structures 中已经存在对应的 name
        if (structures.containsKey(name)) {
            List<List<Map<String, Object>>> existingContent = (List<List<Map<String, Object>>>) structures.get(name); // 获取现有的内容
            for (List<Map<String, Object>> item : content) {
                // 如果 content 中的项不在现有内容中，则添加
                if (!existingContent.contains(item)) {
                    existingContent.add(item);
                }
            }
        } else {
            // 如果 structures 中没有对应的 name，直接将 content 添加进去
            structures.put(name, content);
        }
    }


    private List<Map<String, Object>> sequentialWeightedKeywordMatch(List rows, String editingAreaDefinitions) {
        List<Map<String, Object>> matched_rows = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper(); // 创建 ObjectMapper 实例
        for (Object o : rows) {
            List row;
            if (o instanceof List) {
                row = (List) o;
            } else {
                try {
                    // 使用 Jackson 解析 JSON 字符串
                    JsonNode jsonNode = objectMapper.readTree(String.valueOf(o));
                    if (jsonNode.isArray()) {
                        row = new ArrayList<>();
                        for (JsonNode element : jsonNode) {
                            row.add(objectMapper.convertValue(element, Object.class));
                        }
                    } else {
                        row = Collections.singletonList(objectMapper.convertValue(jsonNode, Object.class));
                    }
                } catch (Exception e) {
                    log.error("解析 JSON 字符串失败: {}", o, e);
                    continue;
                }
            }
            List<TextBlockDto> blocks = TextBlockDto.init(row);
            Map<String, Object> map = checkKeywordOrderAndWeight(blocks, editingAreaDefinitions);

            if ((boolean) map.get("match")) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("row", row);
                response.put("totalWeight", map.get("totalWeight"));
                matched_rows.add(response);
            }
        }

        return matched_rows;
    }

    private Map<String, Object> getPatterns(List<Map<String, Object>> matches, String patternName) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<List<Map<String, Object>>> content = new LinkedList<>();
        for (Map<String, Object> match : matches) {
            content.add((List<Map<String, Object>>) match.get("row"));
        }
        map.put("name", patternName);
        map.put("content", content);
        return map;
    }

    private Map<String, Object> checkKeywordOrderAndWeight(List<TextBlockDto> blocks, String editingAreaDefinitions) {
        Map<String, Object> map = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper(); // 创建 ObjectMapper 实例

        List<EditingAreaDefinitionsDto> editingAreaDefinitionsDtos;
        try {
            // 使用 Jackson 解析 JSON 字符串到目标对象列表
            editingAreaDefinitionsDtos = objectMapper.readValue(
                    editingAreaDefinitions,
                    new TypeReference<List<EditingAreaDefinitionsDto>>() {}
            );
        } catch (Exception e) {
            log.error("解析 editingAreaDefinitions 失败: {}", editingAreaDefinitions, e);
            map.put("match", false);
            map.put("totalWeight", 0.00);
            return map;
        }

        // 按照顺序排序
        List<EditingAreaDefinitionsDto> keywords = editingAreaDefinitionsDtos.stream()
                .sorted(Comparator.comparingInt(EditingAreaDefinitionsDto::getOrder))
                .collect(Collectors.toList());

        Double totalWeight = 0.00;
        int lastPositionX1 = -1;
        int lastPositionY1 = -1;

        for (EditingAreaDefinitionsDto keywordEntry : keywords) {
            List<String> keywordsList = keywordEntry.getKeywords();
            String matchMethod = keywordEntry.getMatchMethod();
            Double weight = keywordEntry.getWeight();
            boolean found = false;

            for (String keyword : keywordsList) {
                for (TextBlockDto block : blocks) {
                    if (isMath(block.getText(), keyword, matchMethod, null)) {
                        Integer positionX1 = CoordinatesDto.getparseCoordinateX1(block.getCoordinates());
                        Integer positionY1 = CoordinatesDto.getparseCoordinateY1(block.getCoordinates());

                        if (positionX1 != null && positionY1 != null) {
                            if (positionX1 > lastPositionX1 || positionY1 > lastPositionY1) {
                                totalWeight += weight;
                                lastPositionX1 = positionX1;
                                lastPositionY1 = positionY1;
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        map.put("match", totalWeight >= threshold);
        map.put("totalWeight", totalWeight);
        return map;
    }

    private boolean isMath(String text, String targetText, String matchMethod, Double threshold) {
        if (threshold != null) {
            threshold = threshold;
        } else if (matchMethod.equals("levenshtein")) {
            threshold = 0.25;
        } else if (matchMethod.equals("cosine")) {
            threshold = 0.4;
        }


        if (matchMethod.equals("direct") && similarityProcessService.isDirectMath(text, targetText)) {
            return true;
        } else if (matchMethod.equals("levenshtein") && similarityProcessService.isLevenshteinMatch(text, targetText, threshold)) {
            return true;
        } else if (matchMethod.equals("levenshtein") && similarityProcessService.isCosineMatch(text, targetText, threshold)) {
            return true;
        }
        return false;
    }

}
