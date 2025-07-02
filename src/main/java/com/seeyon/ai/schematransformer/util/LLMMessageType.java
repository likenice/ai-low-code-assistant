package com.seeyon.ai.schematransformer.util;

public enum LLMMessageType {
    // 定义枚举常量
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");
    // 私有字段存储枚举值
    private final String value;

    // 构造函数
    LLMMessageType(String value) {
        this.value = value;
    }

    // 获取枚举值的方法
    public String getValue() {
        return value;
    }

    public static void main(String[] args) {
        // 示例：获取枚举值
        System.out.println(LLMMessageType.SYSTEM.getValue()); // 输出: system
        System.out.println(LLMMessageType.USER.getValue());   // 输出: user
        System.out.println(LLMMessageType.USER.getValue());   // 输出: user
    }
}