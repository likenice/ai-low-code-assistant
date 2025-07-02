package com.seeyon.ai.common.base;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(assignableTypes = {BaseController.class})
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE+10)
public class GlobalHandler implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(Exception.class)
    public GlobalResponse<Object> handleException(Exception e) {
        if(!(e instanceof PlatformException)) {
            log.error(e.getMessage(), e);
        }
        return GlobalResponse.buildResponse(e);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object responseObject, MethodParameter methodParameter, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        //responseObject是否为null
        if (null == responseObject) {
            return GlobalResponse.buildEmptyResponse();
        }
        //responseObject是否是文件
        if (responseObject instanceof Resource) {
            return responseObject;
        }
        //OpenAI chat访问不改变返回结构
//        if (responseObject instanceof SeeyonOpenAiApi.ChatCompletion) {
//            return responseObject;
//        }
//
//        if (responseObject instanceof OpenAiApi.EmbeddingList) {
//            return responseObject;
//        }

        if (methodParameter.getMethod().getName().equals("models")) {
            return responseObject;
        }
        //该方法返回值类型是否是void
        if (methodParameter.getMethod().getReturnType().isAssignableFrom(Void.TYPE)) {
            return GlobalResponse.buildEmptyResponse();
        }
        //该方法返回值类型是否是GlobalResponseEntity。若是直接返回，无需再包装一层
        if (responseObject instanceof GlobalResponse) {
            return responseObject;
        }
        GlobalResponse globalResponse  = GlobalResponse.from(responseObject);
        if (responseObject instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(globalResponse);
            } catch (Exception e) {
                throw new RuntimeException("String 转换异常", e);
            }
        }
        return globalResponse ;
    }
}
