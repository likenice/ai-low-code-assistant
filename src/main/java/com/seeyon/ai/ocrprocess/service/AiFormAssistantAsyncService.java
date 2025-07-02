package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.dao.UserUseFormInfoDao;
import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.enums.AiIdentifyTypeEnum;
import com.seeyon.ai.ocrprocess.enums.AiformFlowEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTaskStatusEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTypeEnum;
import com.seeyon.ai.ocrprocess.enums.UdcDataTypeEnum;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.form.AttributeDto;
import com.seeyon.ai.ocrprocess.form.AttributeGroupDto;
import com.seeyon.ai.ocrprocess.form.DataHistoryDto;
import com.seeyon.ai.ocrprocess.form.DataJsonDto;
import com.seeyon.ai.ocrprocess.form.EntityDto;
import com.seeyon.ai.ocrprocess.form.FieldDto;
import com.seeyon.ai.ocrprocess.form.FileDto;
import com.seeyon.ai.ocrprocess.form.LayoutDto;
import com.seeyon.ai.ocrprocess.form.RelationDto;
import com.seeyon.ai.ocrprocess.form.UploadRequestDto;
import com.seeyon.ai.ocrprocess.form.request.IdentifyRequest;
import com.seeyon.ai.ocrprocess.form.request.InformationRecordRequest;
import com.seeyon.ai.ocrprocess.form.request.OcrProcessRequest;
import com.seeyon.ai.ocrprocess.form.request.TransferRequest;
import com.seeyon.ai.ocrprocess.form.request.UdcFormGenerate;
import com.seeyon.ai.ocrprocess.form.response.AiFormFlowResponse;
import com.seeyon.ai.ocrprocess.form.response.DataHistoryResponse;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponse;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponseNew;
import com.seeyon.ai.ocrprocess.form.response.IdentifyResponse;
import com.seeyon.ai.ocrprocess.form.response.PageDslResponse;
import com.seeyon.ai.ocrprocess.util.CommonProperties;
import com.seeyon.ai.ocrprocess.util.FilterUtil;
import com.seeyon.ai.ocrprocess.util.StringSimilarity;
import com.seeyon.boot.context.RequestContext;
import com.seeyon.boot.domain.dao.Wrapper;
import com.seeyon.boot.util.id.Ids;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author pb
 */
@Service
@Slf4j
public class AiFormAssistantAsyncService {

    @Autowired
    private AiPromptSvcAppService aiPromptSvcAppService;
    @Autowired
    private MetaDataService metaDataService;
    //    @Value("${seeyon.ocr.path:http://10.101.129.4:8889}")
//    private String baseUrl;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private CommonProperties commonProperties;
    @Autowired
    private OcrProcessService ocrProcessService;
    @Autowired
    private OcrToPageService ocrToPageService;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserUseFormInfoDao userUseFormInfoDao;
    private static final Map<String, String> captionMap = new HashMap<>();

    static {
        for (UdcDataTypeEnum code : UdcDataTypeEnum.values()) {
            captionMap.put(code.code(), code.getCaption());
        }
    }

    public List<DataStandardResponse> transfer(TransferRequest transferRequest, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap, AiFormFLowInfo aiFormFLowInfo,String apiKey) {
        Map dataMap = new LinkedHashMap();
        Map layoutMap = new LinkedHashMap();
        String ocrJson = transferRequest.getOcrJson();
        LinkedHashMap ocrJsonMap = ocrProcessService.toMap(ocrJson);// JSONObject.parseObject(ocrJson, LinkedHashMap.class, Feature.OrderedField);
        String dslJson = JSONUtil.toJsonStr(ocrJsonMap.get("structure"));
        String layout = JSONUtil.toJsonStr(ocrJsonMap.get("layout"));
        dataMap = ocrProcessService.toMap(dslJson);//JSONObject.parseObject(dslJson, LinkedHashMap.class, Feature.OrderedField);
        layoutMap = ocrProcessService.toMap(layout);//JSONObject.parseObject(layout, LinkedHashMap.class, Feature.OrderedField);
        if (dataMap.isEmpty() || layoutMap.isEmpty()) {
            handleError(aiFormFLowInfo, "ocr identify data null", AiformFlowEnum.entityTransferError.code(), userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap,apiKey);
            throw new PlatformException("ocr identify data null");
        }
        List groupPositionList = toList(JSONUtil.toJsonStr(layoutMap.get("group")));  //JSONArray.parseObject(layoutMap.get("group").toString(), LinkedList.class, Feature.OrderedField);
        List rowPositionList = toList(JSONUtil.toJsonStr(layoutMap.get("sublist"))); //JSONArray.parseObject(layoutMap.get("sublist").toString(), LinkedList.class, Feature.OrderedField);
        List<Map> parentEntityList = new LinkedList<>();
        List<Map> subEntityList = new LinkedList<>();
        // DSL数据
        List<DataStandardResponse> dslList = new LinkedList<>();
        DataJsonDto entityInfo = ocrProcessService.getEntityInfo(dataMap);
        // json处理
        jsonFormat(entityInfo.getFieldsInfo(), parentEntityList, true, subEntityList);
        // 父实体信息
        String parentId = UUID.randomUUID().toString();
        DataStandardResponse parentEntityDsl = getEntityDsl(entityInfo.getTableName(), transferRequest.getFormType(), parentId,
                "0", parentEntityList, groupPositionList);
        dslList.add(parentEntityDsl);
        // 子实体信息
        if (!subEntityList.isEmpty() && !rowPositionList.isEmpty()) {
            for (int i = 0; i < subEntityList.size(); i++) {
                Map map = subEntityList.get(i);
                List rowPosition = new LinkedList<>();
                rowPosition.add(rowPositionList.get(i));
                DataJsonDto subEntityInfo = ocrProcessService.getEntityInfo(map);
                List<Map> entityList = new LinkedList<>();
                List<Map> errorEntityList = new LinkedList<>();
                jsonFormat(subEntityInfo.getFieldsInfo(), entityList, true, errorEntityList);
                if (!errorEntityList.isEmpty()) {
                    log.error("子实体中嵌入了子实体：{}", JSONUtil.toJsonStr(errorEntityList));
                }
                DataStandardResponse subEntityDsl = getEntityDsl(subEntityInfo.getTableName(), "standard",
                        UUID.randomUUID().toString(), parentId, entityList, rowPosition);
                dslList.add(subEntityDsl);
            }
        }
        log.info("json处理后DSL数据：{}", JSONUtil.toJsonStr(dslList));
        int fieldNum = 0;
        if (transferRequest.getType() == AiIdentifyTypeEnum.Img.getCode()) {
            fieldNum = fieldLayOutJudge(dslList);
        } else if (transferRequest.getType() == AiIdentifyTypeEnum.Excel.getCode()) {
            fieldNum = excelFieldLayOutJudge(dslList);
        } else {
            handleError(aiFormFLowInfo, AiformFlowEnum.entityTransferError.getCaption(), AiformFlowEnum.entityTransferError.code(), userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap,apiKey);
            throw new PlatformException(" error type");
        }
        userUseFormInfo.setGeneratedElementCount("{\"fields\":" + fieldNum + "}");
        fieldDateTypeJudge(dslList, transferRequest, userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap, aiFormFLowInfo);
        log.info("转换后数据:{}", JSONUtil.toJsonStr(dslList));
        return dslList;

    }


