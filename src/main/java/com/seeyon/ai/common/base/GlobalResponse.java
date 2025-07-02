package com.seeyon.ai.common.base;


import com.seeyon.ai.common.exception.ErrorCode;
import com.seeyon.ai.common.exception.PlatformException;
import lombok.Data;

import javax.xml.bind.ValidationException;

@Data
public class GlobalResponse<T> {
    private String code = "0";
    private String message;
    private T data;

    public static <T> GlobalResponse from(T t) {
        GlobalResponse response = new GlobalResponse<>();
        response.setData(t);
        return response;
    }

    public static GlobalResponse buildResponse(String message) {
        GlobalResponse response = new GlobalResponse();
        response.setCode(ErrorCode.MANAGE_ERROR);
        response.setMessage(message);
        return response;
    }

    public static GlobalResponse buildResponse(Exception e) {
        GlobalResponse response = new GlobalResponse();
        if (e == null) {
            response.setCode(ErrorCode.MANAGE_ERROR);
            response.setMessage("返回内容为空");
        }
        if (e instanceof PlatformException) {
            response.setCode(((PlatformException) e).code());
            response.setMessage(((PlatformException) e).message());
        } else if (e instanceof ValidationException) {
            response.setCode(ErrorCode.MANAGE_ERROR);
            response.setMessage(e.getCause().getMessage());
        } else {
            response.setCode(ErrorCode.MANAGE_ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public static GlobalResponse buildEmptyResponse() {
        GlobalResponse response = new GlobalResponse();
        response.setMessage("");
        return response;
    }

}
