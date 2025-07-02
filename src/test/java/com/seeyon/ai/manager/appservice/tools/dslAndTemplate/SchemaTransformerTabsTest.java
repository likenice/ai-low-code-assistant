package com.seeyon.ai.manager.appservice.tools.dslAndTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SchemaTransformerTabsTest {

    @Test
    public void convertAllTabs() {
        String layoutSchemaStr = "";
        String templateStr = "";
        String resultStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "tabsDemo/1.1ocr_schema_all_tabs_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "tabsDemo/2.1stream_all_tabs_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr =  mapper.readTree(templateNodeContent).toString();

            String result = "tabsDemo/result/1.1_2.1.json";
            String resultContent = SchemaTransformerUtilTest.readResourcesFile(result);
            resultStr =  mapper.readTree(resultContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }
        // 读取文件内容为字符串
//            String templateStr = mapper.readTree(udcSchemaFile).toString();
//            String layoutSchemaStr = mapper.readTree(ocrSchemaFile).toString();

        // 创建参数对象
        SchemaTransformerParams params = new SchemaTransformerParams();
        params.setTemplate(templateStr);
        params.setLayoutSchema(layoutSchemaStr);


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            JsonNode templateJsonNode =  mapper.readTree(templateStr);
            JsonNode layoutSchemaJsonNode =  mapper.readTree(layoutSchemaStr);
            JsonNode resultJsonNode =  mapper.readTree(resultStr);
            convertLayoutByTemplate = SchemaTransformer.convertTabsLayoutByDslTemplate(layoutSchemaJsonNode,templateJsonNode);

            ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

            //id会变化.所以清空
            ((ObjectNode) layoutList.get(0)).put("id", "");
            ((ObjectNode) resultJsonNode.get(0)).put("id", "");
            //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
            SchemaTransformerJsonUtil.removeAllNodeId(layoutList);
            SchemaTransformerJsonUtil.removeAllNodeId(resultJsonNode);

            String layoutListString = SchemaTransformerUtil.normalizeJson(layoutList);
            String resultString = SchemaTransformerUtil.normalizeJson(resultJsonNode);

            Assert.assertEquals("和预期结果不匹配",resultString,layoutListString );
            Assert.assertEquals(2, templateJsonNode.get("children").get(0).get("settings").get("tabsSetting").size());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }



    @Test
    public void convertPartTabs() {
        // String layoutSchemaStr = "";
        // String templateStr = "";
        // String resultStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "tabsDemo/1.2ocr_schema_part_tabs_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            // layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "tabsDemo/2.2stream_part_tabs_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            // templateStr =  mapper.readTree(templateNodeContent);

            String result = "tabsDemo/result/1.2_2.2.json";
            String resultContent = SchemaTransformerUtilTest.readResourcesFile(result);
            // resultStr =  mapper.readTree(resultContent);
       

            SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        // try {
        
            JsonNode layoutSchemaJsonNode =  mapper.readTree(sourceGroupContent);
            JsonNode templateJsonNode =  mapper.readTree(templateNodeContent);
            JsonNode resultJsonNode =  mapper.readTree(resultContent);
            convertLayoutByTemplate = SchemaTransformer.convertTabsLayoutByDslTemplate(layoutSchemaJsonNode,templateJsonNode);

            ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

            //id会变化.所以清空
            for(JsonNode layout : layoutList){
                SchemaTransformerJsonUtil.removeAllNodeId(layout);
            }
            for(JsonNode result2 : resultJsonNode){
                SchemaTransformerJsonUtil.removeAllNodeId(result2);
            }
            //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"

            SchemaTransformerJsonUtil.removeAllNodeId(layoutList);
            SchemaTransformerJsonUtil.removeAllNodeId(resultJsonNode);
            String layoutListString = SchemaTransformerUtil.normalizeJson(layoutList);
            String resultString = SchemaTransformerUtil.normalizeJson(resultJsonNode);

            Assert.assertEquals("和预期结果不匹配",resultString,layoutListString );
        

        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }

}
