package com.seeyon.ai.manager.appservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.schematransformer.service.SchemaTransformer;
import com.seeyon.ai.schematransformer.util.SchemaTransformerJsonUtil;
import com.seeyon.ai.schematransformer.util.SchemaTransformerUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;


public class SchemaTransformerJsonUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCollectTypeAndSourceSchemaId() throws Exception {
        // 准备测试数据
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getResourceAsStream("/SchemaTransformerCollapse/templateNode.json");
        JsonNode node = mapper.readTree(inputStream);

        // 执行测试
        Map<String, String> result = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(node);

        // 验证结果
        Assert.assertEquals(6, result.size());
        Assert.assertEquals("udcContainer_D6j4", result.get("container"));
        Assert.assertEquals("udcForm_Y70u", result.get("form"));
        Assert.assertEquals("udcGrid_0Ufl", result.get("grid"));
        Assert.assertEquals("udcGridCell_mLaY", result.get("gridCell"));
        Assert.assertEquals("input_DDN3", result.get("input"));
        Assert.assertEquals("divider_GfFn", result.get("divider"));
    }

    @Test
    public void testCollectTypeAndSourceSchemaIdWithNullNode() {
        // 测试空节点场景
        Map<String, String> result = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testCollectTypeAndSourceSchemaIdWithEmptyNode() throws Exception {
        // 测试空对象场景
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyNode = mapper.createObjectNode();
        Map<String, String> result = SchemaTransformerJsonUtil.collectTypeAndSourceSchemaId(emptyNode);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testRemoveTabs() throws Exception {
        // 读取测试文件
        JsonNode schemaWithTabs = loadJsonFromResource("/schemaOption/schema_has_tabs.json");
        JsonNode expectedSchema = loadJsonFromResource("/schemaOption/schema_no_tabs.json");
        
        // 执行转换
        JsonNode result = SchemaTransformerJsonUtil.removeTabs(schemaWithTabs);
        
        // 验证结果
        // 1. 验证转换后的结果不应该包含type为tabs的节点
        Assert.assertFalse("转换后的结果不应该包含tabs节点", containsTabsNode(result));
        
        // 2. 验证转换后的结果与预期结果相匹配

        String expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedSchema);
        String actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        
        // System.out.println("预期结果：");
        // System.out.println(expectedJson);
        // System.out.println("实际结果：");
        // System.out.println(actualJson);
        
        Assert.assertEquals(
            "转换后的结果应该与预期结果匹配\n" +
            "预期结果：" + expectedJson + "\n" +
            "实际结果：" + actualJson,
            expectedJson,
            actualJson
        );
    }

    @Test
    public void testRemoveTabsWithNullInput() {
        assertNull("输入为null时应返回null", SchemaTransformerJsonUtil.removeTabs(null));
    }

    @Test
    public void testRemoveTabsWithNoTabs() throws Exception {
        // 读取不包含tabs的schema
        JsonNode schemaWithoutTabs = loadJsonFromResource("/schemaOption/schema_no_tabs.json");
        
        // 执行转换
        JsonNode result = SchemaTransformerJsonUtil.removeTabs(schemaWithoutTabs);
        
        String expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaWithoutTabs);
        String actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        // 验证结果与输入相同
        // assertEquals(
        //     objectMapper.writeValueAsString(schemaWithoutTabs),
        //     objectMapper.writeValueAsString(result),
        //     "不包含tabs的schema转换后应该保持不变"
        // );
        Assert.assertEquals(
            "转换后的结果应该与预期结果匹配\n",
            expectedJson,
            actualJson
        );
    }

    @Test
    public void testRemoveTabsWithArrayNode() throws Exception {
        // 创建一个包含数组的测试数据
        String jsonWithArray = "[{\"type\":\"tabs\",\"items\":[{\"type\":\"input\"}]}, {\"type\":\"form\"}]";
        JsonNode arrayNode = objectMapper.readTree(jsonWithArray);
        
        // 执行转换
        JsonNode result = SchemaTransformerJsonUtil.removeTabs(arrayNode);
        
        // 验证结果
        assertFalse("转换后的结果不应该包含tabs节点", containsTabsNode(result));
        assertEquals("应该保留非tabs节点", 
            "[{\"type\":\"form\"}]",
            objectMapper.writeValueAsString(result));
    }

    @Test
    public void testAddTabs() throws Exception {
        // 准备测试数据
        JsonNode sourceNode = loadJsonFromResource("/schemaAddTabs/schema_no_tabs1.json");
        JsonNode expectedNode = loadJsonFromResource("/schemaAddTabs/schema_no_tabs1_result.json");
        JsonNode tabsNode = loadJsonFromResource("/schemaAddTabs/tabsTemplate.json");
        
        // 转换为ArrayNode
        ArrayNode sourceNodes = (ArrayNode) sourceNode;
        List<JsonNode> tabsNodeList = new ArrayList<>();
        if (tabsNode.isArray()) {
            tabsNode.forEach(tabsNodeList::add);
        }
        
        // 执行测试
        SchemaTransformerJsonUtil.addTabs(sourceNodes, tabsNodeList);
        
        // 验证结果
        SchemaTransformerJsonUtil.removeAllNodeId(expectedNode);
        SchemaTransformerJsonUtil.removeAllNodeId(sourceNodes);

        String expectedJson = SchemaTransformerUtil.normalizeJson(expectedNode);
        String actualJson = SchemaTransformerUtil.normalizeJson(sourceNodes);
        
        Assert.assertEquals(
            "转换后的结果应该与预期结果匹配\n" +
            "预期结果：" + expectedJson + "\n" +
            "实际结果：" + actualJson,
            expectedJson,
            actualJson
        );
    }

    @Test
    public void testAddTabsWithNullInput() {
        // 测试空输入
        ArrayNode emptyArray = objectMapper.createArrayNode();
        SchemaTransformerJsonUtil.addTabs(null, new ArrayList<>());
        SchemaTransformerJsonUtil.addTabs(emptyArray, null);
        SchemaTransformerJsonUtil.addTabs(emptyArray, new ArrayList<>());
        
        // 验证空数组没有被修改
        Assert.assertEquals(0, emptyArray.size());
    }

    @Test
    public void testAddTabsWithEmptyTabsSetting() throws Exception {
        // 准备测试数据
        JsonNode sourceNode = loadJsonFromResource("/schemaAddTabs/schema_no_tabs1.json");
        ArrayNode sourceNodes = (ArrayNode) sourceNode.deepCopy();
        
        // 创建没有tabsSetting的tabs节点
        ObjectNode tabsNode = objectMapper.createObjectNode()
                .put("type", "tabs")
                .put("id", "tabs_id_11");
        
        List<JsonNode> tabsNodeList = Collections.singletonList(tabsNode);
        
        // 执行测试
        SchemaTransformerJsonUtil.addTabs(sourceNodes, tabsNodeList);

        SchemaTransformerJsonUtil.removeAllNodeId(sourceNode);
        SchemaTransformerJsonUtil.removeAllNodeId(sourceNodes);


        // 验证结果 - 应该保持原始内容不变
        Assert.assertEquals(

            SchemaTransformerUtil.normalizeJson(sourceNode),
            SchemaTransformerUtil.normalizeJson(sourceNodes)
        );
    }


    // 辅助方法：检查结果是否包含原始内容
    private boolean containsOriginalContent(JsonNode result, JsonNode original) {
        if (result == null || original == null) {
            return false;
        }
        
        // 检查是否包含原始节点中的关键内容
        String resultStr = result.toString();
        String originalStr = original.toString();
        
        // 检查一些关键属性是否存在
        return resultStr.contains("\"type\":\"grid\"") &&
               resultStr.contains("\"type\":\"gridCell\"") &&
               resultStr.contains("\"type\":\"input\"") &&
               resultStr.contains("\"type\":\"inputNumber\"");
    }

    @Test
    public void testNormalizeJsonWithSimpleJson() throws Exception {
        // 准备测试数据 - 使用简单的乱序JSON
        String inputJson = "{\"c\":\"1\",\"a\":\"a\",\"b\":\"3\",\"child\":[{\"c\":\"1\",\"a\":\"1\"}]}";
        String expectedJson = "{\"a\":\"a\",\"b\":\"3\",\"c\":\"1\",\"child\":[{\"a\":\"1\",\"c\":\"1\"}]}";

        // 将输入JSON转换为JsonNode
        JsonNode inputNode = objectMapper.readTree(inputJson);
        JsonNode expectedNode = objectMapper.readTree(expectedJson);


        SchemaTransformerJsonUtil.removeAllNodeId(inputNode);
        SchemaTransformerJsonUtil.removeAllNodeId(expectedNode);
        // 执行normalizeJson方法
        String normalizedJson = SchemaTransformerUtil.normalizeJson(inputNode);
        String normalizedExpectedJson = SchemaTransformerUtil.normalizeJson(expectedNode);

        // 验证结果
        Assert.assertEquals(
            "JSON数据经过标准化后应该按照属性名称排序",
            normalizedExpectedJson,
            normalizedJson
        );

        // 验证JSON字符串中没有空格和换行符
        Assert.assertFalse(
            "标准化后的JSON不应该包含换行符",
            normalizedJson.contains("\n")
        );
        Assert.assertFalse(
            "标准化后的JSON不应该包含空格",
            normalizedJson.contains(" ")
        );

        // 验证实际输出的内容
        Assert.assertEquals(
            "标准化后的JSON应该完全匹配预期格式",
            "{\"a\":\"a\",\"b\":\"3\",\"c\":\"1\",\"child\":[{\"a\":\"1\",\"c\":\"1\"}]}",
            normalizedJson
        );
    }

    @Test
    public void testGetTabsNode() throws Exception {
        // 准备测试数据
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 读取输入schema文件
        InputStream inputStream = getClass().getResourceAsStream("/schemaGetTabsNode/schema_has_tabs1.json");
        JsonNode inputSchema = objectMapper.readTree(inputStream);
        
        // 读取预期结果文件
        InputStream expectedStream = getClass().getResourceAsStream("/schemaGetTabsNode/tabs1_result.json");
        JsonNode expectedNode = objectMapper.readTree(expectedStream);

        // 执行测试
        List<JsonNode> result = SchemaTransformerJsonUtil.getTabsNode(inputSchema);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        
        JsonNode actualNode = result.get(0);
        
        // 验证关键字段
        assertEquals("tabs", actualNode.get("type").asText());
        assertEquals("tabs_11", actualNode.get("sourceSchema").get("id").asText());
        assertEquals("tabs_id_11", actualNode.get("id").asText());
        
        // 验证tabsSetting
        JsonNode tabsSettings = actualNode.get("settings").get("tabsSetting");
        assertEquals(2, tabsSettings.size());
        assertEquals("页签1", tabsSettings.get(0).get("name").asText());
        assertEquals("页签2", tabsSettings.get(1).get("name").asText());
    }

    @Test
    public void testGetTabsNodeWithNoTabs() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode input = objectMapper.createObjectNode();
        input.put("type", "page");
        
        List<JsonNode> result = SchemaTransformerJsonUtil.getTabsNode(input);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

   // 辅助方法：根据type查找节点
   private JsonNode findNodeByType(JsonNode node, String type) {
    if (node == null) {
        return null;
    }

    if (node.isObject()) {
        if (node.has("type") && type.equals(node.get("type").asText())) {
            return node;
        }
        
        // 检查所有字段
        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode result = findNodeByType(elements.next(), type);
            if (result != null) {
                return result;
            }
        }
    } else if (node.isArray()) {
        for (JsonNode element : node) {
            JsonNode result = findNodeByType(element, type);
            if (result != null) {
                return result;
            }
        }
    }
    
    return null;
}

