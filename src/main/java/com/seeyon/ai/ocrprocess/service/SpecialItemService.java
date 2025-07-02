package com.seeyon.ai.ocrprocess.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.ocrprocess.form.TextBlockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SpecialItemService {
    @Autowired
    private SimilarityProcessService similarityProcessService;


    public Map<String, Object> findItem(List<Map<String, Object>> blocks) {
        String specialItemDefinitionsStr = "{\n" +
//                "        \"按钮\": [\"删除\", \"插入\", \"修改\", \"新增\", \"复制\", \"+\", \"-\", \"提交\", \"保存\",\"▼\", \"快速定位\"],\n" +
                "        \"实体关联\": [\"Q\", \"@\", \"田\",\"国\",\"4\",\"/\",\"?\", \"曲\",\"Y\",\"口\", \"勾选\", \"品\", \"白\",\"与\", \"区\", \"吕\", \"Hc\"],\n" +
                "        \"换页\": [\"1条/页\", \"共5页\", \"共5条\"]\n" +
                "    }";
        ObjectMapper objectMapper = new ObjectMapper();

        LinkedHashMap<String, List<String>> specialItemDefinitions;
        try {
            specialItemDefinitions = objectMapper.readValue(specialItemDefinitionsStr, new TypeReference<LinkedHashMap<String, List<String>>>() {});
        } catch (Exception e) {
            log.error("解析 specialItemDefinitionsStr 失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }

        List<Map<String, Object>> blocksToRemove = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();

        for (Map<String, Object> block : blocks) {
            boolean matched = false;
            TextBlockDto textBlockDto = TextBlockDto.initMap(block);
            String coords = textBlockDto.getCoordinates();
            String value = textBlockDto.getText();

            if (!value.isEmpty()) {
                for (String itemName : specialItemDefinitions.keySet()) {
                    List<String> description = specialItemDefinitions.get(itemName);
                    for (String s : description) {
                        if (similarityProcessService.minDistance(value, s) == 0 || (value.length() > 2 && similarityProcessService.minDistance(value, s) == 1)) {
                            if (!items.containsKey(itemName)) {
                                items.put(itemName, block);
                            }
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        break;
                    }
                }
            }
            if (matched) {
                blocksToRemove.add(block);
            }
        }

        for (int i = blocks.size() - 1; i >= 0; i--) {
            Map<String, Object> block = blocks.get(i);
            if (blocksToRemove.contains(block)) {
                blocks.remove(block);
            }
        }

        return items;
    }}
