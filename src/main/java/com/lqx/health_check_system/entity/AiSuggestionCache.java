package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_suggestion_cache")
public class AiSuggestionCache {

    @TableId(type = IdType.AUTO)
    private Long cacheId;

    private Long examId;

    private String dataHash;

    private String aiConclusion;

    private String aiAdvice;

    private String modelType;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}