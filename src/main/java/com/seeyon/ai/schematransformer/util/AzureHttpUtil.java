package com.seeyon.ai.schematransformer.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.seeyon.ai.schematransformer.annotation.NotNull;
import com.seeyon.ai.schematransformer.constant.CopilotConstants;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.schematransformer.exception.BuzinessException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Azure HTTP工具类
 * 
 * @author wangxun
 */
public class AzureHttpUtil {



    private static final int CONNECT_TIMEOUT = 600 * 60 * 1000; // 600分钟
    private static final int READ_TIMEOUT = 600 * 1000; // 600秒

    public static String sendMessageUriTemplate = "{uri}/openai/deployments/{model}/chat/completions?api-version={apiVersion}";


    /**
     * 发送消息
     * 
     * @param llmMessages 消息列表
     * @return 响应内容
     * @throws IOException IO异常
     * @throws InterruptedException 中断异常
     */
    public static String sendMessage(List<LLMMultiModalMessage> llmMessages, AppProperties appProperties) throws IOException, InterruptedException {

        Map llmBodyMap = getLLMBodyMap(llmMessages);

        String body = CopilotConstants.OBJECT_MAPPER.writeValueAsString(llmBodyMap);

        String url = sendMessageUriTemplate
                .replace("{uri}", appProperties.getLlmUrl())
                .replace("{model}", appProperties.getLlmModel())
                .replace("{apiVersion}", appProperties.getLlmApiVersion());
        String resultStr = sendHttpRequest(url, body,appProperties);
        return resultStr;//getAzureMessage(resultStr);

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

        Map llmBodyMap = getLLMBodyMap(systemTemplate, content);

        String body = CopilotConstants.OBJECT_MAPPER.writeValueAsString(llmBodyMap);

        String url = sendMessageUriTemplate
                .replace("{uri}", appProperties.getLlmUrl())
                .replace("{model}", appProperties.getLlmModel())
                .replace("{apiVersion}", appProperties.getLlmApiVersion());

        String resultStr = sendHttpRequest(url, body,appProperties);
        return resultStr;//getAzureMessage(resultStr);
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
        try {
            // 创建信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // 初始化 SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("SSL 上下文初始化失败", e);
        }
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("POST");
        
        // 设置连接超时
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        
        // 设置读取超时
        connection.setReadTimeout(READ_TIMEOUT);
        String gptApiKey = appProperties.getLlmApiKey();
        if (StringLLMUtil.isEmpty(gptApiKey)) {
            throw new RuntimeException("apiKey is null");
        }
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("api-key", gptApiKey);
        
        // 设置输入输出
        connection.setDoOutput(true);
        
        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // 获取响应
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new BuzinessException("HTTP请求失败，响应码: " + responseCode);
        }
        
        // 读取响应内容
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
    private static @NotNull Map getLLMBodyMap(String systemTemplate, String content) {
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
        return map;
    }

    /**
     * 获取LLM请求体
     * 
     * @param llmMessages 消息列表
     * @return 请求体
     */
    private static @NotNull Map getLLMBodyMap(List<LLMMultiModalMessage> llmMessages) {
//        Map<String,String> systemMessage = new HashMap<>();
//
////        systemMessage.put("role", "system");
////        systemMessage.put("content", systemTemplate);
////        Map<String,String> userMessage = new HashMap<>();
////        userMessage.put("role", "user");
////        userMessage.put("content", content);
////
        Map map = new HashMap<>();
////        List messagesList = new ArrayList();
////        messagesList.add(systemMessage);
////        messagesList.add(userMessage);
        map.put("messages", llmMessages);
        map.put("temperature", 0);
//        map.put("top_p", 0.1);
//        map.put("top_k", 1);
//        map.put("max_tokens", 4096);
//        map.put("frequency_penalty", 0);
//        map.put("presence_penalty", 0);

        return map;
    }
//
//    /**
//     * 构建请求头
//     *
//     * @return 请求头
//     */
//    private static String[] buildHeaders() {
//        // 需要将api-key替换成页面配置
//
//        return new String[]{"Content-Type", "application/json", "api-key", AppSettingsState.getInstance().getGptApiKey()};
//    }

