package com.seeyon.ai.manager.appservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SchemaTransformerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void convertLayoutByStreamTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取 UDC_SCHEMA
//            File udcSchemaFile = new File("d:/stream_udcSchema.json");
//            if (!udcSchemaFile.exists()) {
//                System.out.println("文件不存在: " + udcSchemaFile.getAbsolutePath());
//                return;
//            }
//
//            // 读取 OCR_SCHEMA_DEMO
//            File ocrSchemaFile = new File("d:/ocr_schema_demo.json");
//            if (!ocrSchemaFile.exists()) {
//                System.out.println("文件不存在: " + ocrSchemaFile.getAbsolutePath());
//                return;
//            }

            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerCollapse/stream_udcSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();

            // 读取文件内容为字符串
//            String templateStr = mapper.readTree(udcSchemaFile).toString();
//            String layoutSchemaStr = mapper.readTree(ocrSchemaFile).toString();

            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplate(templateStr);
//            params.setLayoutSchema(layoutSchemaStr);



            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/stream_result.json");
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//            System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }
    }





    @Test
    public void convertLayoutByGridTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {
//            // 读取 UDC_SCHEMA
//            File udcSchemaFile = new File("d:/grid_udcSchema.json");
//            if (!udcSchemaFile.exists()) {
//                System.out.println("文件不存在: " + udcSchemaFile.getAbsolutePath());
//                return;
//            }
//
//            // 读取 OCR_SCHEMA_DEMO
//            File ocrSchemaFile = new File("d:/ocr_schema_demo.json");
//            if (!ocrSchemaFile.exists()) {
//                System.out.println("文件不存在: " + ocrSchemaFile.getAbsolutePath());
//                return;
//            }
//
//            // 读取文件内容为字符串
//            String templateStr = mapper.readTree(udcSchemaFile).toString();
//            String layoutSchemaStr = mapper.readTree(ocrSchemaFile).toString();

            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerCollapse/grid_udcSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();


            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplate(templateStr);
//            params.setLayoutSchema(layoutSchemaStr);



            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/grid_result.json");
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//            System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Test
    public void convertLayoutByLabelComponentTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {

            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerCollapse/label_component_udcSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();


            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplate(templateStr);
//            params.setLayoutSchema(layoutSchemaStr);



            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/label_component_result.json");
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//            System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }
    }


//    @Test
//    public void testRemoveTabsNodeAndSaveChildren_MixedContent() throws Exception {
//        // 准备测试数据
//        String input = """
//            {
//                "titleName": "混合内容测试",
//                "groups": [
//                    {
//                        "type": "collapse",
//                        "name": "普通分组1"
//                    },
//                    {
//                        "type": "tabs",
//                        "settings": {
//                            "tabsSetting": [{
//                                "name": "页签1"
//                            },{
//                                "name": "页签2"
//                            }]
//                        },
//                        "components": [
//                            {
//                                "type": "collapse",
//                                "name": "tabs中的分组",
//                                "children": [
//                                    {
//                                        "type": "input",
//                                        "name": "输入框1"
//                                    }
//                                ]
//                            }
//                        ]
//                    },
//                    {
//                        "type": "collapse",
//                        "name": "普通分组2"
//                    }
//                ]
//            }
//            """;
//
//        JsonNode layoutSchema = objectMapper.readTree(input);
//
//        // 执行测试
//        ObjectNode result = SchemaTransformerJsonUtil.removeTabsNodeInOcrSchema(layoutSchema);
//
//        // 验证结果
//        assertNotNull(result);
//        assertTrue(result.has("groups"));
//        JsonNode groups = result.get("groups");
//        assertTrue(groups.isArray());
//        assertEquals(3, groups.size());
//        assertEquals("普通分组1", groups.get(0).get("name").asText());
//        assertEquals("tabs中的分组", groups.get(1).get("name").asText());
//        assertEquals("页签1/页签2", groups.get(1).get("settings").get("belongTabs").asText());
//        assertEquals("普通分组2", groups.get(2).get("name").asText());
//    }
}