package com.lqx.health_check_system.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqx.health_check_system.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnythingLLMClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AiProperties aiProperties;

    public boolean streamChat(String prompt,
                              Consumer<String> tokenConsumer,
                              Consumer<Exception> errorConsumer) {
        HttpURLConnection conn = null;
        try {
            String urlStr = String.format("%s/api/v1/workspace/%s/stream-chat",
                    aiProperties.getUrl(), aiProperties.getWorkspace());
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiProperties.getApiKey());
            conn.setConnectTimeout(aiProperties.getConnectTimeout());
            conn.setReadTimeout(aiProperties.getReadTimeout());

            Map<String, Object> body = new HashMap<>();
            body.put("message", prompt);
            body.put("mode", "chat");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || !line.startsWith("data:")) continue;

                    String jsonData = line.substring(5).trim();
                    try {
                        JsonNode node = MAPPER.readTree(jsonData);

                        if (node.has("textResponse")) {
                            String token = node.path("textResponse").asText("");
                            if (!token.isEmpty()) {
                                tokenConsumer.accept(token);
                            }
                        }

                        if (shouldClose(node)) {
                            return true;
                        }
                    } catch (Exception e) {
                        log.warn("解析AnythingLLM响应失败: {}", jsonData, e);
                    }
                }
            }
            return true;

        } catch (Exception e) {
            log.error("AnythingLLM调用失败", e);
            errorConsumer.accept(e);
            return false;
        } finally {
            Optional.ofNullable(conn).ifPresent(HttpURLConnection::disconnect);
        }
    }

    private boolean shouldClose(JsonNode node) {
        boolean closed = node.path("close").asBoolean(false);
        boolean hasError = node.path("error").asBoolean(false);
        String textResponse = node.path("textResponse").asText("");

        return closed || hasError || (!textResponse.isEmpty() && closed);
    }
}