private JsonNode loadJsonFromResource(String resourcePath) throws Exception {
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return objectMapper.readTree(is);
    }
}

private boolean containsTabsNode(JsonNode node) {
    if (node == null) {
        return false;
    }

    if (node.isObject()) {
        // 检查当前节点是否为tabs类型
        if (node.has("type") && "tabs".equals(node.get("type").asText())) {
            return true;
        }

        // 递归检查所有字段
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            if (containsTabsNode(fields.next().getValue())) {
                return true;
            }
        }
    } else if (node.isArray()) {
        // 递归检查数组中的所有元素
        for (JsonNode element : node) {
            if (containsTabsNode(element)) {
                return true;
            }
        }
    }

    return false;
}

@Test
public void testGetTabsArrayNodeByTemplateAndOcr() throws Exception {
    // 准备测试数据
    JsonNode ocrSchema = loadJsonFromResource("/tabsDemo/3.1ocr_schema.json");
    JsonNode dslTemplateSchema = loadJsonFromResource("/tabsDemo/3.1dslTemplate.json");
    JsonNode expectedResult = loadJsonFromResource("/tabsDemo/result/3.1tabs_arraynode.json");

    // 执行测试
    List<JsonNode> result = SchemaTransformerJsonUtil.getTabsArrayNodeByTemplateAndOcr(ocrSchema, dslTemplateSchema);

    // 验证结果
    assertNotNull("结果不应为空", result);
    assertEquals("应该包含2个tabs节点", 2, result.size());

    // 将List<JsonNode>转换为ArrayNode以便比较
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode resultArray = objectMapper.createArrayNode();
    result.forEach(resultArray::add);

    // 验证结果与预期JSON是否匹配
    SchemaTransformerJsonUtil.removeAllNodeId(expectedResult);
    SchemaTransformerJsonUtil.removeAllNodeId(resultArray);
    String expectedJson = SchemaTransformerUtil.normalizeJson(expectedResult);
    String actualJson = SchemaTransformerUtil.normalizeJson(resultArray);

    assertEquals(
        "转换后的结果应该与预期结果匹配\n" +
        "预期结果：" + expectedJson + "\n" +
        "实际结果：" + actualJson,
        expectedJson,
        actualJson
    );
}

