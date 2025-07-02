package com.seeyon.ai.ocrprocess.controller;

import com.seeyon.ai.common.base.BaseController;
import com.seeyon.ai.ocrprocess.appservice.AiEdocAppService;
import com.seeyon.ai.ocrprocess.appservice.AiFormAssistantAppService;
import com.seeyon.ai.ocrprocess.appservice.AiFormAssistantAsyncAppService;
import com.seeyon.ai.ocrprocess.form.request.EdocIdentifyRequest;
import com.seeyon.ai.ocrprocess.form.request.InformationRecordRequest;
import com.seeyon.ai.ocrprocess.form.request.TransferRequest;
import com.seeyon.ai.ocrprocess.form.request.UdcFormGenerate;
import com.seeyon.ai.ocrprocess.form.response.AiFormFlowResponse;
import com.seeyon.ai.ocrprocess.form.response.DataHistoryResponse;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponseNew;
import com.seeyon.ai.ocrprocess.service.CacheService;
import com.seeyon.boot.enums.StandardAppId;
import com.seeyon.boot.transport.ListResponse;
import com.seeyon.boot.transport.SingleResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * @author pb
 */
@RestController
@RequestMapping( "ai-low-code-assistant/form/assistant/async")
@Slf4j
//@Tag(name = "表单助手异步管理访问接口")
public class AiFormAssistantAsyncController extends BaseController {

    @Autowired
    private AiFormAssistantAsyncAppService aiFormAssistantAsyncAppService;
    @Autowired
    private AiEdocAppService aiEdocAppService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    AiFormAssistantAppService aiFormAssistantAppService;

    @PostMapping("/dsl/transfer")
    @Operation(summary = "transfer")
    public DataStandardResponseNew transfer(@RequestBody TransferRequest transferRequest, HttpServletRequest request) {
        return aiFormAssistantAsyncAppService.transfer(transferRequest,request);
    }

    @PostMapping("/ocr/identify")
//    @Operation(summary = "ocr识别")
    public String ocrIdentify(@RequestBody UdcFormGenerate ocrIdentifyRequest, HttpServletRequest request) {
        return String.valueOf(aiFormAssistantAsyncAppService.ocrIdentify(ocrIdentifyRequest,request));
    }

    @PostMapping("edoc/identify")
//    @Operation(summary = "公文识别")
    public String edocIdentify(@RequestBody EdocIdentifyRequest edocIdentifyRequest, HttpServletRequest request) {
        return String.valueOf(aiEdocAppService.edocIdentify(edocIdentifyRequest,request));
    }


    @GetMapping("/getFlow")
//    @Operation(summary = "获取页面助手状态")
    public AiFormFlowResponse getFlow(@RequestParam(value = "id") String id) {
        return aiFormAssistantAsyncAppService.getFlow(id);
    }


    @PostMapping("/upload")
//    @Operation(summary = "图片上传")
    public List<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return aiFormAssistantAsyncAppService.upload(file, request);
    }

    @GetMapping("data/info")
    public DataHistoryResponse dataInfo(){
        return aiFormAssistantAsyncAppService.dataInfo();
    }
    @PostMapping("/information/record")
    public void informationRecord(@RequestBody InformationRecordRequest informationRecordRequest, HttpServletRequest request) {
        aiFormAssistantAsyncAppService.informationRecord(informationRecordRequest,request);
    }


}
