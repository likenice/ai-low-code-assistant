package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class RepeatedSetDetectorService {

    @Autowired
    AppProperties appProperties;
    @Autowired
    SimilarityProcessService similarityProcessService;
    @Autowired
    AiPromptSvcAppService aiPromptSvcAppService;
//    @Value("${seeyon.ocr.path:http://10.101.129.4:8889}")
//    private String baseUrl;


    public void process(List rows, Map<String, Object> specialStructures,Map llmInitialAnalysisMap,Map llmConfidenceMap) {
        // 重复节
        Map<String, Object> patterns = new LinkedHashMap<>();
        List<List<List<Object>>> consecutiveRepeatedRows = new LinkedList<>();

        Map<String, Object> params = new HashMap<>();
        params.put("rows", rows);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        HttpEntity respEntity = null;
        JsonNode jsonObject = null;
        ObjectMapper objectMapper = new ObjectMapper();
        String detectDuplicatesUrl = "";
        if(true){
            detectDuplicatesUrl = appProperties.getOcrUrl()+"/udc/detectDuplicates";
        }else {
            detectDuplicatesUrl = appProperties.getOcrUrl()+"/ai-manager/form/detectDuplicates";
        }
        try {
            HttpPost httpPost = new HttpPost(detectDuplicatesUrl);
            StringEntity stringEntity = new StringEntity(objectMapper.writeValueAsString(params), "UTF-8");
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setEntity(stringEntity);
            response = client.execute(httpPost);
            respEntity = response.getEntity();
            String responseString = EntityUtils.toString(respEntity);
            jsonObject = objectMapper.readTree(responseString);
        } catch (IOException e) {
            log.info("重复节结构识别失败：{}", e.getMessage());
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                client.close();
            } catch (IOException e) {
                log.error("关闭 HTTP 连接失败：{}", e.getMessage());
            }
        }

        if (jsonObject != null && "1".equals(jsonObject.get("status").asText())) {
            String result = jsonObject.get("result").asText();
            if (!result.equals("") && !result.equals("[]")) {
                try {
                    consecutiveRepeatedRows = objectMapper.readValue(result, new TypeReference<List<List<List<Object>>>>() {
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (!consecutiveRepeatedRows.isEmpty()) {
                String hints = getHints(consecutiveRepeatedRows);
                System.out.println(hints);
                Map<String, Object> promptParams = new HashMap<>();
                promptParams.put("input", JSONUtil.toJsonStr(rows));
                promptParams.put("hint", hints);
                AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                aiPromptSvcCallDto.setPromptCode("identifySubListsFromRepeated");
                aiPromptSvcCallDto.setInput("按照我的要求完成数据的转译。");
                aiPromptSvcCallDto.setPromptVarMap(promptParams);
                String promptResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                patterns.put("name", "repeated_set");
                patterns.put("content", promptResponse);
                addPatterns(specialStructures, patterns);
            }
        } else {
            if (jsonObject != null) {
                log.info("重复节结构识别失败：{}", jsonObject.get("message").asText());
            } else {
                log.info("重复节结构识别失败：响应格式不正确");
            }
        }
        log.info("rows:{}", JSONUtil.toJsonStr(rows));
        log.info("specialStructures:{}", JSONUtil.toJsonStr(specialStructures));
    }

    public void addPatterns(Map<String, Object> structures, Map<String, Object> patterns) {
        String name = (String) patterns.get("name"); // 获取 patterns 中的 "name"
        List<Map<String, Object>> content = new LinkedList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            content = mapper.readValue(String.valueOf(patterns.get("content")), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 如果 structures 中已经存在对应的 name
        if (structures.containsKey(name)) {
            List<Map<String, Object>> existingContent = (List<Map<String, Object>>) structures.get(name); // 获取现有的内容
            for (Map<String, Object> item : content) {
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


    public String getHints(List<List<List<Object>>> consecutiveRepeatedRows) {
        StringBuilder hint = new StringBuilder();  // 使用 StringBuilder 来拼接字符串
        if (consecutiveRepeatedRows != null && !consecutiveRepeatedRows.isEmpty()) {
            // 遍历每一对重复行
            for (int i = 0; i < consecutiveRepeatedRows.size(); i++) {
                List<List<Object>> repeatedSet = consecutiveRepeatedRows.get(i);
                List<Object> row1 = repeatedSet.get(0);// 第一行
                List<Object> row2 = repeatedSet.get(1);// 第二行
                // 拼接提示字符串
                hint.append(i + 1).append(". ").append(row1).append("是sublist的一行;")
                        .append(row2).append("是下一行。\n");
            }
        }
        return hint.toString();  // 返回生成的提示字符串
    }


    public List<Integer> findLongestConsecutiveSubsequence(List<Integer> list) {
        if (list == null || list.size() == 0) {
            return new ArrayList<>();
        }
        Collections.sort(list);
        // 用来存储当前的连续子序列
        List<Integer> currentSequence = new ArrayList<>();
        // 用来存储最长的连续子序列
        List<Integer> longestSequence = new ArrayList<>();

        // 遍历排序后的列表
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                // 初始化当前连续子序列
                currentSequence.add(list.get(i));
            } else {
                // 检查当前元素与前一个元素的差
                int diff = list.get(i) - list.get(i - 1);
                if (diff == 1 || diff == 2) {
                    // 如果差为1或2，加入当前子序列
                    currentSequence.add(list.get(i));
                } else {
                    // 如果不连续，检查当前子序列是否为最长子序列
                    if (currentSequence.size() > longestSequence.size()) {
                        longestSequence = new ArrayList<>(currentSequence);
                    }
                    // 重置当前子序列
                    currentSequence.clear();
                    currentSequence.add(list.get(i));
                }
            }
        }

        // 最后一次检查
        if (currentSequence.size() > longestSequence.size()) {
            longestSequence = currentSequence;
        }

        return longestSequence;
    }


    public List<Integer> getSubsequenceIndices(List<Integer> fullSeq, List<Integer> subSeq) {
        List<Integer> indices = new ArrayList<>();
        Set<Integer> subSeqSet = new HashSet<>(subSeq);
        // 寻找相同值的下标
        for (int i = 0; i < fullSeq.size(); i++) {
            Integer val = fullSeq.get(i);
            if (subSeqSet.contains(val)) {
                indices.add(i);
            }
        }

        return indices;
    }

    private boolean isConsecutive(List<Integer> seq) {
        // 如果序列长度小于 2，返回 true，因为没有相邻元素需要比较
        if (seq.size() < 2) {
            return true;
        }
        // 遍历序列的每个元素，从第二个元素开始
        for (int i = 1; i < seq.size(); i++) {
            int diff = seq.get(i) - seq.get(i - 1);
            // 检查当前元素与前一个元素的差值是否为 1 或 2
            if (diff != 1 && diff != 2) {
                return false;  // 如果差值不为 1 或 2，返回 false
            }
        }

        // 如果所有相邻元素的差值都符合条件，返回 true
        return true;
    }

}
