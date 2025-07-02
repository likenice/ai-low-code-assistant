package com.seeyon.ai.schematransformer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * DSL转换工具类
 * 
 * @author AI Assistant
 */
public class DslTransformUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 需要遍历的字段列表
    private static final List<String> TRAVERSABLE_FIELDS = Arrays.asList(
        "children", "components", "settings", "dataSource", "gridCell", "container", "grid"
    );

    /**
     * 深度克隆Map对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepClone(Map<String, Object> source) {
        try {
            return objectMapper.readValue(
                objectMapper.writeValueAsString(source),
                Map.class
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deep clone failed", e);
        }
    }

    /**
     * 解析JSON字符串
     */
    @SuppressWarnings("unchecked")
    public static JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    /**
     * 遍历DSL树结构
     */
    @SuppressWarnings("unchecked")
    public static void loopChildren(ObjectNode data, ObjectNode parentData, int index,
                                    TriConsumer<ObjectNode, ObjectNode, Integer> callback) {
        if (data == null) return;

        // 处理当前节点

        callback.accept(data, parentData, index);

        // 处理子节点
        JsonNode childrenNode = data.get("children");
        if (childrenNode != null && childrenNode.isArray()) {
            ArrayNode children = (ArrayNode) childrenNode;
            for (JsonNode child : children) {
                if (child.isObject()) {

                    loopChildren((ObjectNode) child, data, 0, callback);
                }
            }
        }
    }

    /**
     * 深度优先搜索处理实体数据
     */
    @SuppressWarnings("unchecked")
    public static void dfs(Map<String, Object> entityData, ArrayNode entityLists) {
        if (entityData == null) return;
        
        JsonNode entityNode = objectMapper.valueToTree(entityData);
        entityLists.add(entityNode);
        
        List<Map<String, Object>> children = (List<Map<String, Object>>) entityData.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                dfs(child, entityLists);
            }
        }
    }

    /**
     * 三参数Consumer接口
     */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    /**
     * 首字母大写转换
     * 
     * @param str 输入字符串
     * @return 首字母大写的字符串
     */
    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 生成指定长度的随机字符串
     * 
     * @param length 字符串长度
     * @param type 生成类型
     * @return 随机字符串
     */
    public static String getRandomString(int length, String type) {
        return UUID.randomUUID().toString().substring(0, length);
    }

    /**
     * 将数值列表转换为总和为100的百分比列表
     * 
     * @param values 原始数值列表
     * @return 百分比列表
     */
    public static List<Object> scaleToSum100(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        
        double sum = 0;
        for (Object value : values) {
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
            }
        }
        
        List<Object> result = new ArrayList<>();
        if (sum > 0) {
            for (Object value : values) {
                if (value instanceof Number) {
                    double scaled = (((Number) value).doubleValue() / sum) * 100;
                    result.add(scaled);
                } else {
                    result.add(value);
                }
            }
        } else {
            result.addAll(values);
        }
        
        return result;
    }
} 