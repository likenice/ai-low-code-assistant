package com.seeyon.ai.ocrprocess.appservice;


import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.form.request.InformationRecordRequest;
import com.seeyon.ai.ocrprocess.form.request.TransferRequest;
import com.seeyon.ai.ocrprocess.form.request.UdcFormGenerate;
import com.seeyon.ai.ocrprocess.form.response.AiFormFlowResponse;
import com.seeyon.ai.ocrprocess.form.response.DataHistoryResponse;
import com.seeyon.ai.ocrprocess.form.response.DataStandardResponseNew;
import com.seeyon.ai.ocrprocess.service.AiFormAssistantAsyncService;
import com.seeyon.ai.ocrprocess.service.AiFormAssistantService;
import com.sun.org.apache.bcel.internal.generic.NEW;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

/**
 * @author pb
 */
@Service
@Slf4j
public class AiFormAssistantAsyncAppService {


    @Autowired
    private AiFormAssistantAsyncService aiFormAssistantAsyncService;
    @Autowired
    private AiFormAssistantService aiFormAssistantService;


    public Long ocrIdentify(UdcFormGenerate ocrIdentifyRequest, HttpServletRequest request) {
        return aiFormAssistantAsyncService.ocrIdentify(ocrIdentifyRequest,request);

    }


    public DataStandardResponseNew transfer(TransferRequest transferRequest,HttpServletRequest request) {
        return aiFormAssistantAsyncService.pageTransfer(transferRequest, new UserUseFormInfo(), new HashMap<>(), new HashMap<>(), new AiFormFLowInfo(),request.getHeader("api-key"));
    }

    public AiFormFlowResponse getFlow(String id) {
        return aiFormAssistantAsyncService.getFlow(id);
    }

    public List<String> upload(MultipartFile file, HttpServletRequest request) {
        String apiKey = request.getHeader("api-key");
        return aiFormAssistantService.upload(file, apiKey);
    }


    public DataHistoryResponse dataInfo() {
        return aiFormAssistantAsyncService.dataInfo();
    }

    public void informationRecord(InformationRecordRequest informationRecordRequest, HttpServletRequest request) {
        aiFormAssistantAsyncService.informationRecord(informationRecordRequest,request);
    }
}
