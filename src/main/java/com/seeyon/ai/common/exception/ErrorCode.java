package com.seeyon.ai.common.exception;

/**
 * AI平台错误码定义，按照四位码进行定义，四位的含义如下：
 *   模块  错误一级分类  子类
 *    x        x       xx
 *  例如：1404 ->  管理功能-资源找不到
 *       2401 ->  模型-无权访问
 *       3503 ->  提示词-服务不可用
 *       9000 ->  其他-未知错误
 *
 *  模块定义
 *  --------------------
 *      1   管理功能
 *      2   大模型
 *      3   提示词
 *      4   Agent
 *      5   RAG
 *      6   助手
 *      7   工具
 *      8   基础设施
 *      9   其他
 *  --------------------
 */
public class ErrorCode {

    // 管理功能的错误码定义
    public static final String MANAGE_ERROR = "1000";
    public static final String MANAGE_NOT_SUPPORT = "1400";
    public static final String PAGE_SIZE_TOO_LARGE = "1410";
    public static final String MANAGE_DB_ERROR = "1700";
    public static final String API_KEY_NOT_FOUND = "1804";
    public static final String API_KEY_REQUIRE = "1805";
    public static final String API_KEY_IN_VALID = "1806";
    public static final String FILE_TOO_LARGE = "1900";

    // 模型相关的错误码定义
    public static final String MODEL_ERROR = "2000";
    public static final String MODEL_UN_AUTHORIZED = "2401";
    public static final String MODEL_IN_ACTIVE = "2403";
    public static final String MODEL_NOT_FOUND = "2404";


    // 提示词相关的错误码定义
    public static final String PROMPT_ERROR = "3000";
    public static final String PROMPT_UN_AVAILABLE = "3503";

    // Agent相关错误码定义
    public static final String AGENT_FILE_NOT_FOUND = "5404";
    public static final String AGENT_FILE_FILE_EXISTS = "5101";
    public static final String AGENT_FILE_ERROR = "5500";
    public static final String AGENT_COMMON_SERVICE_ERROR = "5102";

    // 向量库(RAG)相关的错误码定义
    public static final String REPOSITORY_ERROR = "5000";
    public static final String REPOSITORY_USED = "5200";
    public static final String REPOSITORY_IN_VALID = "5503";
    public static final String REPOSITORY_NOT_FOUND = "5404";
    public static final String REPOSITORY_EXISTS = "5100";
    public static final String REPOSITORY_FILE_EXISTS = "5101";
    public static final String REPOSITORY_FILE_NOT_EXISTS = "5102";
    public static final String REPOSITORY_CHUNK_NOT_EXISTS = "5103";
    public static final String REPOSITORY_KNOWLEDGE_SOURCE_TYPE_NOT_EXISTS = "5104";
    public static final String REPOSITORY_KNOWLEDGE_SOURCE_DEFAULT_MODAL_NOT_EXISTS = "5105";
    public static final String REPOSITORY_KNOWLEDGE_SOURCE_SAVE_RECORD_ERROR = "5106";
    public static final String REPOSITORY_KNOWLEDGE_SOURCE_ADD_QUEUE_ERROR = "5107";

    // 助手相关错误码定义
    public static final String ASSISTANT_GENERAL_ERROR = "5600";
    public static final String ASSISTANT_NOT_EXISTS = "5601";

    // 用户相关的错误码定义
    public static final String LOGIN_ERROR = "6000";
    public static final String AUTH_ERROR = "6001";

    // 工具相关的错误码定义
    public static final String TOOLS_ERROR = "7000";
    public static final String TOOLS_PARAMS_ERROR = "7400";
    public static final String TOOLS_UNKNOWN_ERROR = "7500";


    // 基础设施错误码定义
    public static final String BASIC_IO_NET = "8600";

    // 其他错误码定义
    public static final String OTHER_ERROR = "9000";

}
