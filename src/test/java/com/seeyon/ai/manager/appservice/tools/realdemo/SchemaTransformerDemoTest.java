package com.seeyon.ai.manager.appservice.tools.realdemo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SchemaTransformerDemoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void convertLayoutByStreamTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {


            String sourceGroupPath = "realDemo/demo1/demo1_ocr.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "realDemo/demo1/demo1_template.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();

            String resultNodePath = "realDemo/demo1/demo1_result.json";
            String resultContent = SchemaTransformerUtilTest.readResourcesFile(resultNodePath);
            String demo1ResultStr =  mapper.readTree(resultContent).toString();

            JsonNode realNode = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
            // File outputFile = new File("");
            // mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
            // System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());


    
            // JsonNode templateJsonNode =  mapper.readTree(templateStr);
            // JsonNode layoutSchemaJsonNode =  mapper.readTree(layoutSchemaStr);
            JsonNode demo1ResultNode =  mapper.readTree(demo1ResultStr);
//            JsonNode demo1ResultNode2 = demo1ResultNode.get("data");

            SchemaTransformerJsonUtil.removeAllNodeId(demo1ResultNode);
            SchemaTransformerJsonUtil.removeAllNodeId(realNode);

            String demo1ResultNormJson = SchemaTransformerUtil.normalizeJson(demo1ResultNode);
            String realResultNormJson = SchemaTransformerUtil.normalizeJson(realNode);

            Assert.assertEquals("和预期结果不匹配",realResultNormJson,demo1ResultNormJson );
           
            
    

        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }
    }




}