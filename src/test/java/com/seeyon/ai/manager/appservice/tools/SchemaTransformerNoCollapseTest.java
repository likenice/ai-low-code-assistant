package com.seeyon.ai.manager.appservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
import org.junit.Test;

import java.io.IOException;

public class SchemaTransformerNoCollapseTest {

    @Test
    public void convertLayoutByDatagrid_gridTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerCollapse/nocollapse_datagrid_grid_udcSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();

            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/nocollapse_datagrid_grid_result.json");
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
    public void convertLayoutByNoDatagrid_gridTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取 UDC_SCHEMA

            String sourceGroupPath = "SchemaTransformerCollapse/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            String layoutSchemaStr =  mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerCollapse/nocollapse_nodatagrid_grid_udcSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            String templateStr =  mapper.readTree(templateNodeContent).toString();

            // 创建参数对象
            SchemaTransformerParams params = new SchemaTransformerParams();
            params.setTemplate(templateStr);
            params.setLayoutSchema(layoutSchemaStr);



            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/nocollapse_nodatagrid_grid_result.json");
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

}