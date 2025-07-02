package com.seeyon.ai.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.util.HttpRequestUtil;
import com.seeyon.boot.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@ConditionalOnProperty(name = "seeyon.apiKey.enabled", havingValue = "true") // 当配置为true时启用
@Slf4j
public class ApiKeyValidationAspect {
    @Around("execution(* com.seeyon.ai.ocrprocess.controller..*.*(..)) || execution(* com.seeyon.ai.schematransformer.controller..*.*(..))")
    public Object validateToken(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前HTTP请求
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String apiKey = request.getHeader("api-key");
        if (!isValidApiKey(apiKey)) {
            throw new PlatformException("apiKey不存在");
        }
        return joinPoint.proceed();
    }

    private boolean isValidApiKey(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return false;
        }
        if(!apiKeyIsExist(apiKey)){
            return false;
        }
        return true;
    }

    private boolean apiKeyIsExist(String apiKey) {
        String url = "https://ai.seeyonv8.com/ai-manager/service/info/api/prompt/apiKeyIsExist";
        Map<String, Object> params = new HashMap<>();
        params.put("input", "你好");
        params.put("promptVarMap", new HashMap<>());
        try {
            HttpEntity respEntity = HttpRequestUtil.httpPostRequest(url, params, apiKey);
            String res = EntityUtils.toString(respEntity);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(res);
            String code = jsonNode.get("code").asText();
            log.info("校验apiKey是否存在返回值：{}",res);
            if (code.equals("1804")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("校验apiKey是否存在请求异常：{}",e);
            return false;
        }
    }
}
