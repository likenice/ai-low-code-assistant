package com.seeyon.ai.manager.appservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.service.SchemaTransformerDataGrid;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchemaTransformerDataGridTest {

    private JsonNode loadJsonFromFile(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return new ObjectMapper().readTree(content);
    }

    @Test
    public void testConvertGroupDataGridStructureInDataGridNew_MultiLevel() throws IOException {
        // 加载测试数据
        JsonNode sourceGroup = loadJsonFromFile("src/test/resources/dataGrid/多级表头(三级表头)_ocr.json");
        JsonNode templateNode = loadJsonFromFile("src/test/resources/dataGrid/多级表头(三级表头)_dsl_template.json");
        JsonNode expectedResult = loadJsonFromFile("src/test/resources/dataGrid/多级表头(三级表头)_dsl_result.json");

        // 执行转换
        JsonNode result = SchemaTransformerDataGrid.convertGroupDataGridStructureInDataGridNew(sourceGroup, templateNode);

        // 验证结果
        String layoutListString = null;
        String resultString = null;
        try {
            SchemaTransformerJsonUtil.removeAllNodeId(expectedResult);
            SchemaTransformerJsonUtil.removeAllNodeId(result);
            layoutListString = SchemaTransformerUtil.normalizeJson(expectedResult);
            resultString = SchemaTransformerUtil.normalizeJson(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals("多级表头转换结果不匹配",resultString,layoutListString );

      
    }

    @Test
    public void testConvertGroupDataGridStructureInDataGridNew_SingleLevel() throws IOException {
        // 加载测试数据
        JsonNode sourceGroup = loadJsonFromFile("src/test/resources/dataGrid/一级表头_ocr.json");
        JsonNode templateNode = loadJsonFromFile("src/test/resources/dataGrid/一级表头_dsl_template.json");
        JsonNode expectedResult = loadJsonFromFile("src/test/resources/dataGrid/一级表头_dsl_result.json");

        // 执行转换
        JsonNode result = SchemaTransformerDataGrid.convertGroupDataGridStructureInDataGridNew(sourceGroup, templateNode);

     

         // 验证结果
         String layoutListString = null;
         String resultString = null;
         try {

             SchemaTransformerJsonUtil.removeAllNodeId(expectedResult);
             SchemaTransformerJsonUtil.removeAllNodeId(result);

             layoutListString = SchemaTransformerUtil.normalizeJson(expectedResult);
        
             resultString = SchemaTransformerUtil.normalizeJson(result);
         } catch (Exception e) {
             e.printStackTrace();
         }
 
         Assert.assertEquals("单级表头转换结果不匹配",resultString,layoutListString );
    }
} 