@Test
public void testGetTabsArrayNodeByTemplateAndOcrWithNullInput() {
    // 测试空输入
    assertNull("输入为null时应返回null", 
        SchemaTransformerJsonUtil.getTabsArrayNodeByTemplateAndOcr(null, null));
}

@Test
public void testGetTabsArrayNodeByTemplateAndOcrWithNoTabs() throws Exception {
    // 创建不包含tabs的测试数据
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode ocrSchema = objectMapper.createObjectNode();
    ocrSchema.put("titleName", "");
    ocrSchema.set("groups", objectMapper.createArrayNode());
    
    JsonNode dslTemplateSchema = loadJsonFromResource("/tabsDemo/3.1dslTemplate.json");
    
    // 执行测试
    List<JsonNode> result = SchemaTransformerJsonUtil.getTabsArrayNodeByTemplateAndOcr(ocrSchema, dslTemplateSchema);
    
    // 验证结果
    assertNotNull("结果不应为null", result);
    assertEquals("不包含tabs时应返回空列表", 0, result.size());
}

@Test
public void testRemoveAllNodeId() throws Exception {
    // 准备测试数据 - 保留dataSource中的id，只删除根节点和children下节点的id
    String input = "{"
        + "\"children\":["
        + "  {"
        + "    \"children\":["
        + "      {"
        + "        \"dataSource\":{\"id\":\"cfb3-3\",\"otherField\":\"value\"},"
        + "        \"name\":\"\","
        + "        \"id\":\"reference_1\""
        + "      }"
        + "    ],"
        + "    \"dataSource\":{\"id\":\"\"},"
        + "    \"name\":\"\","
        + "    \"id\":\"group_1\""
        + "  }"
        + "],"
        + "\"dataSource\":{\"entityName\":\"表名称\",\"id\":\"ds-1\"},"
        + "\"name\":\"重复表名称\","
        + "\"id\":\"dataGrid_1\""
        + "}";

    String expected = "{"
        + "\"children\":["
        + "  {"
        + "    \"children\":["
        + "      {"
        + "        \"dataSource\":{\"id\":\"cfb3-3\",\"otherField\":\"value\"},"
        + "        \"name\":\"\""
        + "      }"
        + "    ],"
        + "    \"dataSource\":{\"id\":\"\"},"
        + "    \"name\":\"\""
        + "  }"
        + "],"
        + "\"dataSource\":{\"entityName\":\"表名称\",\"id\":\"ds-1\"},"
        + "\"name\":\"重复表名称\""
        + "}";
    
    JsonNode inputNode = objectMapper.readTree(input);
    JsonNode expectedNode = objectMapper.readTree(expected);
    
    // 执行测试
    SchemaTransformerJsonUtil.removeAllNodeId(inputNode);
    SchemaTransformerJsonUtil.removeAllNodeId(expectedNode);
    // 验证结果 - 使用 normalizeJson 进行比较
    String actualJson = SchemaTransformerUtil.normalizeJson(inputNode);
    String expectedJson = SchemaTransformerUtil.normalizeJson(expectedNode);
    
    assertEquals(
        "删除id属性后的结果应该与预期匹配\n" +
        "预期结果：" + expectedJson + "\n" +
        "实际结果：" + actualJson,
        expectedJson,
        actualJson
    );
}

    @Test
    public void testRemoveAllNodeId2() throws Exception {
        // 准备测试数据 - 保留dataSource中的id，只删除根节点和children下节点的id
        String input = "{"
                + "\"children\":["
                + "  {"
                + "    \"children\":["
                + "      {"
                + "        \"dataSource\":{\"id\":\"cfb3-3\",\"otherField\":\"value\"},"
                + "        \"name\":\"\","
                + "        \"id\":\"reference_1\""
                + "      }"
                + "    ],"
                + "    \"sourceSchema\":{\"id\":\"123\"},"
                + "    \"name\":\"\","
                + "    \"id\":\"group_1\""
                + "  }"
                + "],"
                + "\"dataSource\":{\"entityName\":\"表名称\",\"id\":\"ds-1\"},"
                + "\"name\":\"重复表名称\","
                + "\"id\":\"dataGrid_1\""
                + "}";

        String expected = "{"
                + "\"children\":["
                + "  {"
                + "    \"children\":["
                + "      {"
                + "        \"dataSource\":{\"id\":\"cfb3-3\",\"otherField\":\"value\"},"
                + "        \"name\":\"\""
                + "      }"
                + "    ],"
                + "    \"sourceSchema\":{\"id\":\"1234\"},"
                + "    \"name\":\"\""
                + "  }"
                + "],"
                + "\"dataSource\":{\"entityName\":\"表名称\",\"id\":\"ds-1\"},"
                + "\"name\":\"重复表名称\""
                + "}";

        JsonNode inputNode = objectMapper.readTree(input);
        JsonNode expectedNode = objectMapper.readTree(expected);

        // 执行测试
        SchemaTransformerJsonUtil.removeAllNodeId(inputNode);
        SchemaTransformerJsonUtil.removeAllNodeId(expectedNode);

        // 验证结果 - 使用 normalizeJson 进行比较
        String actualJson = SchemaTransformerUtil.normalizeJson(inputNode);
        String expectedJson = SchemaTransformerUtil.normalizeJson(expectedNode);

        assertNotEquals(
                "删除id属性后的结果应该与预期匹配\n" +
                        "预期结果：" + expectedJson + "\n" +
                        "实际结果：" + actualJson,
                expectedJson,
                actualJson
        );
    }

} 