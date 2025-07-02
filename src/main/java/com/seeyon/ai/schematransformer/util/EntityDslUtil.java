package com.seeyon.ai.schematransformer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityDslUtil {

    /**
     * 将实体JSON转换为DSL格式
     * @param sourceJson 源JSON字符串
     * @return DSL格式的JSON字符串
     */
    public static String convertToDsl(String sourceJson)  {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode sourceNode = null;
        try {
            sourceNode = mapper.readTree(sourceJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode resultArray = mapper.createArrayNode();
        
        // 遍历顶层数组
        for (JsonNode groupNode : sourceNode) {
            ObjectNode newGroup = mapper.createObjectNode();
            newGroup.put("caption", groupNode.get("caption").asText());
            
            // 处理属性列表
            ArrayNode newAttributes = mapper.createArrayNode();
            JsonNode attributes = groupNode.get("attributeDtoList");
            if (attributes != null && attributes.isArray()) {
                for (JsonNode attr : attributes) {
                    ObjectNode newAttr = mapper.createObjectNode();
                    // 只保留指定字段
                    newAttr.put("caption", attr.get("caption").asText());
                    newAttr.put("dataType", attr.get("dataType").asText());
                    newAttr.put("name", attr.get("name").asText());
                    newAttr.put("notNull", attr.get("notNull").asBoolean());
                    
                    newAttributes.add(newAttr);
                }
            }
            
            newGroup.set("attributeDtoList", newAttributes);
            resultArray.add(newGroup);
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultArray);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
//    /**
//     * 单元测试示例
//     */
//    public static void main(String[] args) throws Exception {
//        // 读取源文件
//        String sourceJson = Files.readString(Paths.get("ai-manager-biz/src/test/resources/entity/udc_entity1.json"));
//        // 转换
//        String dslJson = convertToDsl(sourceJson);
//
//        // 写入到指定文件
//        Files.writeString(Paths.get("d:/entity_dsl_demo1.json"), dslJson);
//        log.info("DSL JSON已写入到 d:/entity_dsl_demo1.json");
//
//        // 读取期望的DSL文件内容
//        String expectedDsl = Files.readString(Paths.get("ai-manager-biz/src/test/resources/entity/udc_entity1_dsl.json"));
//        ObjectMapper mapper = new ObjectMapper();
//        // 比较结果(使用normalizeJson确保JSON格式一致)
//        // assertEquals("和预期结果不匹配",
//        //     SchemaTransformerUtil.normalizeJson(mapper.readTree(expectedDsl)),
//        //     SchemaTransformerUtil.normalizeJson(mapper.readTree(dslJson)));
//
//    }
}
