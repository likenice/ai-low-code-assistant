package com.seeyon.ai.manager.appservice.tools.dslAndTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.manager.appservice.tools.SchemaTransformerUtilTest;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * 处理模版中 没有分组 的场景
 */
public class SchemaTransformerNoCollapseTest {


    @Test
    public void convertDslSchemaByGrid() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_datagrid_grid_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
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
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=text 在模版中没有, 则获取第一个:test_aa
        Assert.assertEquals("金额", layoutList.get(2).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("test_aa", layoutList.get(2).get("children").get(0).get("sourceSchema").get("id").asText());

        //datagrid 中列 type=input 在模版中有, 则匹配上:input_bb
        Assert.assertEquals("input_bb", layoutList.get(2).get("children").get(2).get("sourceSchema").get("id").asText());


    }


    @Test
    public void convertDslSchemaByLableComponent() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_datagrid_label_component_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());

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

    /**
     * 对nodata_grid 中嵌套1层容器 (只转换后的分组和重复节数据)
     */
    @Test
    public void convertDslSchemaByNoDataGridGrid() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_nodatagrid_grid_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        //网格
        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        //验证第二组 和第一组是否在一个容器下.
        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(1).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());


    }

    /**
     * 对nodata_grid 中嵌套两层容器  (只转换后的分组和重复节数据)
     */
    @Test
    public void convertDslSchemaByNoDataGridGrid2Level() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_nodatagrid_grid_dslSchema2.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        //网格
        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        //验证第二组 和第一组是否在一个容器下.
        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(1).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());


    }

    /**
     * 对nodata_grid 中嵌套两层容器 1层grid
     */
    @Test
    public void convertDslSchemaByNoDataGridGrid3LevelAndGrid() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_nodatagrid_grid_dslSchema(2grid).json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        //网格
        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        //验证第二组 和第一组是否在一个容器下.
        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(1).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());


    }


    /**
     * 没有分组, 没有重复节,label+组件 (只转换后的分组和重复节数据)
     */
    @Test
    public void convertDslSchemaByNoDataGridLableComponent() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_nodatagrid_label_component_dslSchema.json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
        } catch (IOException e) {
            System.out.println("发生IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("发生未预期的错误: " + e.getMessage());
            e.printStackTrace();
        }


        SchemaTransformer.SchemaTransformResult convertLayoutByTemplate = null;
        try {
            convertLayoutByTemplate = SchemaTransformer.convertLayoutByDslTemplate(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ArrayNode layoutList = convertLayoutByTemplate.getLayoutList();

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertNull(layoutList.get(0).get("settings").get("title"));
        //网格

        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件

        Assert.assertEquals("label", layoutList.get(0).get("children").get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("content").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("label_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        Assert.assertEquals("inputNumber", layoutList.get(0).get("children").get(1).get("children").get(0).get("type").asText());
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(0).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());


    }

    /**
     * 没有分组, 没有重复节,嵌套grid  (全量数据)
     */
    @Test
    public void convertLayoutByNoDataGridGridFull() {
        String layoutSchemaStr = "";
        String templateStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String sourceGroupPath = "SchemaTransformerDemo/ocr_schema_demo.json";
            String sourceGroupContent = SchemaTransformerUtilTest.readResourcesFile(sourceGroupPath);
            layoutSchemaStr = mapper.readTree(sourceGroupContent).toString();

            String templateNodePath = "SchemaTransformerDemo/nocollapse_nodatagrid_grid_dslSchema(2grid).json";
            String templateNodeContent = SchemaTransformerUtilTest.readResourcesFile(templateNodePath);
            templateStr = mapper.readTree(templateNodeContent).toString();
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


        JsonNode convertLayoutByDsl = null;
        try {
            convertLayoutByDsl = SchemaTransformer.convertLayoutByDslSchema(mapper.readTree(layoutSchemaStr), (ObjectNode) mapper.readTree(templateStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        JsonNode layoutList = convertLayoutByDsl.get("children").get(0).get("children").get(0).get("children").get(1).get("children").get(0).get("children").get(1).get("children");

        //断言convertLayoutByTemplate的 children[0].settings.content = "标题名称"
        Assert.assertEquals(3, layoutList.size());
        Assert.assertEquals("分组1_1", layoutList.get(0).get("settings").get("title").asText());
        //网格
        Assert.assertEquals("gridCell", layoutList.get(0).get("children").get(0).get("type").asText());
        Assert.assertEquals("udcGridCell_9Lqw", layoutList.get(0).get("children").get(0).get("sourceSchema").get("id").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridRow").asText());
        Assert.assertEquals("1/2", layoutList.get(0).get("children").get(0).get("settings").get("styles").get("gridColumn").asText());

        //网格中的组件
        Assert.assertEquals("文本组件11", layoutList.get(0).get("children").get(0).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("wbzj_11", layoutList.get(0).get("children").get(0).get("children").get(0).get("dataSource").get("dataField").asText());
        Assert.assertEquals("inputNumber_XD11", layoutList.get(0).get("children").get(0).get("children").get(0).get("sourceSchema").get("id").asText());

        //验证第二组 和第一组是否在一个容器下.
        Assert.assertEquals("文本组件12", layoutList.get(0).get("children").get(1).get("children").get(0).get("settings").get("titleName").asText());
        Assert.assertEquals("input_XD11", layoutList.get(1).get("children").get(1).get("children").get(0).get("sourceSchema").get("id").asText());

        //convertLayoutByDsl 保存为d:/convertLayoutByDsl.json
        try {

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("d:/convertLayoutByDsl.json"), convertLayoutByDsl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

        }

    }


}