    public static String getAzureMessage(String jsonString) {

        //"choices":[{"finish_reason":"length"}  表示token超长.
        //"choices":[{"finish_reason":"stop"} 表示正常结束
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();


        // 读取methodInfos
        JsonArray methodInfos = jsonObject.getAsJsonArray("choices");
        //异常时格式
        if(methodInfos == null){
            methodInfos = jsonObject.getAsJsonObject("data").getAsJsonArray("choices");
        }
        StringBuilder methodInfoBuilder = new StringBuilder();
        if(methodInfos != null) {
            for (int i = 0; i < methodInfos.size(); i++) {
                if(methodInfos.get(i) != null){

                    JsonObject messageObj = methodInfos.get(i).getAsJsonObject().get("message").getAsJsonObject();
                    String role = messageObj.get("role").getAsString();
                    String content = messageObj.get("content").getAsString();

                    if("assistant".equals(role)){
                        return content;
                    }
                }

            }
        }

        return "";
    }
//    public static String  test() {
//        String content = "请讲一个30字内,关于小狗的故事" ;
//        List<LLMMultiModalMessage> llmMessages = new ArrayList<>();
//        LLMMultiModalMessage llmMultiModalMessage = new LLMMultiModalMessage(LLMMessageType.USER,content);
//        try {
//            llmMessages.add(llmMultiModalMessage);
//            String story = AzureHttpUtil.sendMessage(llmMessages,null);
//            return story;
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static void main(String[] args) {
        String path = "d:/prompt/prompt1.txt";
        try {
            byte[]  bytes = Files.readAllBytes(Paths.get(path));
            String userContent = new String(bytes,"utf-8");

            AppProperties appProperties = new AppProperties();
            appProperties.setDeployType("private");
//            appProperties.setAiManagerAddress("https://seeyon-ai-platform2.openai.azure.com");
//            appProperties.setAiManagerApiKey("9DONRpxJGBZg4VrdA6C8uSFBNjQO3wIcNIDpUTKzfYj95JLj6dahJQQJ99BDACi0881XJ3w3AAAAACOGZBwT");
            appProperties.setLlmUrl("https://seeyon-ai-platform2.openai.azure.com");
            appProperties.setLlmApiVersion("2025-01-01-preview");
            appProperties.setLlmModelType("multi");
            appProperties.setLlmModel("gpt-4o");
            appProperties.setLlmApiKey("9DONRpxJGBZg4VrdA6C8uSFBNjQO3wIcNIDpUTKzfYj95JLj6dahJQQJ99BDACi0881XJ3w3AAAAACOGZBwT");


//            String userContent ="按我的要求执行任务";
//            systemContent = systemContent.replace("{{matched}}","[领导签发,关联流程,文件标题,来文号,文件标题]");
//            systemContent = systemContent.replace("{{matching}}","[密级, 标题, 文号, 文种, 密级(废弃), 保密期限文本,保密期限, 紧急程度, 主送机关, 抄送机关, 签发人, 签发日期 / 成文日期, 发文机关, 印发机关, 印发日期, 附件, 附注, 是否草稿, 发布层次, 呈报,  审批意见, 审核意见, 复核意见, 会签意见, 签发意见, 分发意见, 知会意见,  阅读意见, 文书意见, 处理意见, 拟稿人（ 废弃）, 拟稿日期, 主办部门 / 拟稿部门,  拟稿单位, 拟稿说明, ID, 会签人, 会签部门, 审批人, 复核人, 核稿人, 打印 / 印刷,  校对, 联系电话, 职务, 印发份数, 页码, 备注, 分发人, 分发时间, 文件类别, 行文关系]");
            List<LLMMultiModalMessage> llmMessages = new ArrayList<>();
//            LLMMultiModalMessage systemMessage = new LLMMultiModalMessage(LLMMessageType.SYSTEM,systemContent);
            LLMMultiModalMessage userMessage = new LLMMultiModalMessage(LLMMessageType.USER,userContent);
//            llmMessages.add(systemMessage);
            llmMessages.add(userMessage);
            String story = AzureHttpUtil.sendMessage(llmMessages,appProperties);
            System.out.println(story);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        String test = test();
//        System.out.println(test);
    }


}
