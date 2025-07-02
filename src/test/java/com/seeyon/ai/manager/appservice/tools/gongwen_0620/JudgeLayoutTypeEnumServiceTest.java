package com.seeyon.ai.manager.appservice.tools.gongwen_0620;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.schematransformer.enums.LayoutTypeEnum;
import com.seeyon.ai.schematransformer.service.JudgeLayoutTypeEnumService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * JudgeLayoutTypeEnumService测试类
 */
public class JudgeLayoutTypeEnumServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testComplexLayout() throws Exception {
        // 测试COMPLEX布局 - 左右布局
        String complexJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"title\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"2/3\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"input\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"component\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode complexNode = objectMapper.readTree(complexJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(complexNode, "3759629784606892007");
        assertEquals("COMPLEX布局测试失败", LayoutTypeEnum.COMPLEX, result);
    }

    @Test
    public void testTwoCellUpDownLayout() throws Exception {
        // 测试TWO_CELL_UP_DOWN布局 - 上下布局(不同单元格)
        String twoCellUpDownJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"title\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"3/4\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"input\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"component\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode twoCellUpDownNode = objectMapper.readTree(twoCellUpDownJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(twoCellUpDownNode, "3759629784606892007");
        assertEquals("TWO_CELL_UP_DOWN布局测试失败", LayoutTypeEnum.TWO_CELL_UP_DOWN, result);
    }

    @Test
    public void testOneCellUpDownLayout() throws Exception {
        // 测试ONE_CELL_UP_DOWN布局 - 上下布局(同一单元格)
        String oneCellUpDownJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t\t    \"type\": \"container\",\n" +
                "\t\t\t\t\t\"children\": [\n" +
                "\t\t\t\t        {\n" +
                "\t\t\t\t            \"type\": \"label\"\n" +
                "\t\t\t\t        }\n" +
                "\t\t\t\t    ]\n" +
                "\t\t\t\t},\n" +
                "\t\t\t    {\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"component\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode oneCellUpDownNode = objectMapper.readTree(oneCellUpDownJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(oneCellUpDownNode, "3759629784606892007");
        assertEquals("ONE_CELL_UP_DOWN布局测试失败", LayoutTypeEnum.ONE_CELL_UP_DOWN, result);
    }

    @Test
    public void testSimpleLayout() throws Exception {
        // 测试SIMPLE布局 - 只有一个组件
        String simpleJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"132132133\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"普通label\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"component\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"2/3\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"component\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode simpleNode = objectMapper.readTree(simpleJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(simpleNode, "3759629784606892007");
        assertEquals("SIMPLE布局测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testNullInput() {
        // 测试空输入
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(null, "3759629784606892007");
        assertEquals("空输入测试失败", LayoutTypeEnum.SIMPLE, result);

        result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(objectMapper.createObjectNode(), null);
        assertEquals("空groupId测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testNonGridType() throws Exception {
        // 测试非grid类型
        String nonGridJson = "{\n" +
                "\t\"type\": \"container\",\n" +
                "\t\"children\": []\n" +
                "}";

        JsonNode nonGridNode = objectMapper.readTree(nonGridJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(nonGridNode, "3759629784606892007");
        assertEquals("非grid类型测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testGroupIdNotFound() throws Exception {
        // 测试groupId不存在的情况
        String complexJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"referGroup\": {\n" +
                "\t\t\t\t\t\t\"groupId\": \"3759629784606892007\",\n" +
                "\t\t\t\t\t\t\"groupName\": \"文件标题\",\n" +
                "\t\t\t\t\t\t\"groupType\": \"title\"\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode complexNode = objectMapper.readTree(complexJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumByGroupId(complexNode, "999999999");
        assertEquals("groupId不存在测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    // ========== 按照id判断布局类型的测试用例 ==========

    @Test
    public void testComplexLayoutById() throws Exception {
        // 测试COMPLEX布局 - 左右布局（按照id判断）
        String complexJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"id\": 111\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"2/3\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"input\",\n" +
                "\t\t\t\t\t\"id\": 123\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode complexNode = objectMapper.readTree(complexJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(complexNode, "123");
        assertEquals("COMPLEX布局（按id）测试失败", LayoutTypeEnum.COMPLEX, result);
    }

    @Test
    public void testTwoCellUpDownLayoutById() throws Exception {
        // 测试TWO_CELL_UP_DOWN布局 - 上下布局(不同单元格)（按照id判断）
        String twoCellUpDownJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"id\": 456\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"3/4\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"input\",\n" +
                "\t\t\t\t\t\"id\": 123\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode twoCellUpDownNode = objectMapper.readTree(twoCellUpDownJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(twoCellUpDownNode, "123");
        assertEquals("TWO_CELL_UP_DOWN布局（按id）测试失败", LayoutTypeEnum.TWO_CELL_UP_DOWN, result);
    }

    @Test
    public void testOneCellUpDownLayoutById() throws Exception {
        // 测试ONE_CELL_UP_DOWN布局 - 上下布局(同一单元格)（按照id判断）
        String oneCellUpDownJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t\t    \"type\": \"container\",\n" +
                "\t\t\t\t\t\"children\": [\n" +
                "\t\t\t\t        {\n" +
                "\t\t\t\t            \"type\": \"label\"\n" +
                "\t\t\t\t        }\n" +
                "\t\t\t\t    ]\n" +
                "\t\t\t\t},\n" +
                "\t\t\t    {\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"id\": 123\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode oneCellUpDownNode = objectMapper.readTree(oneCellUpDownJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(oneCellUpDownNode, "123");
        assertEquals("ONE_CELL_UP_DOWN布局（按id）测试失败", LayoutTypeEnum.ONE_CELL_UP_DOWN, result);
    }

    @Test
    public void testSimpleLayoutById() throws Exception {
        // 测试SIMPLE布局 - 只有一个组件（按照id判断）
        String simpleJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"id\": 123\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"2/3\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t        \"type\": \"input\",\n" +
                "\t\t\t\t\t\"id\": 345\n" +
                "\t\t\t    }\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode simpleNode = objectMapper.readTree(simpleJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(simpleNode, "123");
        assertEquals("SIMPLE布局（按id）测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testNullInputById() {
        // 测试空输入（按id判断）
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(null, "123");
        assertEquals("空输入测试失败（按id）", LayoutTypeEnum.SIMPLE, result);

        result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(objectMapper.createObjectNode(), null);
        assertEquals("空id测试失败", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testNonGridTypeById() throws Exception {
        // 测试非grid类型（按id判断）
        String nonGridJson = "{\n" +
                "\t\"type\": \"container\",\n" +
                "\t\"children\": []\n" +
                "}";

        JsonNode nonGridNode = objectMapper.readTree(nonGridJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(nonGridNode, "123");
        assertEquals("非grid类型测试失败（按id）", LayoutTypeEnum.SIMPLE, result);
    }

    @Test
    public void testIdNotFound() throws Exception {
        // 测试id不存在的情况
        String complexJson = "{\n" +
                "\t\"type\": \"grid\",\n" +
                "\t\"children\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"gridCell\",\n" +
                "\t\t\t\"settings\": {\n" +
                "\t\t\t\t\"styles\": {\n" +
                "\t\t\t\t\t\"gridColumn\": \"1/2\",\n" +
                "\t\t\t\t\t\"gridRow\": \"2/3\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\t\"children\": [\n" +
                "\t\t\t\t{\n" +
                "                    \"type\": \"label\",\n" +
                "\t\t\t\t\t\"id\": 111\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        JsonNode complexNode = objectMapper.readTree(complexJson);
        LayoutTypeEnum result = JudgeLayoutTypeEnumService.getLayoutTypeEnumById(complexNode, "999999");
        assertEquals("id不存在测试失败", LayoutTypeEnum.SIMPLE, result);
    }
} 