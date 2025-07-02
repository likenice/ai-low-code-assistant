package com.seeyon.ai.ocrprocess.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private static final String CACHE_NAME = "udc_from_assistant";

    /**
     * 写入/更新缓存
     */
    @CachePut(value = CACHE_NAME, key = "#p0")
    public String saveToCache(String id, String data) {
        return data; // 返回值会被自动缓存
    }

    /**
     * 查询缓存
     */
    @Cacheable(value = CACHE_NAME, key = "#p0")
    public String getFromCache(String id) {
        return null; // 如果缓存不存在，会执行此方法（实际业务中可返回数据库查询）
    }

    /**
     * 删除缓存
     */
    @CacheEvict(value = CACHE_NAME, key = "#p0")
    public void deleteFromCache(String id) {
        // 方法执行后会自动删除缓存
    }
}
