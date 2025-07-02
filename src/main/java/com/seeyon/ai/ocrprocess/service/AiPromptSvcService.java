package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.util.HttpRequestUtil;
import com.seeyon.ai.schematransformer.service.LLMMemoryCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiPromptSvcService {
    private static final String basePath = "prompt/";

    @Autowired
    private AppProperties appProperties;

    public Object promptCallService(AiPromptSvcCallDto aiPromptSvcCallDto, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        String promptCode = aiPromptSvcCallDto.getPromptCode();
        String path = basePath + promptCode + ".txt";
        Map<String,String> map = (Map<String, String>) llmInitialAnalysisMap.get(promptCode);
        try {
            String callResult = "";
            long startTime = System.currentTimeMillis();
            if (appProperties.getDeployType().equals("public")) {
                callResult = llmToAiManager(promptCode, aiPromptSvcCallDto.getInput(), aiPromptSvcCallDto.getPromptVarMap());
            } else {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
                String systemContent = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
                String userContent = aiPromptSvcCallDto.getInput();
                Map<String, Object> promptVarMap = aiPromptSvcCallDto.getPromptVarMap();
                Iterator<String> iterator = promptVarMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String value = String.valueOf(promptVarMap.get(key));
                    systemContent = systemContent.replace("{{" + key + "}}", value);
                }

                LLMMemoryCall llmMemoryCall = new LLMMemoryCall(systemContent, appProperties);
                callResult = llmMemoryCall.call("", userContent);
            }
            long endTime = System.currentTimeMillis();
            if (map == null) {
                Map<String,String> requestMap = new HashMap<>();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{\"userInput:\""+aiPromptSvcCallDto.getInput());
                stringBuilder.append(",\"params:\""+ JSONUtil.toJsonStr(aiPromptSvcCallDto.getPromptVarMap())+"}");
                requestMap.put("input",stringBuilder.toString());
                requestMap.put("outPut","{"+callResult+"}");
                llmInitialAnalysisMap.put(promptCode,requestMap);
                llmConfidenceMap.put(promptCode,Math.round((endTime-startTime)/1000.0f));
            } else {
                String input = map.get("input");
                String outPut = map.get("outPut");
                Map<String,String> requestMap = new HashMap<>();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(input+";");
                stringBuilder.append("{\"userInput:\""+aiPromptSvcCallDto.getInput());
                stringBuilder.append(",\"params:\""+ JSONUtil.toJsonStr(aiPromptSvcCallDto.getPromptVarMap())+"}");
                requestMap.put("input",stringBuilder.toString());
                requestMap.put("outPut",outPut+";"+"{"+callResult+"}");
                llmInitialAnalysisMap.put(promptCode,requestMap);
                int prevTime = (int) llmConfidenceMap.get(promptCode);
                llmConfidenceMap.put(promptCode,prevTime+Math.round((endTime-startTime)/1000.0f));

            }
            return callResult;


        } catch (Exception e) {
            log.error("提示词请求异常", e);
            return "";
        }
    }

    public String llmToAiManager(String code, String message, Map<String, Object> paramMap) {
        String url = appProperties.getAiManagerAddress() + "/ai-manager/service/info/api/prompt/" + code;
        long now = System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        params.put("input", message);
        params.put("promptVarMap", paramMap);
        try {
            HttpEntity respEntity = HttpRequestUtil.httpPostRequest(url, params, appProperties.getAiManagerApiKey());
            Map res = null;
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(EntityUtils.toString(respEntity));
            String data = jsonNode.get("data").asText();
            data = data.replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            return data;
        } catch (IOException e) {
            throw new PlatformException("提示词异常");
        }
    }
}
