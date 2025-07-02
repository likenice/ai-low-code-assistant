package com.seeyon.ai.schematransformer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JsonUtil {


    public static String getClassPackage(String jsonString) {
//        String jsonString = "{\n" +
//                "    \"classInfo\": \"#{替换method代码}\",\n" +
//                "    \"methodInfos\": [{\"methodInfo\": \"\"}]\n" +
//                "}";

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        // 读取classInfo
        if(jsonObject.get("classPackage") != null){
            return jsonObject.get("classPackage").getAsString();
        }
        return "";

    }

    public static String getClassName(String jsonString) {
//        String jsonString = "{\n" +
//                "    \"classInfo\": \"#{替换method代码}\",\n" +
//                "    \"methodInfos\": [{\"methodInfo\": \"\"}]\n" +
//                "}";

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        // 读取classInfo
        if(jsonObject.get("className") != null){
            return jsonObject.get("className").getAsString();
        }
        return "";

    }


    public static String getClassContent(String jsonString) {
//        String jsonString = "{\n" +
//                "    \"classInfo\": \"#{替换method代码}\",\n" +
//                "    \"methodInfos\": [{\"methodInfo\": \"\"}]\n" +
//                "}";

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        // 读取classInfo
        if(jsonObject.get("classContent") != null){
            return jsonObject.get("classContent").getAsString();
        }
        return "";

    }
    public static String getClassImport(String jsonString) {

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        // 读取classInfo
        if(jsonObject.get("classImport") != null){
            return jsonObject.get("classImport").getAsString();
        }
        return "";

    }


    public static List<String> getMethodNameList(String jsonString) {
//        String jsonString = "{\n" +
//                "    \"classInfo\": \"#{替换method代码}\",\n" +
//                "    \"methodInfos\": [{\"methodInfo\": \"\"}]\n" +
//                "}";

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        List<String> methodNameList = new ArrayList<>();

        // 读取methodInfos
        JsonArray methodInfos = jsonObject.getAsJsonArray("methodInfos");
        StringBuilder methodInfoBuilder = new StringBuilder();
        if(methodInfos != null) {
            for (int i = 0; i < methodInfos.size(); i++) {
                if(methodInfos.get(i) != null){
                    methodNameList.add( methodInfos.get(i).getAsJsonObject().get("methodName").getAsString());
                }

            }
        }

        return methodNameList;
    }

    public static List<String> getMethodContentList(String jsonString) {
//        String jsonString = "{\n" +
//                "    \"classInfo\": \"#{替换method代码}\",\n" +
//                "    \"methodInfos\": [{\"methodInfo\": \"\"}]\n" +
//                "}";

        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        List<String> methodInfoList = new ArrayList<>();

        // 读取methodInfos
        JsonArray methodInfos = jsonObject.getAsJsonArray("methodInfos");
        StringBuilder methodInfoBuilder = new StringBuilder();
        if(methodInfos != null) {
            for (int i = 0; i < methodInfos.size(); i++) {
                if(methodInfos.get(i) != null){
                    methodInfoList.add( methodInfos.get(i).getAsJsonObject().get("methodContent").getAsString());
                }

            }
        }

        return methodInfoList;
    }

    /**
     * 是否长度截断结束
     * @param jsonString
     * @return
     */
    public static boolean isLengthFinish(String jsonString) {

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
                    String finishReason = methodInfos.get(i).getAsJsonObject().get("finish_reason").getAsString();
                    if(!"stop".equalsIgnoreCase(finishReason)){
                        if("length".equalsIgnoreCase(finishReason)) {
                            return true;
                        }
                    }
                }

            }
        }

        return false;
    }


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

    public static String getAzureUseTokens(String jsonString) {


        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();


        // 读取methodInfos
        JsonObject usage = jsonObject.getAsJsonObject("usage");
        if(usage == null){
            usage = jsonObject.getAsJsonObject("data").getAsJsonObject("usage");
        }
        StringBuilder methodInfoBuilder = new StringBuilder();

        if(usage != null) {
            return usage.getAsJsonObject().get("total_tokens").getAsString();
        }

        return "";
    }

    public static String object2Json(Collection objList) {
        String json = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            json = objectMapper.writeValueAsString(objList);
//            System.out.println(json); // 输出: [{"name":"Item1"},{"name":"Item2"}]
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String object2Json(Object object) {
        String json = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * 删除两个字符串之间的重复内容，只处理before末尾和curr开头之间的重叠部分
     * @param before 之前的文本
     * @param curr 当前文本
     * @return 合并后的文本
     */
    public static String deleteDuplite(String before, String curr) {
        if (curr == null || before == null) {
            return curr;
        }
        
        // 从最长的可能重复长度开始尝试
        int maxLength = Math.min(curr.length(), before.length());
        for (int i = maxLength; i > 0; i--) {
            String currPrefix = curr.substring(0, i);
            String beforeSuffix = before.substring(before.length() - i);
            
            if (currPrefix.equals(beforeSuffix)) {
                return before + curr.substring(i);
            }
        }
        
        // 没有找到重复部分，直接拼接
        return before + curr;
    }
    public static String deleteDuplite(List<String> resultList) {
        if(resultList == null || resultList.size() == 0){
            return "";
        }
        String result = resultList.get(0);
        for(int i = 1; i < resultList.size(); i++){
            result = deleteDuplite( result,resultList.get(i));
        }
        return result;
    }

    /**
     * 将source 所有属性和子属性都按照首字母排序
     * @param source
     * @return
     */
    public static String orderJsonProps(String source){
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode node = objectMapper.readTree(source);
            return SchemaTransformerUtil.normalizeJson(node);
        }catch (Exception e){
            e.printStackTrace();
        }
        return source;
    }

    public static void main(String[] args) {
        String before = "{\"vLayout\": \"[729, 460, 779, 516]\",\n\"type\": \"f\"\n},\n{\n \"fn\": \"监事会工作秘书核稿\",\n\"nLayout\": \"[32, 5";
        String curr = "{\n \"fn\": \"监事会工作秘书核稿\",\n\"nLayout\": \"[32, 534]\",\"aaa\":\"bbb\"}";

       List<String> resultList = new ArrayList<>();
       resultList.add(before);
       resultList.add(curr);
       resultList.add(before);
       resultList.add(curr);


       String result = deleteDuplite(resultList);
       System.out.println("result:============"+result);

        // String result = deleteDuplite(before,curr);
        // System.out.println("result:============"+result);

    }

    public static void writeFile(String data, String filePath) {
        // 定义要写入的字符串内容
//        String rule2Expected = "Your string content here";  // 请替换为实际的字符串值

        // 指定输出文件的路径
//        String filePath = "d:/expect.json";

        // 写入到文件中
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            // 将字符串写入到文件中
            fileWriter.write(data);
            System.out.println("写入成功");
        } catch (IOException e) {
            System.out.println("写入文件时出错: " + e.getMessage());
        }
    }

    public static Set<String> getAllUdcReferenceFullName(JsonNode udcSchema){

        Set<String> filedFullNameLists = new HashSet<>();
        JsonNode jsonNode = udcSchema.get("childById");
        jsonNode.fieldNames().forEachRemaining(field -> {
            JsonNode nodeInfo = jsonNode.get(field);
            if(nodeInfo.get("type") == null){
                return;
            }
            String nodeType = nodeInfo.get("type").asText();
            if(nodeType.equals("UdcReference")){
                if(nodeInfo.has("settings") && nodeInfo.get("settings").has("dataReference")  ){
                    JsonNode dataReferenceNode = nodeInfo.get("settings").get("dataReference");
                    String fullName = dataReferenceNode.get("fullName").asText();
                    String appName = dataReferenceNode.get("appName").asText();
                    filedFullNameLists.add(fullName+"_"+appName);
                }

            }

        });
        return filedFullNameLists;
    }

    /**
     * 更新udcNode 中type= "UdcReference"  , 设置settings-> dataReference -> fullName
     *
     * @param udcNode
     * @param referenceMap
     */
    public static void updateUdcReference(JsonNode udcNode, Map<String, String> referenceMap) {
        JsonNode jsonNode = udcNode.get("childById");
        jsonNode.fieldNames().forEachRemaining(field -> {
            JsonNode nodeInfo = jsonNode.get(field);
            if(nodeInfo.get("type") == null || !nodeInfo.get("type").asText().equals("UdcReference")){
                return;
            }

            if(nodeInfo.has("settings") && nodeInfo.get("settings").has("dataReference") && nodeInfo.get("settings").get("dataReference").has("entityFullName")){
                JsonNode dataReferenceNode = nodeInfo.get("settings").get("dataReference");
                JsonNode entityFullNameNode = dataReferenceNode.get("entityFullName");
                JsonNode appNameNode = dataReferenceNode.get("appName");
                if(entityFullNameNode != null && !entityFullNameNode.isNull()){
                    String key = entityFullNameNode.asText() +"_"+appNameNode.asText();
                    String referenceFullName = referenceMap.get(key);
                    if(referenceFullName != null){
                        ((ObjectNode)dataReferenceNode).put("fullName", referenceFullName);
                    }
                }

            }
        });
    }

    /**
     * filedFullName = "com.seeyon.edoc335172694483814428.domain.entity.IssuedDocument.issueOpinion", 返回 "com.seeyon.edoc335172694483814428.domain.entity.IssuedDocument"
     * 获取最后一个句号前的字符串
     * @param filedFullName
     * @return
     */
    public static String getEntityFullNameByFieldFullName(String filedFullName){
        int index = filedFullName.lastIndexOf(".");
        if(index == -1){
            return filedFullName;
        }
        return filedFullName.substring(0,index);
    }
}
