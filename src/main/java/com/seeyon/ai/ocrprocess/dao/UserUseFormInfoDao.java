package com.seeyon.ai.ocrprocess.dao;


import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.entity.UserUseFormInfo;
import com.seeyon.ai.ocrprocess.form.AiFormFLowInfo;
import com.seeyon.ai.ocrprocess.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserUseFormInfoDao  {
    @Autowired
    CacheService cacheService;

    public void create(UserUseFormInfo userUseFormInfo){

        cacheService.saveToCache(userUseFormInfo.getTaskId()+"_dataInfo", JSONUtil.toJsonStr(userUseFormInfo));
    }

    public void update(UserUseFormInfo userUseFormInfo){
        cacheService.saveToCache(userUseFormInfo.getTaskId()+"_dataInfo", JSONUtil.toJsonStr(userUseFormInfo));
    }
    public UserUseFormInfo selectOneById(String id){
        String fromCache = cacheService.getFromCache(id + "_dataInfo");
        UserUseFormInfo userUseFormInfo = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            userUseFormInfo = objectMapper.readValue(fromCache, UserUseFormInfo.class);
        } catch (JsonProcessingException e) {
            throw new PlatformException("Failed to parse AiFormFLowInfo from cache", e);
        }
        return userUseFormInfo;
    }

}
