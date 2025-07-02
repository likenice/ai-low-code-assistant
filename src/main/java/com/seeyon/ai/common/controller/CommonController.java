//package com.seeyon.ai.common.controller;
//
//import cn.hutool.json.JSONUtil;
//import com.fasterxml.jackson.core.JsonFactory;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//
//import com.seeyon.ai.common.base.BaseController;
//import com.seeyon.ai.common.base.GlobalResponse;
//import com.seeyon.ai.ocrprocess.controller.AiFormAssistantAsyncController;
//import com.seeyon.ai.ocrprocess.controller.AiFormAssistantController;
//import com.seeyon.ai.ocrprocess.domain.service.AiEdocService;
//import com.seeyon.ai.ocrprocess.domain.service.AiFormAssistantAsyncService;
//import com.seeyon.ai.ocrprocess.domain.service.AiFormAssistantService;
//import com.seeyon.ai.ocrprocess.dto.form.AiFormFLowInfo;
//import com.seeyon.ai.ocrprocess.dto.form.CallbackMultipartFileDto;
//import com.seeyon.ai.ocrprocess.dto.form.request.EdocIdentifyRequest;
//import com.seeyon.ai.ocrprocess.dto.form.request.IdentifyRequest;
//import com.seeyon.ai.ocrprocess.dto.form.request.InformationRecordRequest;
//import com.seeyon.ai.ocrprocess.dto.form.request.TransferRequest;
//import com.seeyon.ai.schematransformer.dto.SchemaTransformerParams;
//import com.seeyon.cip.connector.dto.callback.ConnectorCallbackMultipartFileDto;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.poi.ss.formula.functions.T;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.multipart.MultipartHttpServletRequest;
//import org.springframework.web.multipart.MultipartResolver;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import java.io.InputStream;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//
//@RestController
//@RequestMapping("/service/cip-connector/base")
//@Slf4j
//@Tag(name = "Schema转换接口")
//public class CommonController {
//
//    @Autowired
//    private MultipartResolver multipartResolver;
//
//    @Autowired
//    private AiFormAssistantService aiFormAssistantService;
//    @Autowired
//    private AiFormAssistantAsyncService aiFormAssistantAsyncService;
//    @Autowired
//    private AiEdocService aiEdocService;
//
//    SchemaTransformerOldController schemaTransformerOldController = new SchemaTransformerOldController();
//
//    /**
//     * 解析 request 获取 文件流
//     */
//    private Map<String, CallbackMultipartFileDto> getMultipartFileMap(HttpServletRequest request) {
//        try {
//            Map<String, CallbackMultipartFileDto> multipartFileMap = new HashMap<>();
//            MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);
//            Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
//            for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
//                String key = entry.getKey();
//                MultipartFile multipartFile = entry.getValue();
//                CallbackMultipartFileDto connectorCallbackMultipartFileDto = new CallbackMultipartFileDto();
//                connectorCallbackMultipartFileDto.setName(multipartFile.getName());
//                connectorCallbackMultipartFileDto.setOriginalFilename(multipartFile.getOriginalFilename());
//                connectorCallbackMultipartFileDto.setContentType(multipartFile.getContentType());
//                connectorCallbackMultipartFileDto.setSize(multipartFile.getSize());
//                connectorCallbackMultipartFileDto.setInputStream(multipartFile.getInputStream());
//                connectorCallbackMultipartFileDto.setResource(multipartFile.getResource());
//                connectorCallbackMultipartFileDto.setBytes(multipartFile.getBytes());
//                multipartFileMap.put(key, connectorCallbackMultipartFileDto);
//            }
//            return multipartFileMap;
//        } catch (Exception e) {
//            log.error("getBody error:{}", e.getMessage(), e);
//            return null;
//        }
//    }
//
//    @PostMapping("/callback") //?type=&channel=ocr2Dsl&action=convert&tid=-999
//    @Operation(summary = "")
//    public GlobalResponse callback(@RequestParam(name = "type", required = false, defaultValue = "") String type,
//                                   @RequestParam(name = "channel", required = true) String channel,
//                                   @RequestParam(name = "action", required = false, defaultValue = "") String action,
//                                   @RequestParam(name = "tid", required = true) String tid,
//                                   HttpServletRequest request, HttpServletResponse response
//    ) {
//
//        GlobalResponse<Object> globalResponse = new GlobalResponse();
//        // 解析body、文件、参数、header，增加大小限制
//        String contentType = request.getContentType();
//        JsonNode paramsObject = null;
//        Map<String, CallbackMultipartFileDto> multipartFileMap = Collections.emptyMap();
//        Map<String, InputStream> inputStreamMap = Collections.emptyMap();
//        if (contentType != null && contentType.startsWith("multipart")) {
//            multipartFileMap = getMultipartFileMap(request);
//        }
//        try {
//            if (contentType != null && contentType.contains("application/json")) {
//                paramsObject = new ObjectMapper().readTree(request.getReader().lines().collect(Collectors.joining()));
//            } else if (contentType != null && contentType.contains("multipart/form-data")) {
//                // 获取表单数据
//                Map<String, String[]> parameterMap = request.getParameterMap();
//                if (parameterMap != null && !parameterMap.isEmpty()) {
//                    paramsObject = new ObjectMapper().valueToTree(parameterMap);
//                }
//            }
//        } catch (Exception e) {
//            log.error("获取请求体失败:{}", e.getMessage(), e);
//        }
//
//        String apiKey = "";
//        //headers 是请求头数据
////
////        connectorCallbackRequestDto.getHeaders();
////        if (headers != null && headers.size() > 0) {
////            apiKey = String.valueOf(headers.get("api-key"));
////            if (apiKey != null) {
////                log.info("apiKey:" + apiKey.toString());
////            }
////        }
//
//
//        JsonNode jsonNode = null;
//        ObjectMapper objectMapper = new ObjectMapper()
//                .enable(SerializationFeature.INDENT_OUTPUT)
//                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
//        switch (action) {
//            case "convert_gw":
//                SchemaTransformerParams params = node2Transformser(paramsObject);
//                jsonNode = schemaTransformerOldController.convertLayoutByTemplateGW(params);
//                break;
//            case "convert":
//                SchemaTransformerParams params2 = node2Transformser(paramsObject);
//                jsonNode = schemaTransformerOldController.convertLayoutByTemplate(params2);
//                break;
//            case "sys_config":
//                Map result = sysConfig(paramsObject);
//                jsonNode = objectMapper.valueToTree(result);
//                break;
//            case "transfer":
//                try {
//                    TransferRequest transferRequest = objectMapper.readValue(String.valueOf(paramsObject), TransferRequest.class);
//                    globalResponse.setData(aiFormAssistantAsyncService.transfer(transferRequest));
//                    return globalResponse;
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            case "ocr_identify":
//                try {
//                    IdentifyRequest identifyRequest = objectMapper.readValue(String.valueOf(paramsObject), IdentifyRequest.class);
//                    globalResponse.setData(String.valueOf(aiFormAssistantAsyncService.ocrIdentify(identifyRequest)));
//                    return globalResponse;
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            case "edoc_identify":
//                try {
//                    EdocIdentifyRequest edocIdentifyRequest = objectMapper.readValue(String.valueOf(paramsObject), EdocIdentifyRequest.class);
//                    globalResponse.setData(String.valueOf(aiEdocService.edocIdentify(edocIdentifyRequest)));
//                    return globalResponse;
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            case "record":
//                try {
//                    InformationRecordRequest informationRecordRequest = objectMapper.readValue(String.valueOf(paramsObject), InformationRecordRequest.class);
//                    aiFormAssistantService.informationRecord(informationRecordRequest);
//                    return globalResponse;
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            case "getFlow":
//                globalResponse.setData(aiFormAssistantAsyncService.getFlow(paramsObject.get("id").asLong()));
//                return globalResponse;
//            case "upload":
//
//
//                if (multipartFileMap != null && multipartFileMap.size() > 0) {
//                    CallbackMultipartFileDto file = multipartFileMap.get("file");
//                    globalResponse.setData(aiFormAssistantService.uploadToCommon(file, apiKey));
//                    return globalResponse;
//                }
//                return globalResponse;
//            default:
//                throw new RuntimeException("action:" + action + " not find !");
//
//        }
//
//
//        return convert2GlobalResponse(jsonNode);
//    }
//
//    private SchemaTransformerParams node2Transformser(JsonNode params) {
//        SchemaTransformerParams result = new SchemaTransformerParams();
//        result.setLayoutSchema(params.get("layoutSchema").asText());
//        result.setTemplate(params.get("template").asText());
//        return result;
//    }
//
//    private Map sysConfig(JsonNode params) {
//        Map result = new HashMap<>();
//        result.put("enableUdc", true);
//        result.put("enableGongwen", true);
//
//        return result;
//    }
//
//    private GlobalResponse convert2GlobalResponse(JsonNode params) {
//        GlobalResponse result = new GlobalResponse();
//        result.setCode("0");
//        result.setData(params);
//        return result;
//    }
//
//}