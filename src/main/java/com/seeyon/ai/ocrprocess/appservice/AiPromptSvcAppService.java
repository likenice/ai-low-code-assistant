package com.seeyon.ai.ocrprocess.appservice;

import com.seeyon.ai.ocrprocess.form.AiPromptSvcCallDto;
import com.seeyon.ai.ocrprocess.service.AiPromptSvcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@Slf4j
public class AiPromptSvcAppService {
    @Autowired
    private AiPromptSvcService aiPromptSvcService;

    /**
     * 提示词服务方法
     *
     * @param aiPromptSvcCallDto
     * @return
     */
    public Object promptCallService(AiPromptSvcCallDto aiPromptSvcCallDto, Map llmInitialAnalysisMap, Map llmConfidenceMap) {
        return aiPromptSvcService.promptCallService(aiPromptSvcCallDto,llmInitialAnalysisMap,llmConfidenceMap);
    }



}
