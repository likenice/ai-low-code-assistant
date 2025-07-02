package com.seeyon.ai.manager.appservice.tools.gongwen_0620.unsplit2split;

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

    boolean isWrite2Disk = true;

    @Test
    public void testUnsplit2Split() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_0620/unsplit2split/split1.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_0620/unsplit2split/unsplit1.json";
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
        String splitPath = "src/test/resources/gongwen_0620/unsplit2split/split2.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_0620/unsplit2split/unsplit2.json";
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


    
} 