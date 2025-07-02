package com.seeyon.ai.ocrprocess.appservice;


import com.seeyon.ai.ocrprocess.service.AiFormAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author pb
 */
@Service
@Slf4j
public class AiFormAssistantAppService {

    @Autowired
    private AiFormAssistantService aiFormAssistantService;

    public List<String> upload(MultipartFile file, HttpServletRequest request) {
        String apiKey = request.getHeader("api-key");
        return aiFormAssistantService.upload(file, apiKey);
    }


}
