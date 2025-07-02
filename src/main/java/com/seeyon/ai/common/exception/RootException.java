package com.seeyon.ai.common.exception;

public class RootException extends RuntimeException {

    private static final long serialVersionUID = -2773006305094086615L;

    private String code = ErrorCode.OTHER_ERROR;

    private String[] params;

    public RootException() {
    }

    public RootException(String code) {
        super();
        this.code = code;
    }

    public RootException(String code, String... params) {
        super();
        this.code = code;
        this.params = params;
    }


    public RootException(String code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public String code() {
        return this.code;
    }

    public String message() {
        String msg = this.getMessage();
        return msg;
    }

    public Object[] getParams() {
        return params;
    }

    public boolean hasCustomErrorMessage() {
        return false;
    }
}