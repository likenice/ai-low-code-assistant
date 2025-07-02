package com.seeyon.ai.schematransformer.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.seeyon.ai.schematransformer.annotation.NotNull;
import com.seeyon.ai.schematransformer.constant.CopilotConstants;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.schematransformer.exception.BuzinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek HTTP工具类
 */
public class DeepSeekHttpUtil {
    
    private static final int CONNECT_TIMEOUT = 600 * 60 * 1000; // 600分钟
    private static final int READ_TIMEOUT = 600 * 1000; // 600秒

    public static String sendMessageUriTemplate = "{uri}/v1/chat/completions";

    /**
     * 发送消息
     * 
     * @param llmMessages 消息列表
     * @return 响应内容
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendMessage(List<LLMMultiModalMessage> llmMessages, AppProperties appProperties) throws IOException, InterruptedException {
        Map llmBodyMap = getLLMBodyMap(llmMessages,appProperties);
        String body = CopilotConstants.OBJECT_MAPPER.writeValueAsString(llmBodyMap);

        String url = sendMessageUriTemplate.replace("{uri}", appProperties.getLlmUrl());
        String resultStr = sendHttpRequest(url, body,appProperties);
        return resultStr;//getDeepSeekMessage(resultStr);
    }

    /**
     * 发送消息
     * 
     * @param systemTemplate 系统模板
     * @param content 内容
     * @return 响应内容
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendMessage(String systemTemplate, String content, AppProperties appProperties) throws IOException, InterruptedException {
        Map llmBodyMap = getLLMBodyMap(systemTemplate, content,appProperties);
        String body = CopilotConstants.OBJECT_MAPPER.writeValueAsString(llmBodyMap);

        String url = sendMessageUriTemplate.replace("{uri}", appProperties.getLlmUrl());
        String resultStr = sendHttpRequest(url, body,appProperties);
        return resultStr;//getDeepSeekMessage(resultStr);
    }

    /**
     * 发送HTTP请求
     * 
     * @param urlString URL
     * @param body 请求体
     * @return 响应内容
     * @throws IOException IO异常
     */
    private static String sendHttpRequest(String urlString, String body, AppProperties appProperties) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        String gptApiKey = appProperties.getLlmApiKey();
        if (StringLLMUtil.isEmpty(gptApiKey)) {
            throw new RuntimeException("apiKey is null");
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + gptApiKey);
        
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new BuzinessException("HTTP请求失败，响应码: " + responseCode);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        String responseBody = response.toString();
        if (StringLLMUtil.isEmpty(responseBody)) {
            throw new BuzinessException("大语言模型请求失败!");
        }
        
        return responseBody;
    }

    /**
     * 获取LLM请求体
     * 
     * @param systemTemplate 系统模板
     * @param content 内容
     * @return 请求体
     */
    private static @NotNull Map getLLMBodyMap(String systemTemplate, String content, AppProperties appProperties) {
        Map<String,String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemTemplate);
        
        Map<String,String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);

        Map map = new HashMap<>();
        List messagesList = new ArrayList();
        messagesList.add(systemMessage);
        messagesList.add(userMessage);
        map.put("messages", messagesList);
        map.put("temperature", 0);
//        map.put("max_tokens", 200);
        map.put("model", appProperties.getLlmModel());
        return map;
    }

    /**
     * 获取LLM请求体
     * 
     * @param llmMessages 消息列表
     * @return 请求体
     */
    private static @NotNull Map getLLMBodyMap(List<LLMMultiModalMessage> llmMessages, AppProperties appProperties) {
        String llmModel = appProperties.getLlmModel();
        if("deepseek-v3".equalsIgnoreCase(llmModel)){
            llmModel = "deepseek-v3";
        }
        Map map = new HashMap<>();
        map.put("messages", llmMessages);
        map.put("temperature", 0);
//        map.put("max_tokens", 200);
        map.put("model", llmModel);
        return map;
    }

    /**
     * 解析DeepSeek响应消息
     * 
     * @param jsonString JSON字符串
     * @return 消息内容
     */
    public static String getDeepSeekMessage(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray choices = jsonObject.getAsJsonArray("choices");
        
        if (choices != null && choices.size() > 0) {
            JsonObject messageObj = choices.get(0).getAsJsonObject().get("message").getAsJsonObject();
            String role = messageObj.get("role").getAsString();
            String content = messageObj.get("content").getAsString();
            
            if ("assistant".equals(role)) {
                return content;
            }
        }
        
        return "";
    }


    public static String  test() {
        String content = "请讲一个30字内,关于小狗的故事" ;
        List<LLMMultiModalMessage> llmMessages = new ArrayList<>();
        LLMMultiModalMessage llmMultiModalMessage = new LLMMultiModalMessage(LLMMessageType.USER,content);
        try {
            llmMessages.add(llmMultiModalMessage);
            AppProperties appProperties = new AppProperties();
            appProperties.setLlmUrl("https://dashscope.aliyuncs.com/compatible-mode");
            appProperties.setLlmModel("deepseek-v3");
            appProperties.setLlmModelType("multi");
            appProperties.setDeployType("private");
            appProperties.setLlmApiKey("sk-df9f09f3c70648ddbe32a4828e8297e0");
            String story = DeepSeekHttpUtil.sendMessage(llmMessages,appProperties);

            return story;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String test = test();
        System.out.println(test);
    }

}
