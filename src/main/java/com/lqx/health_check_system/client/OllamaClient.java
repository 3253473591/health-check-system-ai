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
public class OllamaClient {

    private static final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
    private static final String MODEL_NAME = "deepseek-r1:8b-0528-qwen3-q8_0";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiProperties aiProperties;

    public boolean streamGenerate(String prompt,
                                  Consumer<String> tokenConsumer,
                                  Consumer<Exception> errorConsumer) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(OLLAMA_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(aiProperties.getConnectTimeout());
            conn.setReadTimeout(aiProperties.getReadTimeout());

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", MODEL_NAME);
            requestMap.put("prompt", prompt);
            requestMap.put("stream", true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(MAPPER.writeValueAsString(requestMap).getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonNode node = MAPPER.readTree(line);
                        String token = node.path("response").asText("");
                        boolean done = node.path("done").asBoolean(false);

                        if (!token.isEmpty()) {
                            tokenConsumer.accept(token);
                        }

                        if (done) {
                            return true;
                        }
                    } catch (Exception e) {
                        log.warn("解析Ollama响应行失败: {}", line, e);
                    }
                }
            }
            return true;

        } catch (Exception e) {
            log.error("Ollama调用失败", e);
            errorConsumer.accept(e);
            return false;
        } finally {
            Optional.ofNullable(conn).ifPresent(HttpURLConnection::disconnect);
        }
    }
}