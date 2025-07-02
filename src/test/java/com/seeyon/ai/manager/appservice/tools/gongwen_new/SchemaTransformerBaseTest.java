package com.seeyon.ai.manager.appservice.tools.gongwen_new;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.service.SchemaTransformerBase;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * SchemaTransformerBase类的单元测试
 */
public class SchemaTransformerBaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 测试convertUdc2Dsl方法
     * 将UDC模板转换为DSL格式
     */
    @Test
    public void testConvertUdc2Dsl() throws Exception {
        // 读取测试资源文件
        String resourcePath = "/gongwen_new/template/gw_template_temp1.json";
        JsonNode inputNode = objectMapper.readTree(getClass().getResourceAsStream(resourcePath));

        // 调用被测试方法
        ObjectNode jsonNodes = SchemaTransformer.convertUdc2Dsl(inputNode);
        
        // 将结果保存到指定文件
        String outputPath = "d:/gw_template_result.json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNodes);

        // 初始化标题节点名称
        SchemaTransformerBase.initTitleNodeName(jsonNodes);
        String outputPath2 = "d:/gw_template_result(split).json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath2), jsonNodes);

        // 验证结果
        assertNotNull("处理后的节点不应为空", jsonNodes);
    }
} 