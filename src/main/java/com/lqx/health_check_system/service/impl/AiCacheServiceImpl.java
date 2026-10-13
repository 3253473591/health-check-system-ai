package com.lqx.health_check_system.service.impl;

import com.lqx.health_check_system.config.AiProperties;
import com.lqx.health_check_system.entity.AiSuggestionCache;
import com.lqx.health_check_system.mapper.AiSuggestionCacheMapper;
import com.lqx.health_check_system.service.AiCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCacheServiceImpl implements AiCacheService {

    private final AiSuggestionCacheMapper cacheMapper;
    private final AiProperties aiProperties;

    @Override
    public AiSuggestionCache getCache(Long examId, String dataHash) {
        return cacheMapper.selectValidCache(examId, dataHash);
    }

    @Override
    public void saveCache(Long examId, String dataHash, String conclusion, String advice, String modelType) {
        // 先删除旧缓存
        cacheMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiSuggestionCache>()
                .eq(AiSuggestionCache::getExamId, examId));

        AiSuggestionCache cache = new AiSuggestionCache();
        cache.setExamId(examId);
        cache.setDataHash(dataHash);
        cache.setAiConclusion(conclusion);
        cache.setAiAdvice(advice);
        cache.setModelType(modelType);
        cache.setCreatedAt(LocalDateTime.now());
        cache.setExpiresAt(LocalDateTime.now().plusHours(aiProperties.getCacheTtlHours()));

        cacheMapper.insert(cache);
        log.info("AI建议已缓存，examId={}, 有效期{}小时", examId, aiProperties.getCacheTtlHours());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String calculateDataHash(Map<String, Object> reportData) {
        StringBuilder sb = new StringBuilder();
        sb.append(reportData.getOrDefault("examId", "")).append("|");
        sb.append(reportData.getOrDefault("age", "")).append("|");
        sb.append(reportData.getOrDefault("gender", ""));

        Object tableObj = reportData.get("dataTable");
        if (tableObj instanceof List) {
            List<?> table = (List<?>) tableObj;
            int limit = Math.min(table.size(), 10);
            for (int i = 0; i < limit; i++) {
                Object rowObj = table.get(i);
                if (rowObj instanceof Map) {
                    Map<String, Object> row = (Map<String, Object>) rowObj;
                    Object itemValue = row.get("itemValue");
                    sb.append(itemValue != null ? itemValue.toString() : "").append(",");
                }
            }
            sb.append("|count=").append(table.size());
        }

        return md5(sb.toString());
    }

    @Override
    public void cleanExpiredCache() {
        cacheMapper.cleanExpiredCache();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}