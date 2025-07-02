package com.seeyon.ai.manager.appservice.tools.dslAndTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.*;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.dto.NodePosition;
import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import com.seeyon.ai.schematransformer.service.SchemaTransformerCollapse;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理模版中有分组的场景
 *
 */
public class SchemaTransformerTest {

    @Test
    public void convertDslSchemaByStream() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/stream_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr =  mapper.readTree(templateNodeContent).toString();
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
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr),(ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("1", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("flexRowSize").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(0).get("children").get(1).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(0).get("children").get(1).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=text 在模版中没有, 则获取第一个:test_aa
        Assert.assertEquals("金额", layoutList.get(2).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("test_aa", layoutList.get(2).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=input 在模版中有, 则匹配上:input_bb
        Assert.assertEquals("input_bb", layoutList.get(2).get("children").get(2).get("sourceSchema").get("id").asText());


    }



    @Test
    public void convertDslSchemaByGrid() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/grid_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr =  mapper.readTree(templateNodeContent).toString();
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
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr),(ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        Assert.assertEquals("1", layoutList.get(0).get("children").get(0).get("settings").get("gridTemplateColumns").get(0).asText());
        Assert.assertEquals("28", layoutList.get(0).get("children").get(0).get("settings").get("gridTemplateRows").get(0).asText());
        //网格

        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=text 在模版中没有, 则获取第一个:test_aa
        Assert.assertEquals("金额", layoutList.get(2).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("test_aa", layoutList.get(2).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=input 在模版中有, 则匹配上:input_bb
        Assert.assertEquals("input_bb", layoutList.get(2).get("children").get(2).get("sourceSchema").get("id").asText());


    }

    /**
     * dataGrid组件在容器组件中
     */
    @Test
    public void convertDslSchemaByGrid_dataGridInCollapse() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();


            String templateNodePath = "SchemaTransformerDemo/grid_dslSchema_dataGridInCollapse.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr =  mapper.readTree(templateNodeContent).toString();
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
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr),(ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        //网格

        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("重复表1", layoutList.get(2).get("children").get(0).get("children").get(0).get("settings").get("title").asText());
        //datagrid 中列 type=text 在模版中没有, 则获取第一个:test_aa
        Assert.assertEquals("金额", layoutList.get(2).get("children").get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("test_aa", layoutList.get(2).get("children").get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=input 在模版中有, 则匹配上:input_bb
        Assert.assertEquals("input_bb", layoutList.get(2).get("children").get(0).get("children").get(0).get("children").get(2).get("sourceSchema").get("id").asText());
        //id需要修改
        Assert.assertNotEquals("ssai123", layoutList.get(2).get("children").get(0).get("id").asText());

    }





    @Test
    public void convertDslSchemaByLabelComponent() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/label_component_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr =  mapper.readTree(templateNodeContent).toString();
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
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr),(ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());

        Assert.assertEquals("0.666667", layoutList.get(0).get("children").get(0).get("settings").get("gridTemplateColumns").get(0).asText());
        Assert.assertEquals(28, layoutList.get(0).get("children").get(0).get("settings").get("gridTemplateRows").get(0).asInt());
        //网格

        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件

        Assert.assertEquals("label", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("settings").get("content").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("label_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("inputNumber", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("type").asText());
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=text 在模版中没有, 则获取第一个:test_aa
        Assert.assertEquals("金额", layoutList.get(2).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("test_aa", layoutList.get(2).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=input 在模版中有, 则匹配上:input_bb
        Assert.assertEquals("input_bb", layoutList.get(2).get("children").get(2).get("sourceSchema").get("id").asText());


    }



//    @Test
//    public void convertDslSchemaBySLabelComponent() {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//
//            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
//            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
//            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();
//
//            String templateNodePath = "SchemaTransformerCollapse/label_component_udcSchema.json";
//            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
//            String templateStr =  mapper.readTree(templateNodeContent).toString();
//
//
//            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplate(templateStr);
//            params.setLayoutSchema(layoutSchemaStr);
//
//
//
//            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));
//
//            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/label_component_result.json");
//            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, convertLayoutByTemplate);
//            System.out.println("更新后的DSL Schema已保存到: " + outputFile.getAbsolutePath());
//
//        } catch (IOException e) {
//            System.out.println("发生IO错误: " + e.getMessage());
//            e.printStackTrace();
//        } catch (Exception e) {
//            System.out.println("发生未预期的错误: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    @Test
    public void testFilterNestedNodePosition() {
        // 准备测试数据
        List<NodePosition> nodePositionList = new ArrayList<>();
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Case 1: 创建一个外层节点（包含inner节点，应该被过滤掉）
        NodePosition outerNode = new NodePosition();
        outerNode.setId("outer");
        ObjectNode outerObjectNode = mapper.createObjectNode();
        outerObjectNode.put("id", "outer");
        
        // 添加一个children数组，包含inner节点
        ArrayNode children = mapper.createArrayNode();
        ObjectNode innerChild = mapper.createObjectNode();
        innerChild.put("id", "inner");
        children.add(innerChild);
        outerObjectNode.set("children", children);
        
        outerNode.setObjectNode(outerObjectNode);
        
        // Case 2: 创建一个内层节点（应该被保留）
        NodePosition innerNode = new NodePosition();
        innerNode.setId("inner");
        ObjectNode innerObjectNode = mapper.createObjectNode();
        innerObjectNode.put("id", "inner");
        innerNode.setObjectNode(innerObjectNode);
        
        // Case 3: 创建一个独立节点（应该被保留）
        NodePosition independentNode = new NodePosition();
        independentNode.setId("independent");
        ObjectNode independentObjectNode = mapper.createObjectNode();
        independentObjectNode.put("id", "independent");
        independentNode.setObjectNode(independentObjectNode);
        
        // 将节点添加到列表中
        nodePositionList.add(outerNode);
        nodePositionList.add(innerNode);
        nodePositionList.add(independentNode);
        
        // 执行过滤
        SchemaTransformerUtil.filterNestedNodePosition(nodePositionList);
        
        // 验证结果
        Assert.assertEquals("过滤后应该只剩下2个节点", 2, nodePositionList.size());
        
        // 验证外层节点被删除
        boolean hasOuterNode = nodePositionList.stream()
                .anyMatch(node -> "outer".equals(node.getId()));
        Assert.assertFalse("外层节点应该被过滤掉", hasOuterNode);
        
        // 验证内层节点和独立节点保留
        boolean hasInnerNode = nodePositionList.stream()
                .anyMatch(node -> "inner".equals(node.getId()));
        boolean hasIndependentNode = nodePositionList.stream()
                .anyMatch(node -> "independent".equals(node.getId()));
        
        Assert.assertTrue("内层节点应该被保留", hasInnerNode);
        Assert.assertTrue("独立节点应该被保留", hasIndependentNode);
        
        // 测试空列表场景
        List<NodePosition> emptyList = new ArrayList<>();
        SchemaTransformerUtil.filterNestedNodePosition(emptyList);
        Assert.assertTrue("空列表处理后应该仍然为空", emptyList.isEmpty());
        
        // 测试null场景
        SchemaTransformerUtil.filterNestedNodePosition(null);
        // 如果没有抛出异常，说明null处理正常
    }

    @Test
    public void testFixGridTemplateRows() throws Exception {
        // 准备测试数据
        String jsonStr = "{\n" +
                "        \"name\": \"未命名组1\",\n" +
                "        \"settings\": {\n" +
                "            \"gridTemplateColumns\": [1],\n" +
                "            \"gridTemplateRows\": [0.5, 1.5],\n" +
                "            \"titleName\": \"未命名组1\"\n" +
                "        },\n" +
                "        \"components\": [{\n" +
                "            \"cellColRow\": {\n" +
                "                \"rowIndex\": 1,\n" +
                "                \"flexRowSize\": 2,\n" +
                "                \"colIndex\": 1,\n" +
                "                \"flexColSize\": 1\n" +
                "            }\n" +
                "        }, {\n" +
                "            \"cellColRow\": {\n" +
                "                \"rowIndex\": 3,\n" +
                "                \"flexRowSize\": 1,\n" +
                "                \"colIndex\": 1,\n" +
                "                \"flexColSize\": 1\n" +
                "            }\n" +
                "        }]\n" +
                "    }";

        // 将JSON字符串转换为JsonNode对象
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);

        // 执行测试方法
        SchemaTransformerCollapse.fixGridTemplateRows(jsonNode);

        // 验证结果

        JsonNode settings = jsonNode.get("settings");
        JsonNode gridTemplateRows = settings.get("gridTemplateRows");

        // 断言
        Assert.assertNotNull("gridTemplateRows should not be null", gridTemplateRows);
        Assert.assertEquals("gridTemplateRows should have 3 elements", 3, gridTemplateRows.size());
        
        // 验证具体的值
        Assert.assertEquals("First row height should be 0.5", 0.25, gridTemplateRows.get(0).asDouble(), 0.001);
        Assert.assertEquals("Second row height should be 0.75", 0.25, gridTemplateRows.get(1).asDouble(), 0.001);
        Assert.assertEquals("Third row height should be 0.75", 1.5, gridTemplateRows.get(2).asDouble(), 0.001);

        // 验证总高度保持不变
        double totalHeight = 0;
        for (JsonNode row : gridTemplateRows) {
            totalHeight += row.asDouble();
        }
        Assert.assertEquals("Total height should remain 2.0", 2.0, totalHeight, 0.001);
    }

    @Test
    public void testFixGridTemplateRowsWithEmptyGroups() throws Exception {
        // 测试空groups的情况
        String jsonStr = "{\"groups\": []}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);
        
        // 执行方法不应抛出异常
        SchemaTransformerCollapse.fixGridTemplateRows(jsonNode);
    }

    @Test
    public void testFixGridTemplateRowsWithNoMultiRow() throws Exception {
        // 准备测试数据 - 所有组件都是单行的情况
       // 准备测试数据
       String jsonStr = "{\n" +
       "        \"name\": \"未命名组1\",\n" +
       "        \"settings\": {\n" +
       "            \"gridTemplateColumns\": [1],\n" +
       "            \"gridTemplateRows\": [0.25,0.25, 1.5],\n" +
       "            \"titleName\": \"未命名组1\"\n" +
       "        },\n" +
       "        \"components\": [{\n" +
       "            \"cellColRow\": {\n" +
       "                \"rowIndex\": 1,\n" +
       "                \"flexRowSize\": 2,\n" +
       "                \"colIndex\": 1,\n" +
       "                \"flexColSize\": 1\n" +
       "            }\n" +
       "        }, {\n" +
       "            \"cellColRow\": {\n" +
       "                \"rowIndex\": 3,\n" +
       "                \"flexRowSize\": 1,\n" +
       "                \"colIndex\": 1,\n" +
       "                \"flexColSize\": 1\n" +
       "            }\n" +
       "        }]\n" +
       "    }";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);

        // 执行测试方法
        SchemaTransformerCollapse.fixGridTemplateRows(jsonNode);

        // 验证结果

//        JsonNode groups = jsonNode.get("groups");
//        JsonNode firstGroup = groups.get(0);
        JsonNode settings = jsonNode.get("settings");
        JsonNode gridTemplateRows = settings.get("gridTemplateRows");
        // 验证具体的值
        Assert.assertEquals("First row height should be 0.5", 0.25, gridTemplateRows.get(0).asDouble(), 0.001);
        Assert.assertEquals("Second row height should be 0.75", 0.25, gridTemplateRows.get(1).asDouble(), 0.001);
        Assert.assertEquals("Third row height should be 0.75", 1.5, gridTemplateRows.get(2).asDouble(), 0.001);

    }
}