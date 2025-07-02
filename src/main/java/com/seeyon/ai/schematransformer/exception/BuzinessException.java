package com.seeyon.ai.schematransformer.exception;

/**
 * 业务异常类
 */
public class BuzinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private String code;
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     */
    public BuzinessException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public BuzinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 异常
     */
    public BuzinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param cause 异常
     */
    public BuzinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 设置错误码
     * 
     * @param code 错误码
     */
    public void setCode(String code) {
        this.code = code;
    }
} 