package com.seeyon.ai.ocrprocess.appservice;


import com.seeyon.ai.ocrprocess.form.request.EdocIdentifyRequest;
import com.seeyon.ai.ocrprocess.service.AiEdocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author pb
 */
@Service
@Slf4j
public class AiEdocAppService {


    @Autowired
    private AiEdocService aiEdocService;

    public Long edocIdentify(EdocIdentifyRequest edocIdentifyRequest, HttpServletRequest request) {
       return aiEdocService.edocIdentify(edocIdentifyRequest,request);
    }
}
