package com.lqx.health_check_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class AiProperties {
    private boolean enabled = false;
    private String url = "http://127.0.0.1:3001";
    private String apiKey = "";
    private String workspace = "health-check";

    private int connectTimeout = 10000;
    private int readTimeout = 300000;
    private int streamIdleTimeout = 30000;
    private int cacheTtlHours = 24;
}