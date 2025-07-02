package com.seeyon.ai.manager.appservice.tools.gongwen_0620.split2mergeLabel;

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
    public void testsplit2MergeLabel() throws IOException {
        // 读取split结构
        String splitPath = "src/test/resources/gongwen_0620/split2mergelabel/split1.json";
        JsonNode splitNode = objectMapper.readTree(new File(splitPath));
        // 读取unsplit结构
        String unsplitPath = "src/test/resources/gongwen_0620/split2mergelabel/mergeLabel1.json";
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