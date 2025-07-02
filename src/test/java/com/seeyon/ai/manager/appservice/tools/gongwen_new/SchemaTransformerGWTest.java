package com.seeyon.ai.manager.appservice.tools.gongwen_new;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.service.SchemaTransformerGW;
import com.seeyon.ai.schematransformer.dto.SchemaTransformGridTemplateDTO;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class SchemaTransformerGWTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testInitGridTemplateNode() throws IOException {
        // 准备测试数据
        String path = "src/test/resources/gongwen_new/initGridTemplateNode/template.json";
        JsonNode templateDslNode = objectMapper.readTree(new File(path));
        String gridName = "主网格";

        // 执行测试
        SchemaTransformGridTemplateDTO result = SchemaTransformerGW.initGridTemplateNode(templateDslNode, gridName);

        // 验证结果
         assertNotNull("返回结果不应为空", result);
         assertNotNull("网格模板位置不应为空", result.getGridTemplatePosition());
         assertNotNull("标题单元格节点不应为空", result.getGridCellTitleNode());
         assertNotNull("组件单元格节点不应为空", result.getGridCellComponentNode());
         assertNotNull("描述单元格节点不应为空", result.getGridCellDescNode());
         assertNotNull("标题节点不应为空", result.getTitleNode());
         assertNotNull("默认组件节点不应为空", result.getComponentDefaultNode());
         assertNotNull("描述节点不应为空", result.getDescNode());
         assertNotNull("类型模板映射不应为空", result.getTypeTemplateMap());
         assertNotNull("数据字段模板映射不应为空", result.getDataFieldTemplateMap());

        // // 验证标题节点的具体属性
        // JsonNode titleNode = result.getTitleNode();
        // assertEquals("标题节点类型应为label", "label", titleNode.get("type").asText());
        // assertEquals("标题字体大小应为20", 20, titleNode.get("settings").get("textFontSize").asInt());
        // assertEquals("标题颜色应为error-6", "error-6", titleNode.get("settings").get("textColor").asText());
    }


    boolean isWrite2Disk = true;

    @Test
    public void testUnsplit2Split() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_new/demo1/split1.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_new/demo1/unsplit1.json";
        JsonNode unsplitNode = objectMapper.readTree(new File(unsplitPath));

        SchemaTransformerGW.unsplit2Split(unsplitNode);
        // 用split结构的groups对比
        String expectedSplit = objectMapper.writeValueAsString(splitNode);
        String actualSplit = objectMapper.writeValueAsString(unsplitNode);

        if(isWrite2Disk){
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\expectedSplit.json"), objectMapper.readTree(expectedSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入expectedSplit.json失败: " + e.getMessage());
            }
            // 将actualSplit写入D:\0.testcase\actualSplit.json
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\actualSplit.json"), objectMapper.readTree(actualSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入actualSplit.json失败: " + e.getMessage());
            }
        }
        

        
        assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);

        // // unsplit -> split
      
        // assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);
    }

    @Test
    public void testUnsplit2Split2() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_new/demo1/split2.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_new/demo1/unsplit2.json";
        JsonNode unsplitNode = objectMapper.readTree(new File(unsplitPath));

        SchemaTransformerGW.unsplit2Split(unsplitNode);
        // 用split结构的groups对比
        String expectedSplit = objectMapper.writeValueAsString(splitNode);
        String actualSplit = objectMapper.writeValueAsString(unsplitNode);

        if(isWrite2Disk){
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\expectedSplit2.json"), objectMapper.readTree(expectedSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入expectedSplit2.json失败: " + e.getMessage());
            }
            // 将actualSplit写入D:\0.testcase\actualSplit2.json
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\actualSplit2.json"), objectMapper.readTree(actualSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入actualSplit2.json失败: " + e.getMessage());
            }
        }
        

        
        assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);

        // // unsplit -> split
      
        // assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);
    }

    @Test
    public void testUnsplit2Split3() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_new/demo1/split3.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_new/demo1/unsplit3.json";
        JsonNode unsplitNode = objectMapper.readTree(new File(unsplitPath));

        SchemaTransformerGW.unsplit2Split(unsplitNode);
        // 用split结构的groups对比
        String expectedSplit = objectMapper.writeValueAsString(splitNode);
        String actualSplit = objectMapper.writeValueAsString(unsplitNode);

        if(isWrite2Disk){
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\expectedSplit3.json"), objectMapper.readTree(expectedSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入expectedSplit3.json失败: " + e.getMessage());
            }
            // 将actualSplit写入D:\0.testcase\actualSplit3.json
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\actualSplit3.json"), objectMapper.readTree(actualSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入actualSplit3.json失败: " + e.getMessage());
            }
        }
        

        
        assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);

        // // unsplit -> split
      
        // assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);
    }

    @Test
    public void testUnsplit2Split4() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_new/demo1/split4.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_new/demo1/unsplit4.json";
        JsonNode unsplitNode = objectMapper.readTree(new File(unsplitPath));

        SchemaTransformerGW.unsplit2Split(unsplitNode);
        // 用split结构的groups对比
        String expectedSplit = objectMapper.writeValueAsString(splitNode);
        String actualSplit = objectMapper.writeValueAsString(unsplitNode);

        if(isWrite2Disk){
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\expectedSplit4.json"), objectMapper.readTree(expectedSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入expectedSplit4.json失败: " + e.getMessage());
            }
            // 将actualSplit写入D:\0.testcase\actualSplit4.json
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\actualSplit4.json"), objectMapper.readTree(actualSplit));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入actualSplit4.json失败: " + e.getMessage());
            }
        }
        

        
        assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);

        // // unsplit -> split
      
        // assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);
    }

    @Test
    public void testSplit2MergeLabel() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_new/demo1/split1.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_new/demo1/unsplit1.json";
        JsonNode unsplitNode = objectMapper.readTree(new File(unsplitPath));

        SchemaTransformerGW.split2MergeLabel(splitNode);
        // 用split结构的groups对比
        String expected = objectMapper.writeValueAsString(unsplitNode);
        String actual = objectMapper.writeValueAsString(splitNode);

        if(isWrite2Disk){
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\expected.json"), objectMapper.readTree(expected));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入expectedUnsplit.json失败: " + e.getMessage());
            }
            // 将actualUnsplit写入D:\0.testcase\actualUnsplit.json
            try {
                objectMapper.writeValue(new File("D:\\0.testcase\\actual.json"), objectMapper.readTree(actual));
            } catch (Exception e) {
                e.printStackTrace();
                fail("写入actualUnsplit.json失败: " + e.getMessage());
            }
        }
        

        
        assertEquals("split2Split结构应与Split一致", expected, actual);

        // // unsplit -> split
      
        // assertEquals("unsplit2Split结构应与split一致", expectedSplit, actualSplit);
    }

    
} 