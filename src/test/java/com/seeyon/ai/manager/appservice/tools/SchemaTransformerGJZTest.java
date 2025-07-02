package com.seeyon.ai.manager.appservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SchemaTransformerGJZTest {

    @Test
    public void convertLayoutByStreamTemplateTest() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取 UDC_SCHEMA
            File udcSchemaFile = new File("d:/gjz/udc_schema_stream.json");
            if (!udcSchemaFile.exists()) {
                System.out.println("文件不存在: " + udcSchemaFile.getAbsolutePath());
                return;
            }

            // 读取 OCR_SCHEMA_DEMO
            File ocrSchemaFile = new File("d:/gjz/ocr_schema_demo.json");
            if (!ocrSchemaFile.exists()) {
                System.out.println("文件不存在: " + ocrSchemaFile.getAbsolutePath());
                return;
            }

            // 读取文件内容为字符串
            String templateStr = mapper.readTree(udcSchemaFile).toString();
            String layoutSchemaStr = mapper.readTree(ocrSchemaFile).toString();

            // 创建参数对象
            SchemaTransformerParams params = new SchemaTransformerParams();
            params.setTemplate(templateStr);
            params.setLayoutSchema(layoutSchemaStr);



            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));

            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/gjz/result_stream.json");
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
//
//    @Test
//    public void convertLayoutByGridTemplateTest() {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            // 读取 UDC_SCHEMA
//            File udcSchemaFile = new File("d:/udc_schema_grid.json");
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
//
//            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplateStr(templateStr);
//            params.setLayoutSchemaStr(layoutSchemaStr);
//
//
//
//            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));
//
//            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/result_grid.json");
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
//
//
//    @Test
//    public void convertLayoutByLabelComponentTemplateTest() {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            // 读取 UDC_SCHEMA
//            File udcSchemaFile = new File("d:/udc_schema_label_component.json");
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
//
//            // 创建参数对象
//            SchemaTransformerParams params = new SchemaTransformerParams();
//            params.setTemplateStr(templateStr);
//            params.setLayoutSchemaStr(layoutSchemaStr);
//
//
//
//            JsonNode convertLayoutByTemplate = SchemaTransformer.convertLayoutByTemplate(mapper.readTree(layoutSchemaStr),mapper.readTree(templateStr));
//
//            // // 将更新后的结果重新写入文件
//            File outputFile = new File("d:/result_label_component.json");
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

}