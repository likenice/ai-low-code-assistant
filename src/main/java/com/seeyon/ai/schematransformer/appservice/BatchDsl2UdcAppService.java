//package com.seeyon.ai.schematransformer.appservice;
//
//import com.alibaba.excel.util.StringUtils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.seeyon.ai.schematransformer.dto.*;
//import com.seeyon.ai.schematransformer.model.DslTransformParams;
//import com.seeyon.ai.schematransformer.service.BatchDsl2UdcService;
//import com.seeyon.ai.schematransformer.service.SchemaTransformerGW;
//import com.seeyon.ai.schematransformer.util.JsonUtil;
//import com.seeyon.boot.annotation.*;
//import com.seeyon.boot.context.RequestContext;
//import com.seeyon.boot.enums.StandardAppId;
//import com.seeyon.boot.exception.BusinessException;
////import com.seeyon.boot.starter.cip.constant.CipConstants;
//import com.seeyon.boot.transport.SingleRequest;
//import com.seeyon.boot.transport.SingleResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.bind.annotation.RequestBody;
//
//import java.util.*;
//
//@AppService(value = "批量DSL转Udc", description = "批量DSL转Udc")
////@DubboService
//@Service
//@Slf4j
//public class BatchDsl2UdcAppService {
//
//    @Autowired
//    private MetaDataInfoService metaDataInfoService;
//    @Autowired
//    private BatchDsl2UdcService batchDsl2UdcService;
//
//
////    @AppServiceOperation(openApi=  @OpenApi(  url = "/" + StandardAppId.CIP_CAPABILITY + "/form/assistant/batchOcr2Udc",
////            customs = @Custom(name = CipConstants.OPEN_API_CUSTOM, properties = {
////                    // 此开放API不显示
////                    @CustomAttribute(name = CipConstants.NOT_SHOW_IN_MANUAL, value = "true")
////            })),value = "批量生成文单页面", description = "输入多个\"ocr schema信息\",将其结合模版信息合并. 最终转换为公文文单schema结构.并保存到文单中.")
//    public SingleResponse<GongwenOcrBatchImportResult> batchOcr2Udc(@RequestBody SingleRequest<GongwenOcrBatchImport> params) {
//        //初始化租户信息
//        Long tenantId = RequestContext.get().getTenantId();
//        if(tenantId == null){
//            RequestContext.get().setTraceId(params.getData().getTid());
//        }
//        GongwenOcrBatchImportResult gongwenOcrBatchImportResult = new GongwenOcrBatchImportResult();
//
//        List<JsonNode> ocrSchemas = params.getData().getOcrSchemas();
//        List<String> titleNames = params.getData().getTitleNames();
//        boolean update = params.getData().isUpdate();
//        String type = params.getData().getType();
//        int num = 0;
//        boolean isAllSuccess = true;
//        List<GongwenOcrBatchResult> message = new ArrayList<>();
////        String templateAppName = null;
//        String templateAppId = null;
//        JsonNode templateNode = null;
//        try {
//            //根据gongwenId 获取appName
//            Map<String, Object> paramMap = new HashMap<>();
//            paramMap.put("isDefault", Boolean.valueOf(true));
//            GwAppInfo tempalteAppInfo = metaDataInfoService.getGwAppInfoByGongwenType(type, paramMap);
////            templateAppName = tempalteAppInfo.getAppName();
//            templateAppId = tempalteAppInfo.getAppId();
//            // 读取udcSchema
//            GwPageInfoDto gwPageInfoDto = metaDataInfoService.getUdcSchemaByAppName(templateAppId);
//            String templateSchema = gwPageInfoDto.getPageSchema();
//            ObjectMapper objectMapper = new ObjectMapper();
//            templateNode = objectMapper.readTree(templateSchema);
//
//
//
//            for(JsonNode ocrSchemaNode : ocrSchemas){
//                GongwenOcrBatchResult result = new GongwenOcrBatchResult();
//                String titleName = titleNames.get(num);
//                try {
//                //判断是新增还是更新. 创建/获取appInfo
//
//                    String currAppId = null;
//                    JsonNode appInfo = null;
//
//                    Long pageId = null;
//
//
//
//                    if(!update){
//                        //TODO: 创建公文文单(重名是否创建成功)
//                        Long gwId = metaDataInfoService.createGwPageInfo(titleName,type,0);
//
//                        if(gwId == null){ //不存在
//                            throw new BusinessException("创建失败:"+titleName);
//                        }
//                        GwAppInfo gwAppInfo = metaDataInfoService.getGwAppInfoByGwId(gwId);
//                        if(gwAppInfo == null){ //不存在
//                            throw new BusinessException("未找到gwId:"+gwId);
//                        }
//                        currAppId = gwAppInfo.getAppId();
//
//                    } else {
//                        //根据 titleName 获取文单appInfo
//                        Map<String,Object> titleParamMap = new HashMap<>();
//                        paramMap.put("name",titleName);
//                        GwAppInfo titleNameAppInfo = metaDataInfoService.getGwAppInfoByGongwenType(type,titleParamMap);
//                        currAppId = titleNameAppInfo.getAppId();
//                        if(StringUtils.isBlank(currAppId)){
//                            throw new RuntimeException("文单名称不存在:"+titleName);
//                        }
//
//                    }
//                    // 获取titleName对应的AppInfo  DocumentFormDto对象
//                    appInfo = metaDataInfoService.getAppInfoByAppId(currAppId);
//
//
//                    //统一页面dsl
//                    JsonNode dslNode = SchemaTransformerGW.convertLayoutByTemplate(ocrSchemaNode, templateNode);
//
//                    // 将统一页面DSL转UDCschema
//    //                JsonNode udcNode = dsl2UdcSchema(dslNode,templateNode);
//                    DslTransformParams dslTransformParams = new DslTransformParams();
//                    //TODO: 获取实体信息
////const docFullnamePrefix = 'com.seeyon.edoc335172694483814428.domain.entity';
////                    const docPresetEntity = new Set([
////                    // 发文单
////  `com.seeyon.edoc335172694483814428.domain.entity.IssuedDocument`,
////  `${docFullnamePrefix}.IssuedDocumentExt`,
////  `${docFullnamePrefix}.UdcIssueDocumentExt`,
////
////                    // 收文单
////  `${docFullnamePrefix}.ReceivedDocument`,
////  `${docFullnamePrefix}.ReceivedDocumentExt`,
////  `${docFullnamePrefix}.Shouwenkuozhanbiao`,
////
////                    // 签报单
////  `${docFullnamePrefix}.SignDocument`,
////  `${docFullnamePrefix}.SignDocumentExt`,
////  `${docFullnamePrefix}.UdcSignDocumentExt`,
////
////                    // 签收单
////  `${docFullnamePrefix}.ExchangeDetail`,
////  `${docFullnamePrefix}.ExchangeDetailExt`,
////]);
//
//
//                    //根据类别获取 entityId.
////                    Long entityId = 0L;
//                    ArrayNode entityLists = metaDataInfoService.getGwEntityLists(type);
//                    dslTransformParams.setDsl(dslNode);
//
//                    dslTransformParams.setTempSchema(templateNode.asText());
//                    dslTransformParams.setDoc(true); //true: 公文, false: udc表单
//                    dslTransformParams.setEntityList(entityLists);
//                    dslTransformParams.setAppInfo(appInfo);
//
//                    JsonNode udcNode = batchDsl2UdcService.transformDsl(dslTransformParams);
//
//                    //获取所有参照的fullName
//                    Set<String> udcReferenceFullNameSet = JsonUtil.getAllUdcReferenceFullName(udcNode);
//
//                    //根据allUdcReferenceFullName 查询所有参照的映射关系
//                    Map<String, String> referenceMap = metaDataInfoService.getReferenceMap(udcReferenceFullNameSet);
//
//                    //更新udcNode 中type= "UdcReference"  , 设置settings-> dataReference -> fullName
//                    JsonUtil.updateUdcReference(udcNode, referenceMap);
//
//
////                    // 先获取表单信息
//                    boolean success = metaDataInfoService.savePageInfo(pageId,udcNode);
//                    if(!success){
//                        isAllSuccess = false;
//                    }
//
//                    result.setSuccess(true);
//                    result.setTitleName(titleName);
//                } catch (Exception e) {
//                    isAllSuccess = false;
//                    result.setSuccess(false);
//                    result.setTitleName(titleName);
//                    result.setReasons( e.getMessage());
//                    log.error("Schema转换失败", e);
//                    throw new RuntimeException("Schema转换失败: " + e.getMessage());
//                }
//                message.add(result);
//                num++;
//
//            }
//        }catch (Exception e) {
//            throw new RuntimeException("失败:"+e.getMessage());
//        }
//        gongwenOcrBatchImportResult.setMessage(message);
//        gongwenOcrBatchImportResult.setAllSuccess(isAllSuccess);
//
//        return SingleResponse.from(gongwenOcrBatchImportResult);
//    }
//
//
//}
