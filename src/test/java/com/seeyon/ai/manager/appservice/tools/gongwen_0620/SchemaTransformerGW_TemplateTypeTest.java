package com.seeyon.ai.manager.appservice.tools.gongwen_0620;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.service.SchemaTransformerBase;
import com.seeyon.ai.schematransformer.service.SchemaTransformerGW;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import com.seeyon.ai.schematransformer.dto.SchemaTransformGridTemplateDTO;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class SchemaTransformerGW_TemplateTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    boolean isWrite2Disk = true;

    @Test
    public void testTemplateLayoutType() throws IOException {
        // 读取complex类型
        String complex1 = "src/test/resources/gongwen_0620/template_type/complex1.json";
        JsonNode complex1Node = objectMapper.readTree(new File(complex1));
        // 读取simple类型
        String simple1 = "src/test/resources/gongwen_0620/template_type/simple1.json";
        JsonNode simple1Node = objectMapper.readTree(new File(simple1));

         //获取 所有渲染需要的样式
        ObjectNode simple1TemplateDslSchema = SchemaTransformer.convertUdc2Dsl(simple1Node);
        //生成结果以模版中、最大字体；如果没有最大字体，在顶部增加一个标准默认标题容器（程萌提供规范）；
        SchemaTransformerBase.initTitleNodeName(simple1TemplateDslSchema);
        SchemaTransformGridTemplateDTO simple1NodeDto = SchemaTransformerGW.initGridTemplateNode(simple1TemplateDslSchema, "主网格");
        // 主网格 layoutTypeEnum
        LayoutTypeEnum simple1Enum = SchemaTransformerGW.getTemplateLayoutType(simple1NodeDto);

        assertEquals("simpleType", simple1Enum, LayoutTypeEnum.SIMPLE);

         //获取 所有渲染需要的样式
         ObjectNode complex1TemplateDslSchema = SchemaTransformer.convertUdc2Dsl(complex1Node);
         //生成结果以模版中、最大字体；如果没有最大字体，在顶部增加一个标准默认标题容器（程萌提供规范）；
         SchemaTransformerBase.initTitleNodeName(complex1TemplateDslSchema);
        //获取 所有渲染需要的样式
        SchemaTransformGridTemplateDTO complex1NodeDto = SchemaTransformerGW.initGridTemplateNode(complex1TemplateDslSchema, "主网格");
        // 主网格 layoutTypeEnum
        LayoutTypeEnum complex1Enum = SchemaTransformerGW.getTemplateLayoutType(complex1NodeDto);

          
        assertEquals("complexType", complex1Enum, LayoutTypeEnum.COMPLEX);
    }




    
} 