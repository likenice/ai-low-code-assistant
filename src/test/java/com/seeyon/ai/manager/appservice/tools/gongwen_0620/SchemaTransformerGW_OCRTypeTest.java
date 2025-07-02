package com.seeyon.ai.manager.appservice.tools.gongwen_0620;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.service.SchemaTransformerGW;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class SchemaTransformerGW_OCRTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    boolean isWrite2Disk = true;

    @Test
    public void testOcrLayoutType() throws IOException {
        // 读取complex类型
        String complex1 = "src/test/resources/gongwen_0620/ocr_type/complex1.json";
        JsonNode complex1Node = objectMapper.readTree(new File(complex1));
        // 读取simple类型
        String simple1 = "src/test/resources/gongwen_0620/ocr_type/simple1.json";
        JsonNode simple1Node = objectMapper.readTree(new File(simple1));

     LayoutTypeEnum simpleType = SchemaTransformerGW.getOcrLayoutType(simple1Node);
     LayoutTypeEnum complexType = SchemaTransformerGW.getOcrLayoutType(complex1Node);
        
        assertEquals("simpleType", simpleType, LayoutTypeEnum.SIMPLE);
        assertEquals("complexType", complexType, LayoutTypeEnum.COMPLEX);
    }


    
} 