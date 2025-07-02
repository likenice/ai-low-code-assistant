package com.seeyon.ai.schematransformer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * SchemaTransformerGW 单元测试
 */
public class SchemaTransformerGWTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试 createOneCellUpDownNode 方法
     * 将OCR节点转换为ONE_CELL_UP_DOWN类型节点
     */
    @Test
    public void testCreateOneCellUpDownNode() throws Exception {
        // 读取输入数据
        String inputContent = SchemaTransformerUtilTest.readResourcesFile(
                "gongwen_0620/convertOneCellUpDown/in.json");
        JsonNode inputNode = objectMapper.readTree(inputContent);
        
        // 读取期望结果
        String expectedContent = SchemaTransformerUtilTest.readResourcesFile(
                "gongwen_0620/convertOneCellUpDown/result.json");
        JsonNode expectedResult = objectMapper.readTree(expectedContent);
        
        // 调用被测试方法
        JsonNode actualResult = SchemaTransformerGW.createOneCellUpDownNode(inputNode);
        
        // 标准化JSON后进行对比
        String normalizedActual = SchemaTransformerUtil.normalizeJson(actualResult);
        String normalizedExpected = SchemaTransformerUtil.normalizeJson(expectedResult);
        
        // 断言结果相等
        org.junit.Assert.assertEquals("转换结果与期望结果不匹配", 
                normalizedExpected, normalizedActual);
    }
} 