    public List toList(String str) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 使用LinkedList保持顺序，与原来使用Feature.OrderedField效果一致
            return objectMapper.readValue(str, new TypeReference<LinkedList<Object>>() {
            });
        } catch (Exception e) {
            log.error("json转换失败：{},json数据:{}", e.getMessage(), str);
            throw new PlatformException("json transfer error");
        }
    }


    public Long ocrIdentify(UdcFormGenerate ocrIdentifyRequest, HttpServletRequest request) {
        Integer type = ocrIdentifyRequest.getType();
        Long id = Ids.gidLong();
        ocrIdentifyRequest.setId(id);
        AiFormFLowInfo aiFormFLowInfo = new AiFormFLowInfo();
        aiFormFLowInfo.setId(id);
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrStart.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrStart.code());
        aiFormFLowInfo.setPageContextPath("");
        aiFormFLowInfo.setEntityContextPath("");
        aiFormFLowInfo.setOcrContextPath("");
        cacheService.saveToCache(String.valueOf(id), JSONUtil.toJsonStr(aiFormFLowInfo));
        if (type == AiIdentifyTypeEnum.Img.getCode()) {
            executorService.execute(() -> {
                ocrIdentifyImpl(ocrIdentifyRequest,request.getHeader("api-key"));
            });
        } else if (type == AiIdentifyTypeEnum.Excel.getCode()) {
            executorService.execute(() -> {
                excelIdentifyImpl(ocrIdentifyRequest,request.getHeader("api-key"));
            });
        } else {
            throw new PlatformException(" no know type");
        }
        return ocrIdentifyRequest.getId();
    }

    public void ocrIdentifyImpl(UdcFormGenerate udcFormGenerate,String apiKey) {
        UserUseFormInfo userUseFormInfo = new UserUseFormInfo();
        userUseFormInfo.setTaskId(udcFormGenerate.getId());
        userUseFormInfo.setUserId(RequestContext.get().getUserId());
        userUseFormInfo.setAssistantType(AssistantTypeEnum.UDC);
        userUseFormInfo.setCreationTime(new Date());
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.CANCEL);
        userUseFormInfo.setImagePath((udcFormGenerate.getPath()));
        InputStream inputStreamToImage = fileService.download(udcFormGenerate.getPath());
        BufferedImage image = null;
        FileDto fileDto = fileService.selectOrigFileByStorageKey(udcFormGenerate.getPath());
        try {
            image = ImageIO.read(inputStreamToImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        String fileName = fileDto.getFileName();
        StringBuilder inputImageInfo = new StringBuilder();
        inputImageInfo.append("{\"fileName\":");
        inputImageInfo.append("\"" + fileName + "\"");
        inputImageInfo.append(",\"width\":");
        inputImageInfo.append(width);
        inputImageInfo.append(",\"height\":");
        inputImageInfo.append(height + "}");
        userUseFormInfo.setImageInfo(inputImageInfo.toString());
        IdentifyRequest ocrIdentifyRequest = new IdentifyRequest();
        ocrIdentifyRequest.setPath(udcFormGenerate.getPath());
        ocrIdentifyRequest.setType(udcFormGenerate.getType());
        ocrIdentifyRequest.setEntityInfo(udcFormGenerate.getEntityInfo());
        ocrIdentifyRequest.setId(udcFormGenerate.getId());
        // 使用静态ObjectMapper实例
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        String fromCache = cacheService.getFromCache(String.valueOf(ocrIdentifyRequest.getId()));
        AiFormFLowInfo aiFormFLowInfo;
        try {
            // 替换JSONObject.parseObject
            aiFormFLowInfo = objectMapper.readValue(fromCache, AiFormFLowInfo.class);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }
//        String ocrUrl ="";
//        if(true){
//            ocrUrl = baseUrl+"/udc/recognize";
//        }else {
//            ocrUrl = baseUrl+"/ai-manager/form/udc/recognize";
//        }
        String ocrUrl = "";
        String deployType = appProperties.getDeployType();
        String aiManagerAddress = appProperties.getAiManagerAddress();
        String aiManagerApiKey = appProperties.getAiManagerApiKey();
        if ("public".equalsIgnoreCase(deployType)) {
            ocrUrl = aiManagerAddress + "/ai-manager/form/udc/recognize";
        } else {
            ocrUrl = appProperties.getOcrUrl() + "/udc/recognize";
        }
        String path = ocrIdentifyRequest.getPath();
        log.info("ocr 识别开始:{}", ocrUrl);
        long startTime = System.currentTimeMillis();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(ocrUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            try {
                InputStream inputStream = fileService.download(path);
                ContentBody inputStreamBody = new InputStreamBody(
                        inputStream,
                        ContentType.APPLICATION_OCTET_STREAM,
                        fileDto.getFileName()
                );
                builder.addPart("files", inputStreamBody);
                httpPost.setEntity(builder.build());
                if ("public".equalsIgnoreCase(deployType)) {
                    httpPost.addHeader("api-key", aiManagerApiKey);
                }
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseString = EntityUtils.toString(response.getEntity());
                    long endTime = System.currentTimeMillis();
                    userUseFormInfo.setDurationOcr((int) (endTime - startTime));
                    // 替换JSONObject.parseObject
                    JsonNode jsonNode = objectMapper.readTree(responseString);

                    if (!"1".equals(jsonNode.get("status").asText())) {
                        String message = jsonNode.get("message") == null ? "ocr请求失败" : jsonNode.get("message").asText();
                        handleError(aiFormFLowInfo, message, 2000, userUseFormInfo, null, null,apiKey);
                    } else {
                        processOcrResponse(jsonNode, aiFormFLowInfo, path, ocrIdentifyRequest, udcFormGenerate, userUseFormInfo,apiKey);
                    }
                }
            } catch (IOException e) {
                handleServerError(aiFormFLowInfo, e, ocrIdentifyRequest.getId(), userUseFormInfo, null, null,apiKey);
            }
        } catch (IOException e) {
            handleServerError(aiFormFLowInfo, e, ocrIdentifyRequest.getId(), userUseFormInfo, null, null,apiKey);
            log.error("HTTP客户端创建失败", e);
            throw new PlatformException("HTTP客户端创建失败", e);
        }
    }

    // 辅助方法 - 处理OCR错误
    private void handleError(AiFormFLowInfo aiFormFLowInfo, String message, int code, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap,String apiKey) {
        aiFormFLowInfo.setHandleStage(message);
        aiFormFLowInfo.setHandleStageType(code);
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo, aiFormFLowInfo.getId());
        if (llmInitialAnalysisMap != null) {
            userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
        }
        if (llmConfidenceMap != null) {
            userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
        }
        userUseFormInfo.setErrorMessageStorageKey(writeTxt(message,apiKey));
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.ERROR);
        userUseFormInfo.setEndTime(new Date());
        userUseFormInfoDao.create(userUseFormInfo);
        log.info("ocr identify error:{}", message);
    }

    // 辅助方法 - 处理OCR服务器错误
    private void handleServerError(AiFormFLowInfo aiFormFLowInfo, Exception e, Long requestId, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap,String apiKey) {
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrServerError.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrServerError.code());
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo, requestId);
        if (llmInitialAnalysisMap != null) {
            userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
        }
        if (llmConfidenceMap != null) {
            userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
        }
        userUseFormInfo.setErrorMessageStorageKey(writeTxt(JSONUtil.toJsonStr(e),apiKey));
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.ERROR);
        userUseFormInfo.setEndTime(new Date());
        userUseFormInfoDao.create(userUseFormInfo);
        log.error("ocr识别失败：{}", e);
        throw new PlatformException("ocr identify error", e);
    }

    // 辅助方法 - 处理OCR响应
    private void processOcrResponse(JsonNode jsonNode, AiFormFLowInfo aiFormFLowInfo, String path, IdentifyRequest request, UdcFormGenerate udcFormGenerate, UserUseFormInfo userUseFormInfo,String apiKey) {
        ArrayNode resultArray = (ArrayNode) jsonNode.get("result");
        if (resultArray.size() == 0) {
            handleOcrFieldNull(aiFormFLowInfo, request.getId(), userUseFormInfo,apiKey);
        } else {
            String ocrJson = resultArray.get(0).toString();
            if ("{}".equals(ocrJson)) {
                handleOcrFieldNull(aiFormFLowInfo, request.getId(), userUseFormInfo,apiKey);
            } else {
                processValidOcrResult(ocrJson, aiFormFLowInfo, path, request, udcFormGenerate,userUseFormInfo,apiKey);
            }
        }
    }

    // 辅助方法 - 处理OCR字段为空的情况
    private void handleOcrFieldNull(AiFormFLowInfo aiFormFLowInfo, Long requestId, UserUseFormInfo userUseFormInfo,String apiKey) {
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrFieldNull.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrFieldNull.code());
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo, requestId);
        userUseFormInfo.setErrorMessageStorageKey(writeTxt("ocr 未识别出字段信息",apiKey));
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.ERROR);
        userUseFormInfo.setEndTime(new Date());
        userUseFormInfoDao.create(userUseFormInfo);
        log.error("ocr 未识别出字段信息，请检查上传的图片并重试");
    }

    // 辅助方法 - 处理有效的OCR结果
    private void processValidOcrResult(String ocrJson, AiFormFLowInfo aiFormFLowInfo, String path, IdentifyRequest request, UdcFormGenerate udcFormGenerate, UserUseFormInfo userUseFormInfo,String apiKey) {

        Map<String, Object> llmInitialAnalysisMap = new HashMap<>();
        Map<String, Object> llmConfidenceMap = new HashMap<>();
        try {
            // 替换JSONObject.parseObject
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            Map map = objectMapper.readValue(ocrJson, new TypeReference<Map>() {
            });
            OcrProcessRequest ocrProcessRequest = new OcrProcessRequest();
            ocrProcessRequest.setOcr(JSONUtil.toJsonStr(map.get("ocr")));
            ocrProcessRequest.setTsr(JSONUtil.toJsonStr(map.get("tsr")));
            userUseFormInfo.setOcrResult(writeTxt(JSONUtil.toJsonStr(map),apiKey));
            ocrProcessRequest.setAiFormFLowInfo(aiFormFLowInfo);
            String ocrSchema = ocrProcessService.process(ocrProcessRequest, userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap,apiKey);
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.entityDslCreate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.entityDslCreate.code());
            aiFormFLowInfo.setOcrContextPath("");
            saveToCache(aiFormFLowInfo, request.getId());
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setType(udcFormGenerate.getType());
            transferRequest.setPath(udcFormGenerate.getPath());
            transferRequest.setOcrJson(ocrSchema);
            transferRequest.setFormType(udcFormGenerate.getFormType());
            transferRequest.setSelectCtpEnumInfo(udcFormGenerate.getSelectCtpEnumInfo());
            transferRequest.setAssociationEntityInfo(udcFormGenerate.getAssociationEntityInfo());
            transferRequest.setAppName(udcFormGenerate.getAppName());
            DataStandardResponseNew transfer = pageTransfer(transferRequest, userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap, aiFormFLowInfo,apiKey);
            String storage = writeTxt(JSONUtil.toJsonStr(transfer),apiKey);
            userUseFormInfo.setFinishMessageStorageKey(storage);
            if (llmInitialAnalysisMap != null) {
                userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
            }
            if (llmConfidenceMap != null) {
                userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
            }
            userUseFormInfoDao.create(userUseFormInfo);
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.pageDslTransfer.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.pageDslTransfer.code());
            aiFormFLowInfo.setOcrContextPath(storage);
            saveToCache(aiFormFLowInfo, request.getId());

        } catch (Exception e) {
            handleServerError(aiFormFLowInfo, e, request.getId(), userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap,apiKey);
        }
    }


    // 辅助方法 - 保存到缓存
    private void saveToCache(AiFormFLowInfo aiFormFLowInfo, Long requestId) {
        try {
            // 替换JSONUtil.toJsonStr
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            cacheService.saveToCache(String.valueOf(requestId), objectMapper.writeValueAsString(aiFormFLowInfo));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AiFormFLowInfo", e);
        }
    }


    public void excelIdentifyImpl(UdcFormGenerate udcFormGenerate,String apiKey) {
        UserUseFormInfo userUseFormInfo = new UserUseFormInfo();
        userUseFormInfo.setTaskId(udcFormGenerate.getId());
        userUseFormInfo.setUserId(RequestContext.get().getUserId());
        userUseFormInfo.setAssistantType(AssistantTypeEnum.UDC);
        userUseFormInfo.setCreationTime(new Date());
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.CANCEL);
        userUseFormInfo.setImagePath((udcFormGenerate.getPath()));
        Map llmInitialAnalysisMap = new HashMap();
        Map llmConfidenceMap = new HashMap();
        IdentifyRequest identifyRequest = new IdentifyRequest();
        identifyRequest.setPath(udcFormGenerate.getPath());
        identifyRequest.setType(udcFormGenerate.getType());
        identifyRequest.setEntityInfo(udcFormGenerate.getEntityInfo());
        identifyRequest.setId(udcFormGenerate.getId());
        // 使用静态ObjectMapper实例
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        IdentifyResponse transferResponse = new IdentifyResponse();
        String path = identifyRequest.getPath();
        String fromCache = cacheService.getFromCache(String.valueOf(identifyRequest.getId()));

        AiFormFLowInfo aiFormFLowInfo;
        try {
            // 替换JSONObject.parseObject
            aiFormFLowInfo = objectMapper.readValue(fromCache, AiFormFLowInfo.class);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }

        try {
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.tableStructureGenerate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.tableStructureGenerate.code());

            // 替换JSONUtil.toJsonStr
            cacheService.saveToCache(String.valueOf(identifyRequest.getId()),
                    objectMapper.writeValueAsString(aiFormFLowInfo));

            List<List<Map<List, String>>> list = excelAnalyze(path);
            AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
            aiPromptSvcCallDto.setPromptCode("tableStructureGenerate");

            // 替换JSONObject.toJSONString
            aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(list));
            aiPromptSvcCallDto.setPromptVarMap(new HashMap<>());

            String dataTypeAssistantResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap))
                    .replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");

            String response = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            int num = 0;
            while (num < 5) {
                try {
                    LinkedHashMap<String, Object> tableStructureMap = objectMapper.readValue(response, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
                    break;
                } catch (Exception e) {
                    response = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                    num++;
                }
            }
            log.info("提示词结构生成返回值：{}", response);
            Map dataMap = ocrProcessService.toMap(dataTypeAssistantResponse);
            DataJsonDto entityInfo = ocrProcessService.getEntityInfo(dataMap);
            // json嵌套json情况抹平
            Map<String, Object> noNestJson = new LinkedHashMap<>();
            ocrProcessService.jsonNestHandle(entityInfo.getFieldsInfo(), noNestJson, true);

            Map newDateMap = new LinkedHashMap();
            // 连续未分组
            ocrProcessService.continuouslyUngroupedHandle(noNestJson, false, newDateMap);

            LayoutDto layoutDto = new LayoutDto();
            Map<String, Object> entityLayout = new LinkedHashMap<>();
            entityLayout.put("name", entityInfo.getTableName());
            entityLayout.put("position", getPosition(entityInfo.getTableName(), list));
            layoutDto.setEntity(entityLayout);

            layoutHandle(newDateMap, layoutDto, list);

            // 构建结果JSON
            ObjectNode jsonObject = objectMapper.createObjectNode();
            Map<String, Object> structureMap = new LinkedHashMap<>();
            structureMap.put(entityInfo.getTableName(), newDateMap);

            // 替换JSONObject.parseObject
            jsonObject.set("structure", objectMapper.valueToTree(structureMap));
            jsonObject.set("layout", objectMapper.valueToTree(layoutDto));
            String ocrSchema = jsonObject.toString();
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.entityDslCreate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.entityDslCreate.code());
            aiFormFLowInfo.setOcrContextPath("");
            saveToCache(aiFormFLowInfo, identifyRequest.getId());
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setType(udcFormGenerate.getType());
            transferRequest.setPath(udcFormGenerate.getPath());
            transferRequest.setOcrJson(ocrSchema);
            transferRequest.setFormType(udcFormGenerate.getFormType());
            transferRequest.setSelectCtpEnumInfo(udcFormGenerate.getSelectCtpEnumInfo());
            transferRequest.setAssociationEntityInfo(udcFormGenerate.getAssociationEntityInfo());
            transferRequest.setAppName(udcFormGenerate.getAppName());
            DataStandardResponseNew transfer = pageTransfer(transferRequest, userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap, aiFormFLowInfo,apiKey);
            String storage = writeTxt(JSONUtil.toJsonStr(transfer),apiKey);
            if (llmInitialAnalysisMap != null) {
                userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
            }
            if (llmConfidenceMap != null) {
                userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
            }
            userUseFormInfo.setFinishMessageStorageKey(storage);
            userUseFormInfoDao.create(userUseFormInfo);
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.pageDslTransfer.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.pageDslTransfer.code());
            aiFormFLowInfo.setOcrContextPath(storage);

        } catch (Exception e) {
            handleServerError(aiFormFLowInfo, e, aiFormFLowInfo.getId(), userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap,apiKey);
            log.error("excel识别失败：{}", e);
        }
    }

    private List getPosition(String name, List<List<Map<List, String>>> list) {
        List positionList = new ArrayList();
        boolean b = false;
        for (List<Map<List, String>> maps : list) {
            for (Map<List, String> map : maps) {
                Iterator<List> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    List next = iterator.next();
                    String val = map.get(next);
                    if (Double.compare(StringSimilarity.similarity(name, val), 0.7) == 1) {
                        positionList = next;
                        maps.remove(map);
                        b = true;
                        break;
                    }
                }
                if (b) {
                    break;
                }
            }
            if (b) {
                break;
            }
        }
        if (!b) {
            positionList.add(0);
            positionList.add(0);
            positionList.add(0);
            positionList.add(0);
        }
        return positionList;
    }


    private void layoutHandle(Map dataMap, LayoutDto layoutDto, List<List<Map<List, String>>> list) {
        Map def = new LinkedHashMap();
        Iterator iteratored = dataMap.keySet().iterator();
        List<Map<String, Object>> group = new LinkedList<>();
        List<Map<String, Object>> sublist = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        while (iteratored.hasNext()) {
            Map<String, Object> groupMap = new LinkedHashMap<>();
            Map<String, Object> sublistMap = new LinkedHashMap<>();
            List<List<Integer>> valuePositionList = new LinkedList<>();
            Map map = new LinkedHashMap();
            String key = String.valueOf(iteratored.next());
            Object field = dataMap.get(key);
            try {
                if (field instanceof JsonNode) { // 替换为 Jackson 的 JsonNode
                    JsonNode jsonNode = (JsonNode) field;
                    if (jsonNode.isObject()) { // 判断是否为 JSON 对象
                        groupMap.put("name", key);
                        groupMap.put("position", getPosition(key, list));
                        Iterator<String> iterator = jsonNode.fieldNames();
                        while (iterator.hasNext()) {
                            String next = iterator.next();
                            String val = jsonNode.get(next).asText(); // 获取 JSON 对象的值
                            valuePositionList.add(getPosition(next, list));
                            valuePositionList.add(getPosition(val, list));
                        }
                        groupMap.put("value", valuePositionList);
                    } else if (jsonNode.isArray()) { // 判断是否为 JSON 数组
                        sublistMap.put("name", key);
                        sublistMap.put("position", getPosition(key, list));
                        JsonNode jsonArray = jsonNode;
                        if (jsonArray.size() > 0) {
                            for (JsonNode o : jsonArray) {
                                Iterator<String> iterator = o.fieldNames();
                                while (iterator.hasNext()) {
                                    String next = iterator.next();
                                    String val = o.get(next).asText(); // 获取 JSON 对象的值
                                    valuePositionList.add(getPosition(next, list));
                                    valuePositionList.add(getPosition(val, list));
                                }
                            }
                        }
                        sublistMap.put("value", valuePositionList);
                    }
                }
                if (!groupMap.isEmpty()) {
                    group.add(groupMap);
                }
                if (!sublistMap.isEmpty()) {
                    sublist.add(sublistMap);
                }
            } catch (Exception e) {
                e.printStackTrace(); // 打印异常信息，方便调试
            }
        }
        layoutDto.setSublist(sublist);
        layoutDto.setGroup(group);
    }

