package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.seeyon.ai.common.dto.AppProperties;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.appservice.AiPromptSvcAppService;
import com.seeyon.ai.ocrprocess.dao.UserUseFormInfoDao;
import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.enums.AiIdentifyTypeEnum;
import com.seeyon.ai.ocrprocess.enums.AiformFlowEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTaskStatusEnum;
import com.seeyon.ai.ocrprocess.enums.AssistantTypeEnum;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.FileDto;
import com.seeyon.ai.ocrprocess.form.UploadRequestDto;
import com.seeyon.ai.ocrprocess.form.request.EdocIdentifyRequest;
import com.seeyon.ai.ocrprocess.form.request.OcrProcessRequest;
import com.seeyon.boot.context.RequestContext;
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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * @author pb
 */
@Service
@Slf4j
public class    AiEdocService {

    @Autowired
    private AiPromptSvcAppService aiPromptSvcAppService;
    //    @Value("${seeyon.ocr.path:http://10.101.129.4:8889}")
//    private String baseUrl;
    @Autowired
    AppProperties appProperties;

    @Autowired
    private OcrProcessService ocrProcessService;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private EdocPageDslTransferService edocPageDslTransferService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserUseFormInfoDao userUseFormInfoDao;


    public Long edocIdentify(EdocIdentifyRequest edocIdentifyRequest, HttpServletRequest request) {
        Integer type = edocIdentifyRequest.getType();
        Long id = Ids.gidLong();
        edocIdentifyRequest.setId(id);
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
                ocrIdentifyImpl(edocIdentifyRequest,request.getHeader("api-key"));
            });
        } else if (type == AiIdentifyTypeEnum.Excel.getCode()) {
            executorService.execute(() -> {
                excelIdentifyImpl(edocIdentifyRequest,request.getHeader("api-key"));
            });
        } else {
            throw new PlatformException(" no know type");
        }
        return edocIdentifyRequest.getId();
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


    public void ocrIdentifyImpl(EdocIdentifyRequest edocIdentifyRequest,String apiKey) {
        UserUseFormInfo userUseFormInfo = new UserUseFormInfo();
        userUseFormInfo.setTaskId(edocIdentifyRequest.getId());
        userUseFormInfo.setUserId(RequestContext.get().getUserId());
        userUseFormInfo.setAssistantType(AssistantTypeEnum.EDOC);
        userUseFormInfo.setCreationTime(new Date());
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.CANCEL);
        userUseFormInfo.setImagePath((edocIdentifyRequest.getPath()));
        InputStream inputStreamToImage = fileService.download(edocIdentifyRequest.getPath());
        BufferedImage image = null;
        FileDto fileDto = fileService.selectOrigFileByStorageKey(edocIdentifyRequest.getPath());
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
        inputImageInfo.append("\""+fileName+"\"");
        inputImageInfo.append(",\"width\":");
        inputImageInfo.append(width);
        inputImageInfo.append(",\"height\":");
        inputImageInfo.append(height + "}");
        userUseFormInfo.setImageInfo(inputImageInfo.toString());
        // 使用静态ObjectMapper实例，避免重复创建
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String fromCache = cacheService.getFromCache(String.valueOf(edocIdentifyRequest.getId()));
        AiFormFLowInfo aiFormFLowInfo = null;

        try {
            aiFormFLowInfo = objectMapper.readValue(fromCache, AiFormFLowInfo.class);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }
        // 使用Jackson替代JSONUtil.toJsonStr
        try {
            cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()),
                    objectMapper.writeValueAsString(aiFormFLowInfo));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AiFormFLowInfo", e);
        }
        String ocrUrl = "";
        String deployType = appProperties.getDeployType();
        String aiManagerAddress = appProperties.getAiManagerAddress();
        String aiManagerApiKey = appProperties.getAiManagerApiKey();
        if ("public".equalsIgnoreCase(deployType)) {
            ocrUrl = aiManagerAddress + "/ai-manager/form/gongwen/recognize";
        } else {
            ocrUrl = appProperties.getOcrUrl() + "/gongwen/recognize";
        }
        String path = edocIdentifyRequest.getPath();
        log.info("ocr 识别开始:{}", ocrUrl);
        long startTime = System.currentTimeMillis();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(ocrUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
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
                log.info("ocr服务返回值：{}", responseString);
                // 使用Jackson替代JSONObject.parseObject
                JsonNode jsonNode = objectMapper.readTree(responseString);
                long endTime = System.currentTimeMillis();
                userUseFormInfo.setDurationOcr((int) (endTime-startTime));
                if (!String.valueOf(jsonNode.get("status").asText()).equals("1")) {
                    String message =  jsonNode.get("message")==null?"ocr请求失败":jsonNode.get("message").asText();
                    handleError(aiFormFLowInfo, message, 2000, userUseFormInfo, null, null,apiKey);
                } else {
                    ArrayNode resultArray = (ArrayNode) jsonNode.get("result");
                    if (resultArray.size() == 0) {
                        handleError(aiFormFLowInfo,
                                AiformFlowEnum.ocrFieldNull.getCaption(),
                                AiformFlowEnum.ocrFieldNull.code(), userUseFormInfo, null, null,apiKey);
                    } else {
                        String ocrJson = resultArray.get(0).toString();
                        if ("{}".equals(ocrJson)) {
                            handleError(aiFormFLowInfo,
                                    AiformFlowEnum.ocrFieldNull.getCaption(),
                                    AiformFlowEnum.ocrFieldNull.code(), userUseFormInfo, null, null,apiKey);
                        } else {
                            processOcrResult(aiFormFLowInfo, edocIdentifyRequest, path, ocrJson,userUseFormInfo,apiKey);
                        }
                    }
                }
            }
        } catch (IOException e) {
            handleServerError(aiFormFLowInfo, e, userUseFormInfo, null, null,apiKey);
        }
    }

    // 提取的辅助方法
    // 辅助方法 - 处理OCR错误
    private void handleError(AiFormFLowInfo aiFormFLowInfo, String message, int code, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap,String apiKey) {
        aiFormFLowInfo.setHandleStage(message);
        aiFormFLowInfo.setHandleStageType(code);
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo);
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

    private void handleServerError(AiFormFLowInfo aiFormFLowInfo, Exception e, UserUseFormInfo userUseFormInfo, Map llmInitialAnalysisMap, Map llmConfidenceMap,String apiKey) {
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrServerError.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrServerError.code());
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo);
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

    private void saveToCache(AiFormFLowInfo aiFormFLowInfo) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()),
                    objectMapper.writeValueAsString(aiFormFLowInfo));
        } catch (JsonProcessingException e) {
            log.error("Failed to save to cache", e);
        }
    }

    private void processOcrResult(AiFormFLowInfo aiFormFLowInfo,
                                  EdocIdentifyRequest edocIdentifyRequest,
                                  String path, String ocrJson,UserUseFormInfo userUseFormInfo,String apiKey) {
        try {
            Map<String, Object> llmInitialAnalysisMap = new HashMap<>();
            Map<String, Object> llmConfidenceMap = new HashMap<>();
            ObjectMapper objectMapper = new ObjectMapper();
            Map map = objectMapper.readValue(ocrJson, new TypeReference<Map>() {
            });
            OcrProcessRequest ocrProcessRequest = new OcrProcessRequest();
            String resize = "";
//            if (JSONUtil.toJsonStr(map.get("resize")) != null) {
//                resize = JSONUtil.toJsonStr(map.get("resize"));
//            }
            BufferedImage image = ImageIO.read(fileService.download(path));
            int width = image.getWidth();
            int height = image.getHeight();
            resize = width + "," + height;
            ocrProcessRequest.setOcr(JSONUtil.toJsonStr(map.get("ocr")));
            ocrProcessRequest.setTsr(JSONUtil.toJsonStr(map.get("tsr")));
            ocrProcessRequest.setResize(resize);
            ocrProcessRequest.setAiFormFLowInfo(aiFormFLowInfo);
            ocrProcessRequest.setEdocEntityDtos(edocIdentifyRequest.getEntityInfo());
            userUseFormInfo.setOcrResult(writeTxt(JSONUtil.toJsonStr(map),apiKey));
            String ocrSchema = ocrProcessService.processToEdoc(ocrProcessRequest,userUseFormInfo,llmInitialAnalysisMap,llmConfidenceMap,apiKey);
            String storageKey = writeTxt(ocrSchema,apiKey);
            userUseFormInfo.setFinishMessageStorageKey(storageKey);
            if (llmInitialAnalysisMap != null) {
                userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
            }
            if (llmConfidenceMap != null) {
                userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
            }
            userUseFormInfo.setEndTime(new Date());
            userUseFormInfoDao.create(userUseFormInfo);
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.entityDslCreate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.entityDslCreate.code());
            aiFormFLowInfo.setOcrContextPath(storageKey);
            saveToCache(aiFormFLowInfo);
        } catch (Exception e) {
            handleOcrProcessError(aiFormFLowInfo, e,userUseFormInfo,apiKey);
        }
    }

    private void handleOcrProcessError(AiFormFLowInfo aiFormFLowInfo, Exception e,  UserUseFormInfo userUseFormInfo,String apiKey) {
        aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrProcessError.getCaption());
        aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrProcessError.code());
        aiFormFLowInfo.setOcrContextPath("");
        saveToCache(aiFormFLowInfo);
        userUseFormInfo.setErrorMessageStorageKey(writeTxt("ocr 未识别出字段信息",apiKey));
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.ERROR);
        userUseFormInfo.setEndTime(new Date());
        userUseFormInfoDao.create(userUseFormInfo);
        log.error("ocr识别失败：{}", e);
    }
    public void excelIdentifyImpl(EdocIdentifyRequest edocIdentifyRequest,String apiKey) {
        String path = edocIdentifyRequest.getPath();
        UserUseFormInfo userUseFormInfo = new UserUseFormInfo();
        userUseFormInfo.setTaskId(edocIdentifyRequest.getId());
        userUseFormInfo.setUserId(RequestContext.get().getUserId());
        userUseFormInfo.setAssistantType(AssistantTypeEnum.EDOC);
        userUseFormInfo.setCreationTime(new Date());
        userUseFormInfo.setStatus(AssistantTaskStatusEnum.CANCEL);
        userUseFormInfo.setImagePath(path);
        Map llmInitialAnalysisMap = new HashMap();
        Map llmConfidenceMap = new HashMap();
        String fromCache = cacheService.getFromCache(String.valueOf(edocIdentifyRequest.getId()));
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        AiFormFLowInfo aiFormFLowInfo = null;
        try {
            aiFormFLowInfo = objectMapper.readValue(fromCache, AiFormFLowInfo.class);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }
        try {
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.tableStructureGenerate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.tableStructureGenerate.code());
            cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));
            Map<String, Object> excelResponse = excelAnalyze(path);
            List<List<Map<List, String>>> list = (List<List<Map<List, String>>>) excelResponse.get("rows");
            List<CellDto> cellDtos = (List<CellDto>) excelResponse.get("cells");
            Map<String, String> layoutMapping = (Map<String, String>) excelResponse.get("mapping");
            AiPromptSvcCallDto aiPromptSvcCallDto = new AiPromptSvcCallDto();
            aiPromptSvcCallDto.setPromptCode("edoc_table_structure_generate");
            aiPromptSvcCallDto.setInput(JSONUtil.toJsonStr(list));
            aiPromptSvcCallDto.setPromptVarMap(new HashMap<>());
            String tableStructure = String.valueOf(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
            int num = 0;
            while (num < 5) {
                try {
                    LinkedHashMap<String, Object> tableStructureMap = objectMapper.readValue(tableStructure, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
                    break;
                } catch (Exception e) {
                    tableStructure = JSONUtil.toJsonStr(aiPromptSvcAppService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap)).replaceAll("\n", "").replaceAll("\t", "").replaceAll("```json", "").replaceAll("```", "");
                    num++;
                }
            }
            log.info("提示词结构生成返回值：{}", tableStructure);
            String excelJson = ocrProcessService.edocPageDslProcess(tableStructure, edocIdentifyRequest.getEntityInfo(), aiFormFLowInfo, cellDtos, layoutMapping, "",userUseFormInfo,llmInitialAnalysisMap,llmConfidenceMap);
            String storageKey = writeTxt(excelJson,apiKey);
            userUseFormInfo.setFinishMessageStorageKey(storageKey);
            if (llmInitialAnalysisMap != null) {
                userUseFormInfo.setLlmInitialAnalysis(writeTxt(JSONUtil.toJsonStr(llmInitialAnalysisMap),apiKey));
            }
            if (llmConfidenceMap != null) {
                userUseFormInfo.setDurationLlm(writeTxt(JSONUtil.toJsonStr(llmConfidenceMap),apiKey));
            }
            userUseFormInfo.setEndTime(new Date());
            userUseFormInfoDao.create(userUseFormInfo);
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.entityDslCreate.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.entityDslCreate.code());
            aiFormFLowInfo.setOcrContextPath(storageKey);
            cacheService.saveToCache(String.valueOf(aiFormFLowInfo.getId()), JSONUtil.toJsonStr(aiFormFLowInfo));
            System.out.println(excelJson);
        } catch (Exception e) {
            aiFormFLowInfo.setHandleStage(AiformFlowEnum.ocrProcessError.getCaption());
            aiFormFLowInfo.setHandleStageType(AiformFlowEnum.ocrProcessError.code());
            aiFormFLowInfo.setOcrContextPath("");
            handleServerError(aiFormFLowInfo,e,userUseFormInfo,llmInitialAnalysisMap,llmConfidenceMap,apiKey);
            log.error("excel识别失败：{}", e);
        }
    }


    private Map<String, Object> excelAnalyze(String filePath) {
        List<List<Map<List, String>>> list = new ArrayList<>();
        List<CellDto> cellDtos = new ArrayList<>();
        int index = 0;
        Map<String, String> layoutMapping = new HashMap<>();
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
                int x2 = 1;
                String noNullVal = "";
                int i = 0;
                while (i < row.getLastCellNum()) {
//                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);// 迭代列
                    if (cell == null) {
                        i++;
                        x1 = x2 + 60;
                        continue;
                    }
                    CellStyle style = cell.getCellStyle();
                    boolean hasTopBorder = style.getBorderTop() != BorderStyle.NONE;
                    boolean hasBottomBorder = style.getBorderBottom() != BorderStyle.NONE;
                    String val = getCellValue(cell);
                    int mergedRows = getMergedRowCount(cell);
//                    if(mergedRows==0){
//                        mergedRows=1;
//                    }
                    int mergedCols = getMergedColumnCount(cell);
                    i = i + mergedCols;
                    int y2 = y + mergedRows * 17;
                    Map<List, String> map = new LinkedHashMap<>();
                    List<Integer> layoutList = new ArrayList<>(4);
                    x2 = mergedCols * 60 + x1;
                    layoutList.add(x1);
                    layoutList.add(y);
                    layoutList.add(x2);
                    layoutList.add(y2);
                    x1 = x2;
                    if (val.equals("")) {
                        continue;
                    }
                    map.put(layoutList, val);
                    rowList.add(map);
                    if (hasTopBorder && hasBottomBorder) {
                        CellDto cellDto = new CellDto();
                        cellDto.setId((long) index);
                        List<Double> location = new ArrayList<>();
                        location.add(Double.parseDouble(String.valueOf(layoutList.get(0))));
                        location.add(Double.parseDouble(String.valueOf(layoutList.get(1))));
                        location.add(Double.parseDouble(String.valueOf(layoutList.get(2))));
                        location.add(Double.parseDouble(String.valueOf(layoutList.get(3))));
                        cellDto.setLocation(location);
                        List<Map<String, Object>> contents = new ArrayList<>();
                        Map<String, Object> content = new HashMap<>();
                        content.put(JSONUtil.toJsonStr(layoutList), noNullVal);
                        contents.add(content);
                        cellDto.setContents(contents);
                        layoutMapping.put(JSONUtil.toJsonStr(layoutList), JSONUtil.toJsonStr(layoutList));
                        cellDtos.add(cellDto);
                        index++;
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
        Map<String, Object> response = new HashMap<>();
        response.put("rows", list);
        response.put("cells", cellDtos);
        response.put("mapping", layoutMapping);
        return response;
    }

    public static int getMergedRowCount(Cell cell) {
        if (cell == null) {
            return 1;
        }

        Sheet sheet = cell.getSheet();
        int rowIndex = cell.getRowIndex();
        int columnIndex = cell.getColumnIndex();

        // 遍历所有合并区域
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);

            // 检查当前单元格是否在这个合并区域内
            if (mergedRegion.isInRange(rowIndex, columnIndex)) {
                // 返回合并的行数(结束行-起始行+1)
                return mergedRegion.getLastRow() - mergedRegion.getFirstRow() + 1;
            }
        }

        // 如果没有找到合并区域，返回1
        return 1;
    }

    public static int getMergedColumnCount(Cell cell) {
        if (cell == null) {
            return 1;
        }

        Sheet sheet = cell.getSheet();
        int rowIndex = cell.getRowIndex();
        int columnIndex = cell.getColumnIndex();

        // 遍历所有合并区域
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);

            // 检查当前单元格是否在这个合并区域内
            if (mergedRegion.isInRange(rowIndex, columnIndex)) {
                // 返回合并的列数(结束列-起始列+1)
                return mergedRegion.getLastColumn() - mergedRegion.getFirstColumn() + 1;
            }
        }

        // 如果没有找到合并区域，返回1
        return 1;
    }

    /**
     * 检查单元格是否是合并区域的一部分
     *
     * @param cell 要检查的单元格
     * @return 如果是合并区域返回true，否则返回false
     */
    public static boolean isMerged(Cell cell) {
        return getMergedRowCount(cell) > 1;
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


    private String writeTxt(String str,String apiKey) {
        UploadRequestDto uploadRequestDto = new UploadRequestDto();
        uploadRequestDto.setAppName("ai-from");
        uploadRequestDto.setFileName(UUID.randomUUID().toString() + ".txt");
        uploadRequestDto.setApiKey(apiKey);
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        FileDto upload = fileService.upload(inputStream, uploadRequestDto);
        return upload.getStorageKey();

    }


}
