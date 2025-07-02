package com.seeyon.ai.common.exception;

import com.google.common.collect.Sets;
import io.micrometer.common.util.StringUtils;

import java.util.Set;

public class PlatformException extends RootException {

    private static final long serialVersionUID = 7842362738775991179L;

    private String code = ErrorCode.OTHER_ERROR;

    private Set<String> errors = Sets.newHashSet();

    private boolean hasErrorMessage = false;

    public PlatformException(String customMessage) {
        super(ErrorCode.OTHER_ERROR);
        addError(customMessage);
    }

    public PlatformException(String code, Throwable cause) {
        super(code, cause);
    }

    public PlatformException(Throwable cause) {
        super(ErrorCode.OTHER_ERROR, cause);
        this.errors.add(cause.getMessage());
    }

    public PlatformException(String code, String customMessage) {
        super(code);
        this.code = code;
        addError(customMessage);
    }

    public PlatformException(String code, String message, Throwable cause) {
        super(code, cause);
        this.code = code;
        addError(message);
    }

    public PlatformException addError(String customMessage) {
        if (StringUtils.isNotBlank(customMessage)) {
            this.errors.add(customMessage);
            hasErrorMessage = true;
        }
        return this;
    }

    @Override
    public boolean hasCustomErrorMessage() {
        return hasErrorMessage;
    }

    @Override
    public String getMessage() {
        String msg;
        if (errors.isEmpty()) {
            msg = super.getMessage();
        } else {
            StringBuilder sb = new StringBuilder(errors.size() * 15);
            for (String message : errors) {
                sb.append(message).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            msg = sb.toString();
        }
        return msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String customMessage) {
        addError(customMessage);
    }

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.getMessage();
    }

    public static void throwIt(String customMessage) {
        throw new PlatformException(customMessage);
    }

    public static void throwIt(String code, String customMessage) {
        throw new PlatformException(code, customMessage);
    }

    public static void throwIt(String message, Throwable cause) {
        PlatformException bootException = new PlatformException(ErrorCode.OTHER_ERROR, cause);
        bootException.setMessage(message);
        throw bootException;
    }

}
