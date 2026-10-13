package com.lqx.health_check_system.service;

import com.lqx.health_check_system.entity.AiSuggestionCache;

import java.util.Map;

public interface AiCacheService {

    AiSuggestionCache getCache(Long examId, String dataHash);

    void saveCache(Long examId, String dataHash, String conclusion, String advice, String modelType);

    String calculateDataHash(Map<String, Object> reportData);

    void cleanExpiredCache();
}