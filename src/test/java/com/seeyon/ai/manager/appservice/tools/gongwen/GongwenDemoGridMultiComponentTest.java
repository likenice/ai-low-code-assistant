package com.seeyon.ai.manager.appservice.tools.gongwen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GongwenDemoGridMultiComponentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();



    @Test
    public void convertDslSchemaByLableComponent() {
        String layoutSchemaStr = "";
        String templateStr = "";

        String resultStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "gongwen/demo3/demo3_ocr.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "gongwen/demo3/demo3_template.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();

            String resultPath = "gongwen/demo3/demo3_result.json";
            String resultContent = SchemaTransformerUtilTest.readResourcesFile(resultPath);
            resultStr = mapper.readTree(resultContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode realResultNode = convertLayoutByTemplate.getLayoutList();
         // 将更新后的结果重新写入文件
//         File outputFile = new File("d:/demo3_result.json");
//        try {
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());



        // JsonNode templateJsonNode =  mapper.readTree(templateStr);
        // JsonNode layoutSchemaJsonNode =  mapper.readTree(layoutSchemaStr);
        JsonNode expectResultNode = null;
        try {
            expectResultNode = mapper.readTree(resultStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        String realResultStr = null;
        String expectResultStr = null;
        // 删除影响的id
        SchemaTransformerJsonUtil.removeAllNodeId(realResultNode);
        SchemaTransformerJsonUtil.removeAllNodeId(expectResultNode);
        try {
            realResultStr = SchemaTransformerUtil.normalizeJson(realResultNode);
            expectResultStr = SchemaTransformerUtil.normalizeJson(expectResultNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("和预期结果不匹配",realResultStr,expectResultStr );
//        String resultNodePath = "d:/demo1_result.json";
//
//        // 将更新后的结果重新写入文件
//        File outputFile = new File(resultNodePath);
//        try {
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());


    }

}