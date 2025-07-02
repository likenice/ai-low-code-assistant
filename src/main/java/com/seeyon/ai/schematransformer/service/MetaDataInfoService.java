//package com.seeyon.ai.schematransformer.service;
//
//import com.alibaba.excel.util.StringUtils;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.seeyon.ai.schematransformer.dto.GwAppInfo;
//import com.seeyon.ai.schematransformer.dto.GwPageInfoDto;
//import com.seeyon.boot.exception.BusinessException;
//import com.seeyon.boot.exception.ErrorCode;
//import com.seeyon.boot.starter.dubbo.dto.AppServiceInvokeDto;
//import com.seeyon.boot.starter.extmetadata.dto.UdcEntityMetaDataDto;
//import com.seeyon.boot.starter.extmetadata.dto.request.UdcEntityMetaDataSearchDto;
//import com.seeyon.boot.starter.proxy.appservice.AppServiceReliableInvoker;
//import com.seeyon.boot.transport.*;
//import com.seeyon.boot.util.JsonUtils;
//import com.seeyon.boot.util.id.Ids;
//import com.seeyon.udc.common.dto.ApplicationDto;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
//import org.apache.poi.hssf.record.DVALRecord;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@Service
//@Slf4j
//public class MetaDataInfoService {
//
//
//    @Autowired
//    AppServiceReliableInvoker appServiceReliableInvoker;
//
//    /**
//     * 获取默认公文单id
//     * 根据类别获取公文
//     * @param gongwenType
//     * @return
//     * @throws IOException
//     */
//    public GwAppInfo getGwAppInfoByGongwenType(String gongwenType , Map<String,Object> paramMap) throws IOException {
//
//        String filterPlanGuids = "";
//        switch (gongwenType) {
//            case "fwd":
//                filterPlanGuids = "0";
//                break;
//            case "swd":
//                filterPlanGuids = "1";
//                break;
//            case "qbd":
//                filterPlanGuids = "2";
//                break;
//            case "qsd":
//                filterPlanGuids = "3";
//                break;
//            default:
//                throw new RuntimeException("gongwenType is invalid:"+gongwenType);
//        }
//
//
//        PageResponse<Object> response = null;
//        try {
//            String fullName = "com.seeyon.edoc335172694483814428.api.DocumentFormAppService";
//            String methodName = "selectPageByConditions";
//            PageInfo pageInfo = new PageInfo();
//            pageInfo.setPageNumber(1);
//            pageInfo.setPageSize(200);
//            pageInfo.setNeedTotal(false);
//            Map<String, Object> params = new HashMap<>();
//            params.put("formType", filterPlanGuids);
//
//            if(paramMap != null){
//
//                for (Map.Entry<String,Object> entry : paramMap.entrySet()) {
//                    params.put(entry.getKey(), entry.getValue());
//                }
//            }
//
//            PageRequest pageRequest = PageRequest.from(pageInfo,params);
//
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(pageRequest));
//            appServiceInvokeDto.setParamType(PageRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            response = appServiceReliableInvoker.invokeForPage(SingleRequest.from(appServiceInvokeDto), "edoc335172694483814428");
//
//            PageData<Object> listData = response.getData();
//            List<Object> contentList = listData.getContent();
//            ObjectMapper objectMapper = new ObjectMapper();
//            ArrayNode contentListNode = (ArrayNode)objectMapper.readTree(contentList.toString());
//            for (JsonNode contentNode : contentListNode) {
//                JsonNode jsonNode = contentNode.get("isDefault");
//
//                if (jsonNode != null && jsonNode.asBoolean()) {
//                    return  JsonUtils.fromJson(contentNode.toString(), GwAppInfo.class);
//                }
//            }
//
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
//
//            return null;
//        }
//        log.info("泛化调用(getDefaultIdByGongwenType)获取结果：" + JsonUtils.toJson(response));
//
//        return null;
//
//    }
//
//    /**
//     * 根据gwid查找 公文与udcAppId映射关系
//     *
//     * @param id
//     * @return
//     */
//    public GwAppInfo getGwAppInfoByGwId(Long id) {
//        SingleResponse<Object> singleResponse = null;
//        try {
//            String fullName = "com.seeyon.edoc335172694483814428.api.DocumentFormAppService";
//            String methodName = "selectById";
//
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(id));
//            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            singleResponse = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), "edoc335172694483814428");
//            Object content = singleResponse.getData().getContent();
//            if(content != null){
//                log.info("泛化调用(getGwAppInfoByGwId)：" + content.toString());
//                GwAppInfo gwAppInfo = JsonUtils.fromJson(JsonUtils.toJson(content), GwAppInfo.class);
//                return gwAppInfo;
//            }
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
//            singleResponse = SingleResponse.from(null);
//            singleResponse.setCode(ErrorCode.BUSINESS_ERROR);
//            singleResponse.setMessage(exception.getMessage());
//
//        }
//
//        return null;
//    }
//
//
//    /**
//     * 读取udcSchema
//     *
//     * @param appId
//     * @return
//     * @throws IOException
//     */
//    public GwPageInfoDto getUdcSchemaByAppName(String appId) throws IOException {
//
//        ListResponse<Object> response = null;
//        try {
//            String fullName = "com.seeyon.udc.appservice.PageInfoAppService";
//            String methodName = "selectDetailListByConditions4Page";
//            PageInfo pageInfo = new PageInfo();
//            pageInfo.setPageNumber(1);
//            pageInfo.setPageSize(200);
//            pageInfo.setNeedTotal(false);
//            Map<String, Object> params = new HashMap<>();
//            params.put("appId", appId);
////            params.put("isDefault", true);
//            ListRequest pageRequest = ListRequest.from(params);
//
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(pageRequest));
//            appServiceInvokeDto.setParamType(ListRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            response = appServiceReliableInvoker.invokeForList(SingleRequest.from(appServiceInvokeDto), "udc");
//
//            ListData<Object> listData = response.getData();
//            List<Object> contentList = listData.getContent();
//
//            List<GwPageInfoDto> gwPageInfoDtoList = JsonUtils.toList(contentList.toString(), GwPageInfoDto.class);
//            if (gwPageInfoDtoList != null && gwPageInfoDtoList.size() > 0) {
//                return gwPageInfoDtoList.get(0);
//            }
//
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
//
//            return null;
//        }
//        return null;
//    }
//
//
//
//
//
//    /**
//     * 根据id查找页面信息
//     *
//     * @param id
//     * @return
//     */
//    public Map getPageInfo(Long id) {
//        SingleResponse<Object> singleResponse = null;
//        try {
//            String fullName = "com.seeyon.udc.appservice.PageInfoAppService";
//            String methodName = "selectDetailById";
//
//            String appName = "udc";
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(id));
//            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            singleResponse = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), appName);
//
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
////                if (!isThrowException){
//                singleResponse = SingleResponse.from(null);
//                singleResponse.setCode(ErrorCode.BUSINESS_ERROR);
//                singleResponse.setMessage(exception.getMessage());
////                }
//            return null;
//        }
//
//        Object content = singleResponse.getData().getContent();
//        if(content != null) {
//            log.info("泛化调用(SingleResponse)获取结果：" + content.toString());
//        }
//
//        String contentJson = JsonUtils.toJson(content);
//        return JsonUtils.fromJson(contentJson,HashMap.class);
//    }
//
//    /**
//     *  保存/修改 udcNode
//     *
//     * @param udcNode
//     * @return
//     */
//    public boolean savePageInfo(Long id, JsonNode udcNode) {
//
//
//        SingleResponse<Object> singleResponse = null;
//        try {
//            //TODO 根据id获取pageInfoMap
//
//            Map pageInfoMap = getPageInfo(id);
////            pageInfoMap.put("appId","5610468451101918351");
////            pageInfoMap.put("id", "7846383812688670309");
////            pageInfoMap.put("urlType", "PC");
////            pageInfoMap.put("inParams", "[{\"masterEntityInfo\":\"edoc335172694483814428/com.seeyon.edoc335172694483814428.domain.entity.IssuedDocument\",\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"KxMO1rJIt2\",\"label\":\"实体\",\"code\":\"entity\",\"dataType\":\"DATA_SOURCE\",\"expressionValue\":[],\"list\":false,\"inKey\":\"entity\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"c0m1gK7cZXDXgKj\",\"label\":\"事项状态\",\"code\":\"affairState\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"affairState\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"preset_id\",\"label\":\"id\",\"code\":\"id\",\"dataType\":\"BIGINTEGER\",\"expressionValue\":[],\"inKey\":\"id\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"eOxFVU3XJcLoPvee\",\"label\":\"是否退回到发起者\",\"code\":\"isStartPerson\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"isStartPerson\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"SGkjnjrJb3WzSni\",\"label\":\"拟稿单位\",\"code\":\"draftUnit\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"draftUnit\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"czqGx5QrhvrrQZfh\",\"label\":\"拟稿部门\",\"code\":\"hostDepartment\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"hostDepartment\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"ndl9yDH5yrg4s81\",\"label\":\"发文机关\",\"code\":\"documentIssuingDepartment\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"documentIssuingDepartment\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"T5xhqv4Mojprzhe\",\"label\":\"印发机关\",\"code\":\"printingSendingDepartment\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"printingSendingDepartment\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"cdctFNDSLyPEdNb\",\"label\":\"唯一标识id\",\"code\":\"guid\",\"dataType\":\"STRING\",\"expressionValue\":[],\"inKey\":\"guid\"},{\"attrCmptProps\":{\"businessProps\":{}},\"required\":false,\"id\":\"x4HQJfqNDxqq5iOC\",\"label\":\"实体名称\",\"code\":\"entityName\",\"dataType\":\"STRING\",\"expressionValue\":[{\"type\":\"CONSTANT\",\"value\":\"IssuedDocument\",\"desc\":\"IssuedDocument\"}],\"inKey\":\"entityName\"}]");
////            pageInfoMap.put("inPortletParams", "[]");
////            pageInfoMap.put("publicAccess", false);
////            pageInfoMap.put("pageUrl", "dy8002982175438959568presetDocFormPage");
////            pageInfoMap.put("pageType", "SUBPAGE");
////            pageInfoMap.put("defaultOpenType", null);
////            pageInfoMap.put("description", "");
////            pageInfoMap.put("groupId", "0");
////            pageInfoMap.put("guid", "8002982175438959568");
////            pageInfoMap.put("relationGroupId", "-8054437866462472596");
////            pageInfoMap.put("sortNo", 1);
////            pageInfoMap.put("layoutId", "0");
////            pageInfoMap.put("layoutInfo", "");
////            pageInfoMap.put("referConfigParameters", new ArrayList<>());
////            pageInfoMap.put("defaultPlan", "[]");
//
//
//
//            //保存pageInfoMap
//            String fullName = "com.seeyon.udc.appservice.PageInfoAppService";
//            String methodName = "update";
//            pageInfoMap.put("pageSchema", udcNode.toString());
//
//            String appName = "udc";
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(pageInfoMap));
//            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            singleResponse = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), appName);
////                if (isThrowException) {
////                    DubboResponseUtil.dealResponse(singleResponse, appName);
////                }
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
////                if (!isThrowException){
//            singleResponse = SingleResponse.from(null);
//            singleResponse.setCode(ErrorCode.BUSINESS_ERROR);
//            singleResponse.setMessage(exception.getMessage());
////                }
//            return false;
//        }
//        log.info("泛化调用(SingleResponse)获取结果：" + JsonUtils.toJson(singleResponse));
//
//
//        return true;
//    }
//
//
//
//    /**
//     *
//     * 根据allUdcReferenceFullName 查询所有参照的映射关系
//     * key: entityFullName +"_"+ appName
//     * value: fullName
//     * @param allUdcReferenceFullName
//     * @return
//     */
//    public Map<String, String> getReferenceMap(Set<String> allUdcReferenceFullName) {
//        Map<String, String> resultMap = new HashMap<>();
//        for (String udcReferenceFullName : allUdcReferenceFullName) {
//            SingleResponse<Object> singleResponse = null;
//            try {
//                String fullName = "com.seeyon.udc.appservice.MetaDataAppServiceImpl";
//                String methodName = "selectReferAndAppMetadataByEntity";
//                Map pageInfoMap = new HashMap();
//                String[] split = udcReferenceFullName.split("_");
//                pageInfoMap.put("entityFullName", split[0]);
//                pageInfoMap.put("appName",split[1]);
//
//
//                AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//                appServiceInvokeDto.setInterfaceName(fullName);
//                appServiceInvokeDto.setMethodName(methodName);
//                appServiceInvokeDto.setParamJson(JsonUtils.toJson(pageInfoMap));
//                appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//                appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//                singleResponse = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), "udc");
//
//                Object content = singleResponse.getData().getContent();
//
//                ObjectMapper objectMapper = new ObjectMapper();
//
//                JsonNode contentNode = objectMapper.readTree(content.toString());
//                ArrayNode referMetaDataDtoListNode = (ArrayNode)contentNode.get("referMetaDataDtoList");
//                if(referMetaDataDtoListNode.size()>0){
//                    //获取 enitityFullname 对应的 fullName .(接口是fullName,但是没找到)
//                    JsonNode fullNameNode = referMetaDataDtoListNode.get(0).get("fullName");
//                    if(fullNameNode != null){
//
//                        resultMap.put(udcReferenceFullName, fullNameNode.asText());
//                    }
//                }
//
//
//            } catch (Exception exception) {
//                log.error("化调用异常：" + exception.getMessage(), exception);
////                if (!isThrowException){
//                singleResponse = SingleResponse.from(null);
//                singleResponse.setCode(ErrorCode.BUSINESS_ERROR);
//                singleResponse.setMessage(exception.getMessage());
////                }
//
//            }
//
//
//        }
//        return resultMap;
//
//    }
//
//
//
//
//    /**
//     * 获取公文中titleName  对应的ApplicationDto
//     */
//    public JsonNode getAppInfoByAppId(String appId){
//
//        //   Path: com.seeyon.udc.appservice.ApplicationAppServiceImpl
//        //    方法定义：PageResponse<ApplicationDto> selectPageByConditions(PageRequest request)
//        //    入参：平台标准condition查询，参数使用 hostAppName = edoc335172694483814428
//
//        SingleResponse<Object> response = null;
//        try {
//            String fullName = "com.seeyon.udc.appservice.ApplicationAppServiceImpl";
//            String methodName = "selectById";
//
//
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(appId));
//            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
//            response = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), "udc");
//
//            Object content = response.getData().getContent();
//            ObjectMapper objectMapper = new ObjectMapper();
//            return objectMapper.readTree(content.toString());
//
//
//
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
//        }
//
//        return null;
//    }
//
//
////    //获取所有主实体
////    public JsonNode getAppInfoByAppId(String appId){
////    selectPrimaryEntityByAppId
//
//    public  ArrayNode getGwEntityLists(String type){
//        ObjectMapper objectMapper = new ObjectMapper();
//        ArrayNode arrayNode = objectMapper.createArrayNode();
//
//        ObjectNode entityNode = objectMapper.createObjectNode();
//
//        switch (type) {
//            case "fwd":
//                entityNode.put("name","IssuedDocument");
//                entityNode.put("caption","发文");
//
//                break;
//            case "swd":
//
//                entityNode.put("name","ReceivedDocument");
//                entityNode.put("caption","收文");
//                break;
//            case "qbd":
//
//                entityNode.put("name","SignDocument");
//                entityNode.put("caption","签发");
//
//                break;
//            case "qsd":
//
//                entityNode.put("name","ExchangeDetail");
//                entityNode.put("caption","签收");
//                break;
//            default:
//                throw new RuntimeException("type is invalid:"+type);
//        }
//
//
//        arrayNode.add(entityNode);
//        return arrayNode;
//
//
//    }
//
////    /**
////     *  获取EntityLists
////     * @return
////     */
////    public ArrayNode getEntityLists(Long entityId){
////
////        ListResponse<Object> response = null;
////        try {
////            String fullName = "com.seeyon.boot.starter.extmetadata.appservice.UdcMetaDataAppV2Service";
////            String methodName = "selectAggEntityIncludeAssociation";
////
////
////            SingleRequest request = SingleRequest.from(entityId);
////
////            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
////            appServiceInvokeDto.setInterfaceName(fullName);
////            appServiceInvokeDto.setMethodName(methodName);
////            appServiceInvokeDto.setParamJson(JsonUtils.toJson(request));
////            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
////            appServiceInvokeDto.setCommandId(Ids.uuidString());
//////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
////            response = appServiceReliableInvoker.invokeForList(SingleRequest.from(appServiceInvokeDto), "udc");
////
////            ListData<Object> listData = response.getData();
////            List<Object> contentList = listData.getContent();
////            ObjectMapper objectMapper = new ObjectMapper();
////            ArrayNode contentListNode = (ArrayNode)objectMapper.readTree(contentList.toString());
////            return contentListNode;
////        } catch (Exception exception) {
////            log.error("化调用异常：" + exception.getMessage(), exception);
////        }
////
////        return null;
////
////    }
//
//    /**
//     *  获取EntityLists
//     * @return
//     */
//    public ArrayNode getEntityListsBak(Long entityId){
//
//        ListResponse<Object> response = null;
//        try {
//            String fullName = "com.seeyon.udc.appservice.MetaDataAppServiceImpl";
//            String methodName = "selectAggEntityIncludeAssociationByEntityId";
//
//            SingleRequest request = SingleRequest.from(entityId);
//
//            AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//            appServiceInvokeDto.setInterfaceName(fullName);
//            appServiceInvokeDto.setMethodName(methodName);
//            appServiceInvokeDto.setParamJson(JsonUtils.toJson(request));
//            appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//            appServiceInvokeDto.setCommandId(Ids.uuidString());
////                log.info("泛化调用参数信息：" + JsonUtils.toJson(appServiceInvokeDto));
//            response = appServiceReliableInvoker.invokeForList(SingleRequest.from(appServiceInvokeDto), "udc");
//
//            ListData<Object> listData = response.getData();
//            List<Object> contentList = listData.getContent();
//            ObjectMapper objectMapper = new ObjectMapper();
//            ArrayNode contentListNode = (ArrayNode)objectMapper.readTree(contentList.toString());
//            return contentListNode;
//        } catch (Exception exception) {
//            log.error("化调用异常：" + exception.getMessage(), exception);
//        }
//
//        return null;
//
//    }
//
//    public Object createGwPageInfoCore(String titleName, String gongwenType) {
//
//        String formType = "";
//        switch (gongwenType) {
//            case "fwd":
//                formType = "ISSUED_DOCUMENT";
//                break;
//            case "swd":
//                formType = "RECEIVED_DOCUMENT";
//                break;
//            case "qbd":
//                formType = "SIGNED_DOCUMENT";
//                break;
//            case "qsd":
//                formType = "SIGN_PAGE";
//                break;
//            default:
//                throw new RuntimeException("gongwenType is invalid:"+gongwenType);
//        }
//
//        SingleResponse<Object> response = null;
//
//        String fullName = "com.seeyon.edoc335172694483814428.appservice.microflow.MicroFlowAppServiceImpl";
//        String methodName = "createEdocForm";
//
//
//        Map<String, Object> params = new HashMap<>();
//        //入参
//        params.put("name", titleName);
//        params.put("actionType", "INSERT");
//        params.put("formType", formType);
//        params.put("isEnabled", "ENABLE");
//        SingleRequest singleRequest = SingleRequest.from(params);
//
//        AppServiceInvokeDto appServiceInvokeDto = new AppServiceInvokeDto();
//        appServiceInvokeDto.setInterfaceName(fullName);
//        appServiceInvokeDto.setMethodName(methodName);
//        appServiceInvokeDto.setParamJson(JsonUtils.toJson(singleRequest));
//        appServiceInvokeDto.setParamType(SingleRequest.class.getName());
//        appServiceInvokeDto.setCommandId(Ids.uuidString());
//        response = appServiceReliableInvoker.invoke(SingleRequest.from(appServiceInvokeDto), "udc");
//
//        Object content = response.getData().getContent();
//
//
//        return content;
//
//
//    }
//
//    public Long createGwPageInfo(String titleName, String gongwenType,int num) {
//
//        if(num > 1 && num < 200){
//            titleName = titleName+"("+num+")";
//        } else {
//            throw new BusinessException("名称已经存在, 且超过200个最大上限");
//        }
//
//
//        Object gwPageInfoSingleData = createGwPageInfoCore(titleName,gongwenType);
//        String gwPageInfoSingleDataStr = gwPageInfoSingleData.toString();
//        if(StringUtils.isBlank(gwPageInfoSingleDataStr ) ){
//            return null;
//        } else if( gwPageInfoSingleDataStr.contains("名称已经存在")){//TODO. 如何判定 "名称已经存在"
//            return createGwPageInfo(titleName,gongwenType,num+1);
//        }
//
//        return Long.getLong(gwPageInfoSingleDataStr);
//    }
//
//
//    /**
//     *  获取EntityLists
//     * @return
//     */
//    public ArrayNode getEntityListsMock(){
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            String entityListsStr = readJsonFromFile("ai-form/entityListsStr.txt");
//            JsonNode appInfoNode = objectMapper.readTree(entityListsStr);
//            return (ArrayNode)appInfoNode;
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private String readJsonFromFile(String path) throws IOException {
//        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
//            if (is == null) {
//                throw new IOException("File not found: " + path);
//            }
//            return IOUtils.toString(is, StandardCharsets.UTF_8);
//        }
//    }
////
////
//    /**
//     * //TODO 获取appInfo
//     */
//    public JsonNode getAppInfoMock(String appName){
//
//        String appInfoStr = "{\n" +
//                "        \"copVersion\": \"5.0\",\n" +
//                "        \"appVersion\": \"1.0.0\",\n" +
//                "        \"caption\": \"测试发文单gaojz\",\n" +
//                "        \"description\": \"\",\n" +
//                "        \"icon\": \"\",\n" +
//                "        \"iconBgColor\": \"\",\n" +
//                "        \"categoryId\": null,\n" +
//                "        \"id\": \"2832174142784948425\",\n" +
//                "        \"name\": \"dynamic2832174142784948425\",\n" +
//                "        \"sourceType\": \"FORM\",\n" +
//                "        \"enableRelationEntitySelect\": false,\n" +
//                "        \"enableEditEntityTableName\": true,\n" +
//                "        \"featureType\": \"ONLYPAGE\",\n" +
//                "        \"style\": \"\",\n" +
//                "        \"terminalType\": \"ALL\",\n" +
//                "        \"hostAppName\": \"edoc335172694483814428\",\n" +
//                "        \"scalabilityOptionId\": \"0\",\n" +
//                "        \"guid\": \"-2683527763458702567\",\n" +
//                "        \"portalId\": \"2832174142784948425\",\n" +
//                "        \"publishState\": \"PUBLISHED\",\n" +
//                "        \"systemTheme\": true,\n" +
//                "        \"appTheme\": \"-1\"\n" +
//                "    }";
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode appInfoNode = null;
//        try {
//            appInfoNode = objectMapper.readTree(appInfoStr);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return appInfoNode;
//    }
//
////
////
////    /**
////     * 根据allUdcReferenceFullName 查询所有参照的映射关系
////     * 根据allUdcReferenceFullName 查询所有参照的映射关系
////     * key: appName +"_"+ entityFullName
////     * value: fullName
////     * @param allUdcReferenceFullName
////     * @return
////     */
////    public Map<String, String> getReferenceMapMock(List<String> allUdcReferenceFullName) {
////        ObjectMapper objectMapper = new ObjectMapper();
////        Map<String,String> result = new HashMap();
////        Map<String, Object> defaultResult = new HashMap<>();
////        // 构建文件路径
////        String mockFilePath = "D:\\workspace_ai\\app-common\\app-common-test\\src\\test\\resources\\ai-form\\mock\\";
////
////        // 读取文件内容
////        File mockFolder = new File(mockFilePath);
////        if(mockFolder == null){
////            //TODO: 这是临时策略
////            return new HashMap<>();
////        }
////        //获取mockFile下所有文件
////        File[] files = mockFolder.listFiles();
////        for(File file: files){
////            String fileName = file.getName();
////
////            String entityFullName = fileName.split("_")[0];
////            File mockFile = new File(mockFilePath+fileName);
////
////            fileName = fileName.replace(".json", "");
////            // 使用 ObjectMapper 读取 JSON 文件
////            Map<String, Object> fileContent = null;
////            try {
////                fileContent = objectMapper.readValue(mockFile, Map.class);
////            } catch (IOException e) {
////                throw new RuntimeException(e);
////            }
////
////            // 从 content 字段中获取数据
////            Map<String, Object> content = (Map<String, Object>) fileContent.get("content");
////            if (content == null) {
////                content = fileContent; // 如果没有 content 字段,就使用整个文件内容
////            }
////            List<Map<String, Object>> referMetaDataDtoList = (List<Map<String, Object>>) content.get("referMetadataList");
////
////            // 如果列表为空,创建一个默认项
////            if (referMetaDataDtoList == null || referMetaDataDtoList.isEmpty()) {
////                continue;
////            }
////
//////            defaultResult.put("referMetaDataDtoList", referMetaDataDtoList);
////            Object fullNameValue = referMetaDataDtoList.get(0).get("fullName");
////            result.put(fileName,fullNameValue==null ? "": fullNameValue.toString());
////        }
////        return result;
////
////
////    }
//
//}