// 其他方法保持不变


    private List<List<Map<List, String>>> excelAnalyze(String filePath) {
        List<List<Map<List, String>>> list = new ArrayList<>();
        try (InputStream inputStream = fileService.download(filePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个Sheet
            int y = 1;
            for (Row row : sheet) { // 迭代行
                List<Map<List, String>> rowList = new ArrayList<>();
                if (row.getRowNum() != 0) {
                    y = y + 17;
                }
                int x1 = 0;
                int x2 = 0;
                String noNullVal = "";
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);// 迭代列
                    if (cell == null) {
                        continue;
                    }
                    String val = getCellValue(cell);
                    // 获取单元格数据，并根据单元格类型做相应处理
                    x2++;
                    if (!val.equals("")) {
                        noNullVal = val;
                    }
                    if (!noNullVal.equals("")) {
                        String nextVal = "";
                        if (i + 1 < row.getLastCellNum()) {
                            Cell nextCell = row.getCell(i + 1);
                            if (nextCell != null) {
                                nextVal = getCellValue(nextCell);
                            }
                        }
                        if (!nextVal.equals("") || i == row.getLastCellNum() - 1) {
                            Map<List, String> map = new LinkedHashMap<>();
                            List<Integer> layoutList = new ArrayList<>(4);
                            layoutList.add(x1);
                            layoutList.add(y);
                            layoutList.add(x2);
                            layoutList.add(y);
                            map.put(layoutList, noNullVal);
                            rowList.add(map);
                            noNullVal = "";
                            x1 = x2;
                        }
                    }

                }
                if (rowList.size() > 0) {
                    list.add(rowList);
                }
            }
            System.out.println(JSONUtil.toJsonStr(list));
        } catch (IOException e) {
            log.info("excel解析失败：{}", e);
            throw new PlatformException("excel Analyze error");
        }
        return list;
    }

    private static String getCellValue(Cell cell) {
        String val = "";
        switch (cell.getCellType()) {
            case STRING:
                val = cell.getStringCellValue();
                break;
            case NUMERIC:
                val = String.valueOf(cell.getNumericCellValue());
                break;
            case BOOLEAN:
                val = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                val = String.valueOf(cell.getCellFormula());
                break;
            case BLANK:
                val = cell.getStringCellValue();
            default:
                break;
        }
        return val;
    }


    private void fieldDateTypeJudge(List<DataStandardResponse> dslList, TransferRequest transferRequest, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap, AiFormFLowInfo aiFormFLowInfo) {
        Map<String, RelationDto> entityMap = metaDataService.getRelationInfo(transferRequest.getAppName(), UdcDataTypeEnum.ENTITY.code(), transferRequest.getAssociationEntityInfo(), transferRequest.getSelectCtpEnumInfo());
        Map<String, RelationDto> ctpEnumMap = metaDataService.getRelationInfo(transferRequest.getAppName(), UdcDataTypeEnum.CTPENUM.code(), transferRequest.getAssociationEntityInfo(), transferRequest.getSelectCtpEnumInfo());
        for (DataStandardResponse dataStandardResponse : dslList) {
            List<FieldDto> operateEntity = dataStandardResponse.getOperateEntity();
            for (FieldDto fieldDto : operateEntity) {
                List<AttributeDto> attributeDtoList = fieldDto.getAttributeDtoList();
                List<AttributeDto> collect = attributeDtoList.stream().filter(attributeDto -> {
                    if (!attributeDto.isBelievable()) {
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                Map<String, Object> prompt = new HashMap<>();
                int promptLength = collect.size() > 5 ? 5 : collect.size();
                int frequency = (int) Math.ceil(collect.size() / (double) promptLength);
                AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
                for (int index = 0; index < frequency; index++) {
                    int shengyuchangdu = collect.size() - index * promptLength > 5 ? 5 : collect.size() - index * promptLength;
                    List<AttributeDto> attributeDtoList1 = collect.subList(index * promptLength, index * promptLength + shengyuchangdu);
                    aiPromptSvcCallDto.setPromptCode("fieldDataTypeAssistant");
                    aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(attributeDtoList1));
                    aiPromptSvcCallDto.setPromptVarMap(new HashMap<>());
                    String dataTypeAssistantResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                    prompt.putAll(ocrProcessService.toMap(dataTypeAssistantResponse));
                }
                log.info("ai字段预测信息：{}", JSONUtil.toJsonStr(prompt));
                Map<String, Map<String, Object>> relationInfo = getRelationInfo(prompt, entityMap, ctpEnumMap, llmInitialAnalysisMap, llmConfidenceMap);
                log.info("ai字段匹配信息：{}", JSONUtil.toJsonStr(relationInfo));
                for (AttributeDto attributeDto : attributeDtoList) {
                    if (attributeDto.getColumns() == -1) {
                        attributeDto.setColumns(fieldDto.getColumns());
                    }
                    Map<String, Object> dataTypeMap = (Map<String, Object>) prompt.get(attributeDto.getCaption());
                    if (dataTypeMap == null) {
                        continue;
                    }
                    String dataType = String.valueOf(dataTypeMap.get("value"));
                    if (dataType != null) {
                        attributeDto.setDataType(dataType);
                        String des = "";
                        double dataTypeConfidence = Double.parseDouble(String.valueOf(dataTypeMap.get("confidence")));
                        if ((Double.compare(dataTypeConfidence, 0.8) == -1 && Double.compare(dataTypeConfidence, 0.3) == 1)) {
                            // 一般错误
                            attributeDto.setLevel(1);
                            des = des + "匹配字段类型:" + captionMap.get(dataType) + ",置信度：" + dataTypeConfidence + ";";

                        } else if (Double.compare(dataTypeConfidence, 0.4) == -1 && Double.compare(dataTypeConfidence, 0) == 1) {
                            des = des + "匹配字段类型:" + captionMap.get(dataType) + ",置信度：" + dataTypeConfidence + ";";
                            //  严重错误
                            attributeDto.setLevel(2);
                        }
                        if (des.equals("")) {
                            dataTypeConfidence = 1.0;
                        }
                        Map<String, Object> map = relationInfo.get(attributeDto.getCaption());
                        if (map != null) {
                            List<Map<String, Object>> desList = (List<Map<String, Object>>) map.get("des");
                            double maxConfidence = 0.0;
                            for (Map<String, Object> stringObjectMap : desList) {
                                String matchName = String.valueOf(stringObjectMap.get("matchName"));
                                double confidence = Double.parseDouble(String.valueOf(stringObjectMap.get("confidence")));
                                if (Double.compare(maxConfidence, confidence) == -1) {
                                    maxConfidence = confidence;
                                }
                                des = des + "关联信息：" + matchName + ",置信度：" + confidence + ";";
                            }

                            if ((Double.compare(maxConfidence, 0.8) == -1 && Double.compare(maxConfidence, 0.3) == 1) || desList.size() > 1) {
                                // 一般错误
                                attributeDto.setLevel(1);
                            } else if (Double.compare(maxConfidence, 0.4) == -1 && Double.compare(maxConfidence, 0) == 1) {
                                //  严重错误
                                attributeDto.setLevel(2);
                            }
                            if (Double.compare(maxConfidence, 0.8) == 1 && Double.compare(dataTypeConfidence, 0.8) == 1) {
                                des = "";
                            }
                            RelationDto relationDto = (RelationDto) map.get("relationDto");
                            attributeDto.setRelationType(relationDto.getRelationType());
                            attributeDto.setRelationCode(relationDto.getRelationCode());
                            attributeDto.setRelationApp(relationDto.getRelationApp());
                            attributeDto.setRelationEntity(relationDto.getRelationEntity());
                            attributeDto.setRelationEntityName(relationDto.getRelationEntityName());
                        }
                        String relationEntity = attributeDto.getRelationEntity();
                        if (attributeDto.getDataType().equals(UdcDataTypeEnum.CTPENUM.code())) {
                            if (relationEntity.equals("")) {
                                RelationDto relationDto = ctpEnumMap.get(ctpEnumMap.keySet().iterator().next());
                                attributeDto.setRelationType(relationDto.getRelationType());
                                attributeDto.setRelationCode(relationDto.getRelationCode());
                                attributeDto.setRelationApp(relationDto.getRelationApp());
                                attributeDto.setRelationEntity(relationDto.getRelationEntity());
                                attributeDto.setRelationEntityName(relationDto.getRelationEntityName());
                                des = "未匹配到相应的枚举信息，随机匹配，请自行修改匹配信息";
                                attributeDto.setLevel(2);
                            }
                        } else if (attributeDto.getDataType().equals(UdcDataTypeEnum.ENTITY.code())) {
                            if (relationEntity.equals("")) {
                                RelationDto relationDto = entityMap.get(entityMap.keySet().iterator().next());
                                attributeDto.setRelationType(relationDto.getRelationType());
                                attributeDto.setRelationCode(relationDto.getRelationCode());
                                attributeDto.setRelationApp(relationDto.getRelationApp());
                                attributeDto.setRelationEntity(relationDto.getRelationEntity());
                                attributeDto.setRelationEntityCategory(relationDto.getRelationEntityCategory());
                                attributeDto.setRelationEntityName(relationDto.getRelationEntityName());
                                attributeDto.setRelationStarter(relationDto.getRelationStarter());
                                des = "未匹配到相应的实体信息，随机匹配，请自行修改匹配信息";
                                attributeDto.setLevel(2);

                            }
                        }

                        attributeDto.setInformation(des);
                    }
                }
            }
        }
    }

    private int excelFieldLayOutJudge(List<DataStandardResponse> dslList) {
        int fieldNum = 0;
        for (DataStandardResponse dataStandardResponse : dslList) {
            List<FieldDto> operateEntity = dataStandardResponse.getOperateEntity();
            for (FieldDto fieldDto : operateEntity) {
                fieldDto.setColumns(fieldDto.getContWidth());
                List<AttributeDto> attributeDtoList = fieldDto.getAttributeDtoList();
                for (int num = 0; num < attributeDtoList.size(); num++) {
                    fieldNum++;
                    attributeDtoList.get(num).setColumns(attributeDtoList.get(num).getItemWidth());
                }
            }
        }
        return fieldNum;
    }

    private int fieldLayOutJudge(List<DataStandardResponse> dslList) {
        int filedNum = 0;
        for (DataStandardResponse dataStandardResponse : dslList) {
            List<FieldDto> operateEntity = dataStandardResponse.getOperateEntity();
            for (FieldDto fieldDto : operateEntity) {
                int finalColumn = 0;
                int finalMaxColumn = 0;
                int contWidth = fieldDto.getContWidth() == 0 ? 1 : fieldDto.getContWidth();
                int column = fieldDto.getColumns() == 0 ? 1 : fieldDto.getColumns();
                int length = contWidth / column;
                List<AttributeDto> attributeDtoList = fieldDto.getAttributeDtoList();
                for (int num = 0; num < attributeDtoList.size(); num++) {
                    filedNum++;
                    int itemWidth = attributeDtoList.get(num).getItemWidth();
                    Integer y = attributeDtoList.get(num).getY();
                    double proportion = itemWidth / (double) length;
                    int fieldColumn = Math.round(proportion) == 0 ? 1 : (int) Math.round(proportion);
                    finalColumn = num == 0 ? fieldColumn : finalColumn;
                    if (num == 0) {
                        if (num < attributeDtoList.size() - 1) {
                            Integer nextY = attributeDtoList.get(num + 1).getY();
                            if (Math.abs(nextY - y) > 15) {
                                fieldColumn = -1;
                            }
                        }
                    } else {
                        Integer theLastY = attributeDtoList.get(num - 1).getY();
                        if (Math.abs(y - theLastY) > 15) {
                            finalMaxColumn = Math.max(finalMaxColumn, finalColumn);
                            finalColumn = fieldColumn;
                        } else {
                            finalColumn += fieldColumn;
                            if (num == attributeDtoList.size() - 1) {
                                finalMaxColumn = Math.max(finalMaxColumn, finalColumn);
                            }
                        }
                        if (num == attributeDtoList.size() - 1) {
                            if (Math.abs(theLastY - y) > 15) {
                                fieldColumn = -1;
                            }
                        }
                        if (fieldColumn == 1 && fieldColumn != column && num < attributeDtoList.size() - 1) {
                            Integer nextY = attributeDtoList.get(num + 1).getY();
                            if (Math.abs(nextY - y) > 15 && Math.abs(theLastY - y) > 15) {
                                fieldColumn = -1;
                            }
                        }
                    }
                    // 判断是否单独一行
                    attributeDtoList.get(num).setColumns(fieldColumn);
                }
                fieldDto.setColumns(finalMaxColumn);
            }
        }
        return filedNum;
    }

    private Map<String, Map<String, Object>> getRelationInfo(Map<String, Object> prompt, Map<String, RelationDto> entityMap, Map<String, RelationDto> ctpEnumMap, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        Iterator<String> iterator = prompt.keySet().iterator();
        List<String> entityTypeList = new ArrayList<>();
        List<String> ctpEnumTypeList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            Map<String, Object> dataTypeMap = (Map<String, Object>) prompt.get(fieldName);
            String dataType = String.valueOf(dataTypeMap.get("value"));
            if (dataType.equals(UdcDataTypeEnum.ENTITY.code())) {
                entityTypeList.add(fieldName);
            } else if (dataType.equals(UdcDataTypeEnum.CTPENUM.code())) {
                ctpEnumTypeList.add(fieldName);
            }
        }

        Map<String, String> fieldMatchAssistant = new HashMap<>();
        AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
        Map<String, Object> requestMap = new HashMap<>();

        if (!entityTypeList.isEmpty()) {
            requestMap.put("matched", entityTypeList.toString());
            requestMap.put("matching", entityMap.keySet().toString());
            aiPromptSvcCallDto.setPromptCode("fieldMatchAssistant");
            aiPromptSvcCallDto.setInput("开始");
            aiPromptSvcCallDto.setPromptVarMap(requestMap);
            String fieldMatchAssistantResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            log.info("实体匹配返回结果:{}", fieldMatchAssistantResponse);
            try {
                fieldMatchAssistant.putAll(ocrProcessService.toMap(fieldMatchAssistantResponse));
            } catch (Exception e) {
                log.error("解析实体匹配返回结果失败", e);
            }
        }

        if (!ctpEnumTypeList.isEmpty()) {
            requestMap.put("matched", ctpEnumTypeList.toString());
            requestMap.put("matching", ctpEnumMap.keySet().toString());
            aiPromptSvcCallDto.setPromptCode("fieldMatchAssistant");
            aiPromptSvcCallDto.setInput("开始");
            aiPromptSvcCallDto.setPromptVarMap(requestMap);
            String fieldMatchAssistantResponse = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto, llmInitialAnalysisMap, llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            log.info("枚举匹配返回结果:{}", fieldMatchAssistantResponse);
            try {
                fieldMatchAssistant.putAll(ocrProcessService.toMap(fieldMatchAssistantResponse));
            } catch (Exception e) {
                log.error("解析枚举匹配返回结果失败", e);
            }
        }

        Iterator<String> iterator1 = fieldMatchAssistant.keySet().iterator();
        while (iterator1.hasNext()) {
            String fieldName = iterator1.next();
            String value = JSONUtil.toJsonStr(fieldMatchAssistant.get(fieldName));
            List<Map<String, Object>> desList = new ArrayList<>();
            String finalMatchName = "";
            double maxConfidence = 0.0;
            try {
                JsonNode jsonArray = objectMapper.readTree(value);
                for (JsonNode o : jsonArray) {
                    Map<String, Object> desMap = new HashMap<>();
                    String matchName = o.get("value").asText();
                    double confidence = o.get("confidence").asDouble();
                    if (finalMatchName.equals("")) {
                        maxConfidence = confidence;
                        finalMatchName = matchName;
                    } else {
                        int compare = Double.compare(maxConfidence, confidence);
                        if (compare == -1) {
                            finalMatchName = matchName;
                        }
                    }
                    desMap.put("matchName", matchName);
                    desMap.put("confidence", confidence);
                    desList.add(desMap);
                }
            } catch (Exception e) {
                log.error("解析匹配结果失败: {}", value, e);
                continue;
            }

            RelationDto relationDto = new RelationDto();
            if (entityMap.get(finalMatchName) != null) {
                relationDto = entityMap.get(finalMatchName);
            } else if (ctpEnumMap.get(finalMatchName) != null) {
                relationDto = ctpEnumMap.get(finalMatchName);
            }

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("des", desList);
            valueMap.put("relationDto", relationDto);
            map.put(fieldName, valueMap);
        }

        return map;
    }

    /**
     * 获取dsl
     *
     * @param tableName  表名
     * @param stereoType 表类型
     * @param id
     * @param parentId
     * @param list       数据list
     * @param layout     坐标list 分组size>1 子表 size=1
     * @return
     */
    private DataStandardResponse getEntityDsl(String tableName, String stereoType, String id, String
            parentId, List<Map> list, List<Map> layout) {
        // 获取相近数据
        Map<String, Integer> keyMap = new HashMap<>();
        if (parentId.equals("0")) {
            initKeyMap(keyMap, tableName, stereoType);
        } else {
            keyMap.put("创建时间", 0);
            keyMap.put("更新时间", 0);
            keyMap.put("ID", 0);
            keyMap.put("排序号", 0);
            keyMap.put("更新人", 0);
            keyMap.put("创建人", 0);
        }
        List<FieldDto> fields = new ArrayList<>();
        DataStandardResponse data = new DataStandardResponse();
        for (int i = 0; i < list.size(); i++) {
            boolean b = checkLength(i, layout, list, null);
            if (!b) {
                continue;
            }
            FieldDto fieldsInfo = getFieldsInfo(list.get(i), layout.get(i), keyMap);
            if (fieldsInfo.getCaption() == null) {
                continue;
            }
            fields.add(fieldsInfo);
        }
        data.setCreateEntity(EntityDto.convert(tableName, stereoType, parentId, id));
        data.setOperateEntity(fields);
        return data;
    }

    private void initKeyMap(Map<String, Integer> keyMap, String tableName, String stereoType) {
        keyMap.put("创建时间", 0);
        keyMap.put("更新时间", 0);
        keyMap.put("ID", 0);
        keyMap.put("排序号", 0);
        keyMap.put("更新人", 0);
        keyMap.put("创建人", 0);
        if (stereoType.equals("normal") || stereoType.equals("bill")) {
            keyMap.put("更新人", 0);
            keyMap.put("创建人", 0);
            keyMap.put("创建者", 0);
            keyMap.put("创建人机构", 0);
            keyMap.put("创建人部门", 0);
            keyMap.put("创建人岗位", 0);
            keyMap.put("创建者机构", 0);
            keyMap.put("创建者部门", 0);
            keyMap.put("创建者岗位", 0);
            keyMap.put("扣减规则信息", 0);
        } else if (stereoType.equals("form")) {
            keyMap.put("创建人机构", 0);
            keyMap.put("创建人部门", 0);
            keyMap.put("创建人岗位", 0);
        }
        keyMap.put(tableName + "发送状态", 0);
        keyMap.put(tableName + "状态", 0);
        keyMap.put("发起人", 0);
        keyMap.put("发起时间", 0);
        keyMap.put("发起机构", 0);
        keyMap.put("发起部门", 0);
        keyMap.put("发起岗位", 0);
        keyMap.put("流程模板", 0);
        keyMap.put("流程实例", 0);
        keyMap.put("流程标题", 0);
        keyMap.put("终止人", 0);
        keyMap.put("终止时间", 0);
        keyMap.put("终止原因", 0);
        keyMap.put("终止时单据状态", 0);
        keyMap.put("取消终止人", 0);
        keyMap.put("取消终止时间", 0);
        keyMap.put("取消终止原因", 0);
        keyMap.put("作废人", 0);
        keyMap.put("作废时间", 0);
        keyMap.put("作废时单据状态", 0);
        keyMap.put("取消作废人", 0);
        keyMap.put("取消作废时间", 0);
        keyMap.put("取消作废原因", 0);
        keyMap.put("作废原因", 0);
        keyMap.put("挂起人", 0);
        keyMap.put("挂起时间", 0);
        keyMap.put("挂起原因", 0);
        keyMap.put("挂起时单据状态", 0);
        keyMap.put("取消挂起人", 0);
        keyMap.put("取消挂起时间", 0);
        keyMap.put("取消挂起原因", 0);
        keyMap.put("单据编号", 0);
        keyMap.put("申请人", 0);
        keyMap.put("单据日期", 0);

    }


    /**
     * json处理
     *
     * @param dataMap
     * @param parentList
     * @param c
     * @param subList
     */
    private void jsonFormat(Map dataMap, List<Map> parentList, boolean c, List<Map> subList) {
        boolean b = true;
        Map defMap = new LinkedHashMap();
        Map def = new LinkedHashMap();
        defMap.put("未命名组" + (parentList.size() + subList.size()), def);
        Iterator iteratored = dataMap.keySet().iterator();
        while (iteratored.hasNext()) {
            Map map = new LinkedHashMap();
            String key = String.valueOf(iteratored.next());
            if (key.equals("logo组")) {
                continue;
            }
            Object field = dataMap.get(key);
            try {
                if (field instanceof Map) {  // 替换JSONObject判断
                    map.put(key, field);
                    parentList.add(map);
                    jsonFormat((Map) field, parentList, false, subList);
                } else if (field instanceof List) {  // 替换JSONArray判断
                    List jsonArray = (List) field;
                    if (jsonArray.size() > 0) {
                        Object subField = jsonArray.get(0);
                        if (subField instanceof Map) {  // 替换JSONObject判断
                            map.put(key, subField);
                            subList.add(map);
                        } else {
                            if (c) {
                                def.put(key, field);
                                if (b) {
                                    parentList.add(defMap);
                                    b = false;
                                }
                            }
                        }
                    }
                } else {
                    if (c) {
                        def.put(key, field);
                        if (b) {
                            parentList.add(defMap);
                            b = false;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("类型处理异常：{},字段：{},字段值：{}", e.getMessage(), key, field);
            }
        }
    }

    /**
     * @param y            纵坐标
     * @param keyPosition  坐标数组
     * @param contWidth    当前宽度
     * @param maxColumn    最大比例
     * @param maxContWidth 最大宽度
     * @param column       当前比例
     * @return
     */
    private Map<String, Integer> getNumber(int y, List<Integer> keyPosition, int contWidth, int maxColumn,
                                           int maxContWidth, int column) {
        Map<String, Integer> map = new HashMap<>();
        if (y == 0) {
            y = keyPosition.get(1);
            column++;
        } else {
            // 15误差 是否同行
            if (Math.abs(y - keyPosition.get(1)) <= 15) {
                column++;
                y = keyPosition.get(1);
            } else {
                y = keyPosition.get(1);
                maxColumn = Integer.max(maxColumn, column);
                maxContWidth = Integer.max(maxContWidth, contWidth);
                column = 1;
                contWidth = 0;
            }
        }
        int keyWidth = keyPosition.get(2) - keyPosition.get(0);
        map.put("y", y);
        map.put("column", column);
        map.put("maxColumn", maxColumn);
        map.put("maxContWidth", maxContWidth);
        map.put("contWidth", contWidth);
        map.put("keyWidth", keyWidth);
        return map;
    }

    /**
     * @param i            数组下表
     * @param positionList 坐标数组
     * @param size         枚举个数
     * @param y            纵坐标
     * @return
     */
    private int getValueWidth(int i, List<List<Integer>> positionList, int size, int y) {
        int valueWidth = 0;
        List<Integer> enumValueStartPosition = positionList.get(i);
        i = i + size - 1;
        List<Integer> enumValueEndPosition = positionList.get(i);
        i++;
        // 最后一条数据
        if (i >= positionList.size()) {
            valueWidth = enumValueStartPosition.get(2) - enumValueEndPosition.get(0);
        } else {
            List<Integer> newKeyPosition = positionList.get(i);
            // 15误差 是否同行
            if (Math.abs(y - newKeyPosition.get(1)) <= 15) {
                valueWidth = newKeyPosition.get(0) - enumValueEndPosition.get(0);
            } else {
                valueWidth = enumValueStartPosition.get(2) - enumValueEndPosition.get(0);
            }
        }

        return valueWidth;
    }

    /**
     * 字段详情
     *
     * @param map
     * @return
     */
    private void getAttribute(Map map, Map layoutMap, String groupName, FieldDto fieldDto, Map<String, Integer> keyMap) {
        List<AttributeDto> attributeDtoList = new ArrayList<>();
        int column = 0;
        int maxColumn = 0;
        int contWidth = 0;
        int maxContWidth = 0;

        Map<String, Object> maps = new HashMap<>();
        List<List<Integer>> positionList = (List<List<Integer>>) layoutMap.get("value");
        Iterator iteratored = map.keySet().iterator();
        int i = 0;
        int y = 0;
        while (iteratored.hasNext()) {
            List<Integer> keyPosition = new ArrayList<>();
            if (!checkLength(i, positionList, null, map)) {
                // json 与 layout 数量不同特殊处理
                keyPosition.add(0);
                keyPosition.add(0);
                keyPosition.add(0);
                keyPosition.add(0);
            } else {
                keyPosition = positionList.get(i);
            }
            Map<String, Integer> number = getNumber(y, keyPosition, contWidth, maxColumn, maxContWidth, column);
            y = number.get("y");
            column = number.get("column");
            maxColumn = number.get("maxColumn");
            maxContWidth = number.get("maxContWidth");
            contWidth = number.get("contWidth");
            // 字段key宽度
            int keyWidth = number.get("keyWidth");
            contWidth += keyWidth;
            i++;
            String key = String.valueOf(iteratored.next());
            Object field = map.get(key);
            boolean notNull = false;
            if (key.contains("*") || key.contains("※")) {
                notNull = true;
            }
            String fieldName = key.replace("*", "").replaceAll("※", "");
            AttributeDto attributeDto = new AttributeDto();
            attributeDto.setY(y);
            // 老版本预制字段
//            if (fieldName.equals("申请人")) {
//                fieldName = "申请人员";
//            }
            // 字段名称
            Integer keyValue = keyMap.get(fieldName);
            boolean isInitField = false;
            if (keyValue != null) {
                if (keyValue == 0) {
                    isInitField = true;
                } else {
                    continue;
                }
            } else {
                // 带负号的纯数字或纯小数
                boolean boo = Pattern.compile("^-?[0-9]+$").matcher(fieldName).matches();
                if (!boo) {
                    boo = Pattern.compile("^-?[0-9]+\\.[0-9]+$").matcher(fieldName).matches();
                }
                if (boo) {
                    continue;
                }
            }
            if (isInitField) {
                attributeDto.setSystemField(true);
            }
            keyMap.put(fieldName, 1);
            attributeDto.setCaption(fieldName);
            //是否必填
            attributeDto.setNotNull(notNull);
            if (field instanceof JSONObject) {
                log.debug("已提取分组信息key:{},value:{}", key, JSONUtil.toJsonStr(field));
                continue;
            } else if (field instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) field;
                if (jsonArray.size() > 0) {
                    Object subField = ((JSONArray) field).get(0);
                    if (subField instanceof JSONObject) {
                        log.debug("已提取子表信息key：{},value:{}", key, JSONUtil.toJsonStr(field));
                        continue;
                    } else {
//                        int valueWidth = getValueWidth(i, positionList, jsonArray.size(), y);
                        int valueWidth = getValueWidth(i, positionList, 1, y);
                        i = i + 1;
                        contWidth += valueWidth;
                        attributeDto.setNotNull(notNull);
                        attributeDto.setItemWidth(valueWidth + keyWidth);
                        attributeDto.setDataType(UdcDataTypeEnum.STRING.code());
                        attributeDto.setEnumArr(true);
                        attributeDto.setBelievable(false);
                        attributeDto.setValue(String.valueOf(field));
                    }
                }
            } else {
                boolean b = checkLength(i, positionList, null, map);
                if (!b) {
                    continue;
                }
                int valueWidth = getValueWidth(i, positionList, 1, y);
                i++;
                contWidth += valueWidth;
                attributeDto.setItemWidth(valueWidth + keyWidth);
                attributeDto.setValue(String.valueOf(field));

                if (field instanceof BigDecimal) {
                    attributeDto.setDataType(UdcDataTypeEnum.DECIMAL.code());
                    attributeDto.setDecimalDigits(((BigDecimal) field).scale());
                } else if (field instanceof String) {
                    String dataType = "";
                    if (String.valueOf(field).equals("0")) {
                        dataType = UdcDataTypeEnum.STRING.code();
                        attributeDto.setBelievable(false);
                    } else {
                        // 日期 时间 日期时间 正文 布尔的是否 百分比？
                        dataType = getStrDataType(String.valueOf(field));
                        if (String.valueOf(field).equals("点击上传")) {
                            dataType = UdcDataTypeEnum.ATTACHMENT.code();
                        }
                        if (key.contains("电话") || key.contains("身份证")) {
                            dataType = UdcDataTypeEnum.STRING.code();
                        }
                        if (dataType.equals(UdcDataTypeEnum.STRING.code())) {
                            attributeDto.setBelievable(false);
                        }
                    }
                    attributeDto.setDataType(dataType);
                    if (dataType.equals(UdcDataTypeEnum.DECIMAL.code())) {
                        BigDecimal decimal = new BigDecimal(String.valueOf(field));
                        attributeDto.setDecimalDigits(decimal.scale());
                    }
                } else if (field instanceof Integer) {
                    if ((Integer) field != 0) {
                        attributeDto.setDataType(UdcDataTypeEnum.INTEGER.code());
                    } else {
                        attributeDto.setDataType(UdcDataTypeEnum.STRING.code());
                    }
                } else if (field instanceof Long) {
                    if ((Long) field != 0) {
                        attributeDto.setDataType(UdcDataTypeEnum.BIGINTEGER.code());
                    } else {
                        attributeDto.setDataType(UdcDataTypeEnum.STRING.code());
                    }
                } else if (field instanceof Boolean) {
                    attributeDto.setDataType(UdcDataTypeEnum.BOOLEAN.code());
                } else {
                    log.error("错误类型-未知类型key：{},value:{}", key, JSONUtil.toJsonStr(field));
                }
            }
//            if (!isInitField) {
            attributeDtoList.add(attributeDto);
//            }
        }
        // 最后一行数据
        maxColumn = Integer.max(maxColumn, column);
        maxContWidth = Integer.max(maxContWidth, contWidth);
        if (attributeDtoList.size() != 0) {
            fieldDto.setCaption(FilterUtil.filter(groupName));
            fieldDto.setColumns(maxColumn == 0 ? column : maxColumn);
            fieldDto.setAttributeDtoList(attributeDtoList);
            fieldDto.setContWidth(maxContWidth == 0 ? contWidth : maxContWidth);
        }

    }


    private boolean checkLength(int i, List layoutList, List dataList, Map dataMap) {
        if (i + 1 > layoutList.size()) {
            log.error("错误长度layoutList:{},dataList:{},dataMap:{}", JSONUtil.toJsonStr(layoutList), JSONUtil.toJsonStr(dataList), JSONUtil.toJsonStr(dataMap));
            return false;
        }
        return true;
    }

    /**
     * 获取字段信息
     *
     * @param map
     */
    private FieldDto getFieldsInfo(Map map, Map layoutMap, Map<String, Integer> keyMap) {
        FieldDto fieldDto = new FieldDto();
        Iterator iteratored = map.keySet().iterator();
        List<AttributeDto> attributeDtoList = new LinkedList<>();
        while (iteratored.hasNext()) {
            List<AttributeDto> attributeDtos = new LinkedList<>();
            String key = String.valueOf(iteratored.next());
            Object field = map.get(key);
            // 带分组
            if (field instanceof JSONObject) {
                if (((JSONObject) field).size() == 0) {
                    continue;
                }
                LinkedHashMap linkedHashMap = ocrProcessService.toMap(String.valueOf(field));// JSONObject.parseObject(String.valueOf(field), LinkedHashMap.class, Feature.OrderedField);
                getAttribute(linkedHashMap, layoutMap, key, fieldDto, keyMap);
            } else if (field instanceof HashMap) {
                if (((LinkedHashMap) field).size() == 0) {
                    continue;
                }
                getAttribute((LinkedHashMap) field, layoutMap, key, fieldDto, keyMap);
            }
            // 不带分组
            else if (field instanceof String || field instanceof JSONArray || field instanceof BigDecimal || field instanceof Integer || field instanceof Long || field instanceof Boolean) {
                getAttribute(map, layoutMap, "ex_defGroup", fieldDto, keyMap);
                break;
            } else {
                log.error("错误类型-未知类型key：{},value:{}", key, JSONUtil.toJsonStr(field));
            }
            if (attributeDtos.size() != 0) {
                attributeDtoList.addAll(attributeDtos);
            }
        }

        return fieldDto;
    }


    /**
     * 判断字符串类型
     *
     * @param str
     * @return
     */
    private String getStrDataType(String str) {
        String dataType = "";
        // 整数
        String intRegex = "\\d+";
        //小数
        String decimalRegex = "^-?\\d+(\\.\\d+)?$";
        // 日期时间
        String dateTimeRegex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
        // 日期
        String dateRegex = "\\d{4}-\\d{2}-\\d{2}";
        // 时间
        String timeRegex = "\\d{2}:\\d{2}:\\d{2}";
        // 布尔
        String booleanRegex = "^(true|false)$";

        if (Pattern.matches(intRegex, str)) {
            dataType = UdcDataTypeEnum.STRING.code();
        } else if (Pattern.matches(decimalRegex, str)) {
            dataType = UdcDataTypeEnum.DECIMAL.code();
        } else if (Pattern.matches(dateTimeRegex, str)) {
            dataType = UdcDataTypeEnum.DATETIME.code();
        } else if (Pattern.matches(dateRegex, str)) {
            dataType = UdcDataTypeEnum.DATE.code();
        } else if (Pattern.matches(timeRegex, str)) {
            dataType = UdcDataTypeEnum.TIME.code();
        } else if (Pattern.matches(booleanRegex, str)) {
            dataType = UdcDataTypeEnum.BOOLEAN.code();
        } else if (str.equals("The text editing area for a content field.")) {
            dataType = UdcDataTypeEnum.CONTENT.code();
        } else {
            dataType = UdcDataTypeEnum.STRING.code();
        }
        return dataType;
    }


    private Map<String, List<String>> similarityEntity(String entityName, List<EntityDto> entityInfo) {
        Map<String, List<String>> similarEntityInfo = new HashMap<>();
        List<String> ids = new ArrayList<>();
        List<Map<String, String>> entityInfos = metaDataService.getEntityInfo(entityInfo);
        Map<String, String> indexMap = new HashMap<>();
        double min = 0.00;
        if (entityInfo.isEmpty()) {
            return similarEntityInfo;
        }
        for (int i = 0; i < entityInfo.size(); i++) {
            Map<String, String> map = entityInfos.get(i);
            String name = map.get("name");
            double similarity = StringSimilarity.similarity(entityName, name);
            if (similarity == 0) {
                continue;
            }
            if (indexMap.size() < 3) {
                min = Math.min(min, similarity);
                indexMap.put(String.valueOf(similarity), map.get("id"));
            } else {
                if (similarity > min || min == 0) {
                    indexMap.remove(String.valueOf(min));
                    indexMap.put(String.valueOf(similarity), map.get("id"));
                    min = similarity;
                }
            }
        }
        Iterator<String> iterator = indexMap.keySet().iterator();
        while (iterator.hasNext()) {
            String id = indexMap.get(iterator.next());
            ids.add(id);
        }
        similarEntityInfo.put(entityName, ids);
        return similarEntityInfo;
    }

    private Map<String, AttributeDto> similarityField(List<List<AttributeGroupDto>> attributeGroupInfos) {
        Map<String, AttributeDto> attributeDtoMap = new HashMap<>();
        if (attributeGroupInfos != null && !attributeGroupInfos.isEmpty()) {
            for (List<AttributeGroupDto> attributeGroupInfo : attributeGroupInfos) {
                Map<String, AttributeDto> entityField = metaDataService.getEntityField(attributeGroupInfo);
                attributeDtoMap.putAll(entityField);
            }
        }
        return attributeDtoMap;
    }


    private String writeTxt(String str,String apiKey) {
        UploadRequestDto uploadRequestDto = new UploadRequestDto();
        uploadRequestDto.setAppName("ai-from");
        uploadRequestDto.setFileName(UUID.randomUUID().toString() + ".txt");
        uploadRequestDto.setApiKey(apiKey);
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        FileDto upload = fileService.upload(inputStream, uploadRequestDto);
        return upload.getStorageKey();
    }

    public DataStandardResponseNew pageTransfer(TransferRequest transferRequest, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap, AiFormFLowInfo aiFormFLowInfo,String apiKey) {
        List<DataStandardResponse> dataStandardResponses = transfer(transferRequest, userUseFormInfo, llmInitialAnalysisMap, llmConfidenceMap, aiFormFLowInfo,apiKey);
        if (llmInitialAnalysisMap != null) {
            userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
        }
        if (llmConfidenceMap != null) {
            userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
        }
        String ocrJson = transferRequest.getOcrJson();
        PageDslResponse pageDslResponse = ocrToPageService.transfer(ocrJson, dataStandardResponses);
        userUseFormInfo.setEndTime(new Date());
        log.info("页面dsl前置数据：{}", JSONUtil.toJsonStr(pageDslResponse));
        DataStandardResponseNew dataStandardResponseNew = new DataStandardResponseNew();
        dataStandardResponseNew.setDataStandards(dataStandardResponses);
        dataStandardResponseNew.setPageDsl(pageDslResponse);
        return dataStandardResponseNew;
    }

    public AiFormFlowResponse getFlow(String id) {
        try {
            AiFormFlowResponse aiFormFlowResponse = new AiFormFlowResponse();
            String fromCache = cacheService.getFromCache(String.valueOf(id));
            ObjectMapper objectMapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            AiFormFLowInfo aiFormFLowInfo = null;
            try {
                aiFormFLowInfo = objectMapper.readValue(fromCache, AiFormFLowInfo.class);

            } catch (Exception e) {

            }
            aiFormFlowResponse.setHandleStage(aiFormFLowInfo == null ? "" : aiFormFLowInfo.getHandleStage());
            aiFormFlowResponse.setHandleStageType(aiFormFLowInfo == null ? 0 : aiFormFLowInfo.getHandleStageType());
            String ocrContextPath = aiFormFLowInfo == null ? "" : aiFormFLowInfo.getOcrContextPath();
            if (ocrContextPath != null && !ocrContextPath.equals("")) {
                try {
                    InputStream inputStream = fileService.download(ocrContextPath);
                    byte[] bytes = toByteArray(inputStream);
                    String content = new String(bytes);
                    aiFormFlowResponse.setResult(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if(!fromCache.contains("ocrContextPath")){
                    aiFormFlowResponse.setResult(fromCache);
                }
            }
            return aiFormFlowResponse;
        } catch (Exception e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[4096]; // 4KB 缓冲区
        int bytesRead;

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public DataHistoryResponse dataInfo() {
        DataHistoryResponse dataHistoryResponse = new DataHistoryResponse();
        Wrapper wrapper = new Wrapper(UserUseFormInfo.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());
        LocalDate today = LocalDate.now();
        // 近一周任务量趋势
        Map<String, Integer> weeklyTrend = new LinkedHashMap<>();
        //助手使用分部
        Map<String, Integer> assistantDistribution = new HashMap<>();
        assistantDistribution.put("edoc", 0);
        assistantDistribution.put("udc", 0);
        // 执行状态
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("success", 0);
        statusMap.put("error", 0);
        statusMap.put("cancel", 0);
        // 耗时情况
        Map<String, Integer> timeConsuming = new HashMap<>();
        timeConsuming.put("ai", 0);
        timeConsuming.put("user", 0);
        Long totalElements = 0L;
        double totalSavedHours = 0;
        int count = 0;
        List<DataHistoryDto> list = new ArrayList<>();
        try {
            Date zeroDate = sdf.parse(dateStr);
            weeklyTrend.put(String.valueOf(today.minusDays(7)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(6)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(5)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(4)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(3)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(2)), 0);
            weeklyTrend.put(String.valueOf(today.minusDays(1)), 0);
            wrapper.and().andLessThanOrEqualTo("creationTime", zeroDate);
//            List<UserUseFormInfo> userUseFormInfos = userUseFormInfoDao.selectListByWrapper(wrapper);
            List<UserUseFormInfo> userUseFormInfos = new ArrayList<>();
            for (UserUseFormInfo userUseFormInfo : userUseFormInfos) {
                String statusName = "";
                String assistantTypeName = "";
                DataHistoryDto dataHistoryDto = new DataHistoryDto();
                AssistantTypeEnum assistantType = userUseFormInfo.getAssistantType();
                String formName = userUseFormInfo.getFormName();
                Date creationTime = userUseFormInfo.getCreationTime();
                String imageInfo = userUseFormInfo.getImageInfo();
                Integer durationPage = userUseFormInfo.getDurationPage();
                Integer durationUdc = userUseFormInfo.getDurationUdc();
                Integer durationCorrection = userUseFormInfo.getDurationCorrection();
                AssistantTaskStatusEnum status = userUseFormInfo.getStatus();
                String generatedElementCount = userUseFormInfo.getGeneratedElementCount();
                Date endTime = userUseFormInfo.getEndTime();
                String creationTimeStr = sdf.format(creationTime);
                Integer integer = weeklyTrend.get(creationTimeStr);
                int timeDifferenceInSeconds = getTimeDifferenceInSeconds(creationTime, endTime) + durationPage + durationUdc;
                timeDifferenceInSeconds = (int) timeDifferenceInSeconds / 1000;
                ObjectMapper objectMapper = new ObjectMapper();
                Map imageInfoMap = objectMapper.readValue(imageInfo, new TypeReference<Map>() {
                });
                Map generatedElementCountMap = objectMapper.readValue(generatedElementCount, new TypeReference<Map>() {
                });
                if (integer != null) {
                    weeklyTrend.put(creationTimeStr, integer + 1);
                }
                if (status.equals(AssistantTaskStatusEnum.SUCCESS)) {
                    statusName = "成功";
                    count++;
                    statusMap.put("success", statusMap.get("success") + 1);
                    timeConsuming.put("ai", timeConsuming.get("ai") + timeDifferenceInSeconds);
                    timeConsuming.put("user", timeConsuming.get("user") + durationCorrection);
                } else if (status.equals(AssistantTaskStatusEnum.ERROR)) {
                    statusName = "失败";
                    statusMap.put("error", statusMap.get("error") + 1);
                } else {
                    statusName = "取消";
                    statusMap.put("cancel", statusMap.get("cancel") + 1);
                }
                if (assistantType.equals(AssistantTypeEnum.UDC)) {
                    assistantDistribution.put("udc", assistantDistribution.get("udc") + 1);
                    assistantTypeName = "表单生成助手";
                } else {
                    assistantDistribution.put("edoc", assistantDistribution.get("edoc") + 1);
                    assistantTypeName = "公文生成助手";
                }
                String fileName = String.valueOf(imageInfoMap.get("fileName")).split("\\.")[0];
                dataHistoryDto.setCreationTime(sdf2.format(creationTime));
                dataHistoryDto.setStatus(statusName);
                dataHistoryDto.setFormName(formName == null ? fileName : formName);
                dataHistoryDto.setElements(Integer.parseInt(String.valueOf(generatedElementCountMap.get("fields"))));
                dataHistoryDto.setDuration(timeDifferenceInSeconds + durationCorrection);
                dataHistoryDto.setAssistantType(assistantTypeName);
                totalElements = totalElements + Integer.parseInt(String.valueOf(generatedElementCountMap.get("fields")));
                list.add(dataHistoryDto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DecimalFormat df = new DecimalFormat("#.##");
        dataHistoryResponse.setTotalElements(totalElements);
        totalSavedHours = timeConsuming.get("ai") + timeConsuming.get("user");
        dataHistoryResponse.setTotalSavedHours(totalSavedHours);
        int totalTasks = statusMap.get("success") + statusMap.get("error");
        dataHistoryResponse.setTotalTasks(totalTasks);
        dataHistoryResponse.setCompletedTasks(statusMap.get("success"));
        dataHistoryResponse.setFailedTasks(statusMap.get("error"));
        double successRate = statusMap.get("error") == 0 ? 100.00 : statusMap.get("success") / (double) (statusMap.get("success") + statusMap.get("error")) * 100;
        dataHistoryResponse.setSuccessRate(Double.parseDouble(df.format(successRate)));
        double totalDurationSeconds = count != 0 ? totalSavedHours / (double) count : 0;
        dataHistoryResponse.setTotalDurationSeconds(Double.parseDouble(df.format(totalDurationSeconds)));
        dataHistoryResponse.setWeeklyTrend(weeklyTrend);
        List<Map<String, Integer>> assistantDistributionList = new ArrayList<>();
        assistantDistributionList.add(assistantDistribution);
        dataHistoryResponse.setAssistantDistribution(assistantDistributionList);
        Collections.sort(list, new Comparator<DataHistoryDto>() {
            @Override
            public int compare(DataHistoryDto r1, DataHistoryDto r2) {
                // 直接比较日期字符串（ISO 8601 格式支持字符串比较）
                return r1.getCreationTime().compareTo(r2.getCreationTime());
            }
        });
        dataHistoryResponse.setHistory(list);
        return dataHistoryResponse;
    }

    public int getTimeDifferenceInSeconds(Date startDate, Date endDate) {
        // 获取毫秒时间戳
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        // 计算毫秒差并取绝对值
        long diffInMillis = Math.abs(endTime - startTime);
        // 将毫秒转换为秒
        return (int) diffInMillis;
    }

    public void informationRecord(InformationRecordRequest informationRecordRequest, HttpServletRequest request) {
        String apiKey = request.getHeader("api-key");
        UserUseFormInfo userUseFormInfo = userUseFormInfoDao.selectOneById(informationRecordRequest.getTaskId());
        userUseFormInfo.setGeneratedFormId(informationRecordRequest.getGeneratedFormId());
        userUseFormInfo.setUserCorrectedAnalysis(writeTxt(informationRecordRequest.getUserCorrectedAnalysis(),apiKey));
        userUseFormInfo.setCorrectionDiff(writeTxt(informationRecordRequest.getCorrectionDiff(),apiKey));
        userUseFormInfo.setIsCorrected(informationRecordRequest.getIsCorrected());
        userUseFormInfo.setDurationCorrection(informationRecordRequest.getDurationCorrection());
        userUseFormInfo.setSelectedTemplateId(informationRecordRequest.getSelectedTemplateId());
        userUseFormInfo.setTemplateSchema(writeTxt(informationRecordRequest.getTemplateSchema(),apiKey));
        userUseFormInfo.setDurationPage(informationRecordRequest.getDurationPage());
        userUseFormInfo.setDurationUdc(informationRecordRequest.getDurationUdc());
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.SUCCESS);
        userUseFormInfo.setFormName(informationRecordRequest.getFormName());
        userUseFormInfoDao.update(userUseFormInfo);

    }
}
