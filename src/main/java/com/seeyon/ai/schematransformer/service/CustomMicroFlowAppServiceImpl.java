//package com.seeyon.ai.schematransformer.service;
//
//import com.seeyon.boot.annotation.AppService;
//import com.seeyon.boot.annotation.AppServiceOperation;
//import com.seeyon.boot.annotation.Custom;
//import com.seeyon.boot.annotation.CustomAttribute;
//import com.seeyon.boot.transport.SingleRequest;
//import com.seeyon.boot.transport.SingleResponse;
//import com.seeyon.udc.common.constant.MicroFlowCommonConstant;
//
//@AppService(value = "自定义微流程实现类", customs = @Custom(properties = @CustomAttribute(name = MicroFlowCommonConstant.APP_SERVICE_ANNOTATION_CUSTOM_KEY, value = "true")) )
//public class CustomMicroFlowAppServiceImpl  {
//
//    @AppServiceOperation(value = "xxxx")
//    public SingleResponse<Void> clickHandler(SingleRequest<Long> request) {
//        // --snip--
//        return SingleResponse.ok();
//    }
//}