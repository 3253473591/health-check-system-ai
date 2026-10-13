package com.lqx.health_check_system.service.impl;

import com.lqx.health_check_system.client.AnythingLLMClient;
import com.lqx.health_check_system.client.OllamaClient;
import com.lqx.health_check_system.service.AiGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerateServiceImpl implements AiGenerateService {

    private final OllamaClient ollamaClient;
    private final AnythingLLMClient anythingLLMClient;

    @Override
    public boolean generateWithOllama(String prompt, Consumer<String> onToken, Consumer<Exception> onError) {
        log.info("使用Ollama生成报告");
        return ollamaClient.streamGenerate(prompt, onToken, onError);
    }

    @Override
    public boolean generateWithRag(String prompt, Consumer<String> onToken, Consumer<Exception> onError) {
        log.info("使用RAG(AnythingLLM)生成报告");
        return anythingLLMClient.streamChat(prompt, onToken, onError);
    }
}