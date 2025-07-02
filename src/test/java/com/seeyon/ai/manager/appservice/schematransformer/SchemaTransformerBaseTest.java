package com.seeyon.ai.manager.appservice.schematransformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.service.SchemaTransformerBase;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * SchemaTransformerBase类的单元测试
 */
public class SchemaTransformerBaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试initTitleNodeName方法
     * 入参是demo_initTitleNodeName.json，将调用后的参数保存到d:/demo0408.json
     */
    @Test
    public void testInitTitleNodeName() throws Exception {
        // 读取测试资源文件
        String resourcePath = "/initTitleNode/demo_initTitleNodeName.json";
        JsonNode inputNode = objectMapper.readTree(getClass().getResourceAsStream(resourcePath));

        String expectResultNodePath = "/initTitleNode/demo_initTitleNodeName_result.json";
        JsonNode expectResultNode = objectMapper.readTree(getClass().getResourceAsStream(expectResultNodePath));

        // 调用被测试方法
        SchemaTransformerBase.initTitleNodeName(inputNode);

        // 将结果保存到指定文件
        String outputPath = "d:/demo0408.json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), inputNode);
        
        // 验证结果
        assertNotNull("处理后的节点不应为空", inputNode);
        
//        // 验证是否存在表头节点
//        boolean hasTitleNode = SchemaTransformerBase.hasTitleNode(inputNode);
//        assertEquals("应该存在表头节点", true, hasTitleNode);


        String realResultStr = null;
        String expectResultStr = null;
        // 删除影响的id
        SchemaTransformerJsonUtil.removeAllNodeId(inputNode);
        SchemaTransformerJsonUtil.removeAllNodeId(expectResultNode);
        try {
            realResultStr = SchemaTransformerUtil.normalizeJson(inputNode);
            expectResultStr = SchemaTransformerUtil.normalizeJson(expectResultNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("和预期结果不匹配",realResultStr,expectResultStr );


        // 验证网格拆分是否正确
        // 这里可以根据实际业务逻辑添加更多验证
    }



    /**
     * 测试initTitleNodeName方法
     * 入参是demo_initTitleNodeName.json，将调用后的参数保存到d:/demo0408.json
     */
    @Test
    public void testConvertUdc2Dsl() throws Exception {
        // 读取测试资源文件
        String resourcePath = "/gongwen/template/gw_template_temp1.json";
        JsonNode inputNode = objectMapper.readTree(getClass().getResourceAsStream(resourcePath));

//        String expectResultNodePath = "/initTitleNode/demo_initTitleNodeName_result.json";
//        JsonNode expectResultNode = objectMapper.readTree(getClass().getResourceAsStream(expectResultNodePath));

        // 调用被测试方法
        ObjectNode jsonNodes = SchemaTransformer.convertUdc2Dsl(inputNode);
        //生成结果以模版中、最大字体；如果没有最大字体，在顶部增加一个标准默认标题容器（程萌提供规范）；
        SchemaTransformerBase.initTitleNodeName(jsonNodes);

        // 将结果保存到指定文件
        String outputPath = "d:/udc转dsl.json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNodes);

//        // 验证结果
//        assertNotNull("处理后的节点不应为空", inputNode);
//
////        // 验证是否存在表头节点
////        boolean hasTitleNode = SchemaTransformerBase.hasTitleNode(inputNode);
////        assertEquals("应该存在表头节点", true, hasTitleNode);
//
//
//        String realResultStr = null;
//        String expectResultStr = null;
//        // 删除影响的id
//        SchemaTransformerJsonUtil.removeAllNodeId(inputNode);
//        SchemaTransformerJsonUtil.removeAllNodeId(expectResultNode);
//        try {
//            realResultStr = SchemaTransformerUtil.normalizeJson(inputNode);
//            expectResultStr = SchemaTransformerUtil.normalizeJson(expectResultNode);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        assertEquals("和预期结果不匹配",realResultStr,expectResultStr );


        // 验证网格拆分是否正确
        // 这里可以根据实际业务逻辑添加更多验证
    }
} 