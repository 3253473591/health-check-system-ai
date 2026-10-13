package com.lqx.health_check_system.service.impl;

import com.lqx.health_check_system.config.AiProperties;
import com.lqx.health_check_system.entity.AiSuggestionCache;
import com.lqx.health_check_system.service.AiCacheService;
import com.lqx.health_check_system.service.AiGenerateService;
import com.lqx.health_check_system.service.AiService;
import com.lqx.health_check_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final long EMITTER_TIMEOUT = 5 * 60 * 1000L;
    private static final String EVENT_TOKEN = "token";
    private static final String EVENT_DONE = "done";
    private static final String EVENT_ERROR = "error";
    private static final String EVENT_TIMEOUT = "timeout";
    private static final String EVENT_FALLBACK = "fallback";
    private static final String EVENT_CACHE = "cache";

    private final ReportService reportService;
    private final AiGenerateService aiGenerateService;
    private final AiPromptService aiPromptService;
    private final AiCacheService aiCacheService;
    private final AiProperties aiProperties;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter generateReport(Long examId) {
        Map<String, Object> data = reportService.getReportPageData(examId);
        String dataHash = aiCacheService.calculateDataHash(data);
        AiSuggestionCache cache = aiCacheService.getCache(examId, dataHash);
        if (cache != null) {
            log.info("命中AI缓存，examId={}", examId);
            return streamFromCache(cache);
        }
        String prompt = aiPromptService.buildNormalPrompt(data);
        return createEmitter(examId, prompt, false, dataHash);
    }

    private void createEmitterInternal(SseEmitter emitter, Long examId, String prompt, boolean useRag, String dataHash, boolean allowFallback) {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
        StringBuilder fullContent = new StringBuilder();

        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                sendEvent(emitter, "timeout", "生成超时");
                emitter.complete();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                Consumer<String> tokenConsumer = token -> {
                    lastActivity.set(System.currentTimeMillis());
                    fullContent.append(token);
                    sendEvent(emitter, "token", token);
                };

                boolean success;
                String modelType = useRag ? "RAG" : "OLLAMA";

                if (useRag) {
                    success = aiGenerateService.generateWithRag(prompt, tokenConsumer, error -> log.error("RAG错误", error));
                    if (!success && allowFallback) {
                        sendEvent(emitter, "fallback", "切换至本地模型...");
                        modelType = "OLLAMA";
                        success = aiGenerateService.generateWithOllama(prompt, tokenConsumer, error -> {
                            if (completed.compareAndSet(false, true)) {
                                sendEvent(emitter, "error", error.getMessage());
                            }
                        });
                    }
                } else {
                    success = aiGenerateService.generateWithOllama(prompt, tokenConsumer, error -> {
                        if (completed.compareAndSet(false, true)) {
                            sendEvent(emitter, "error", error.getMessage());
                        }
                    });
                }

                if (success && completed.compareAndSet(false, true)) {
                    saveToCache(fullContent.toString(), examId, dataHash, modelType);
                    sendEvent(emitter, "done", "");
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("生成异常", e);
                if (completed.compareAndSet(false, true)) {
                    sendEvent(emitter, "error", e.getMessage());
                    emitter.complete();
                }
            }
        });
    }

    @Override
    public SseEmitter generateReportWithRag(Long examId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> data = reportService.getReportPageData(examId);
                String dataHash = aiCacheService.calculateDataHash(data);

                AiSuggestionCache cache = aiCacheService.getCache(examId, dataHash);
                if (cache != null) {
                    log.info("命中AI缓存（RAG入口），examId={}", examId);
                    streamCacheToEmitter(emitter, cache);
                    return;
                }

                boolean useRag = aiProperties.isEnabled();
                String prompt = useRag
                        ? aiPromptService.buildRagPrompt(data)
                        : aiPromptService.buildNormalPrompt(data);

                if (!useRag) {
                    notifyFallback(emitter, "RAG服务未启用，已切换至本地模型");
                }

                createEmitterInternal(emitter, examId, prompt, useRag, dataHash, true);

            } catch (Exception e) {
                log.error("生成失败", e);
                try {
                    emitter.send(SseEmitter.event().name(EVENT_ERROR).data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private SseEmitter streamFromCache(AiSuggestionCache cache) {
        SseEmitter emitter = new SseEmitter(30000L);

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name(EVENT_CACHE).data("true"));

                String conclusion = cache.getAiConclusion() != null ? cache.getAiConclusion() : "";
                String advice = cache.getAiAdvice() != null ? cache.getAiAdvice() : "";

                String fullText;
                if (!advice.isEmpty()) {
                    fullText = "【体检结论】\n" + conclusion + "\n\n【健康建议】\n" + advice;
                } else {
                    fullText = conclusion;
                }

                String[] lines = fullText.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    emitter.send(SseEmitter.event().name(EVENT_TOKEN).data(lines[i]));
                    if (i < lines.length - 1) {
                        emitter.send(SseEmitter.event().name(EVENT_TOKEN).data("\n"));
                    }
                }

                emitter.send(SseEmitter.event().name(EVENT_DONE).data(""));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void streamCacheToEmitter(SseEmitter emitter, AiSuggestionCache cache) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_CACHE).data("true"));

            String conclusion = cache.getAiConclusion() != null ? cache.getAiConclusion() : "";
            String advice = cache.getAiAdvice() != null ? cache.getAiAdvice() : "";

            String fullText;
            if (!advice.isEmpty()) {
                fullText = "【体检结论】\n" + conclusion + "\n\n【健康建议】\n" + advice;
            } else {
                fullText = conclusion;
            }

            String[] lines = fullText.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                emitter.send(SseEmitter.event().name(EVENT_TOKEN).data(lines[i]));
                if (i < lines.length - 1) {
                    emitter.send(SseEmitter.event().name(EVENT_TOKEN).data("\n"));
                }
            }

            emitter.send(SseEmitter.event().name(EVENT_DONE).data(""));
            emitter.complete();
        } catch (Exception e) {
            log.error("streamCacheToEmitter 失败", e);
            emitter.completeWithError(e);
        }
    }

    private SseEmitter createEmitter(Long examId, String prompt, boolean useRag, String dataHash) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
        StringBuilder fullContent = new StringBuilder();

        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                log.warn("生成超时，examId={}", examId);
                sendEvent(emitter, EVENT_TIMEOUT, "生成超时，请稍后重试或联系管理员");
                emitter.complete();
            }
        });

        emitter.onCompletion(() -> completed.set(true));
        emitter.onError(e -> log.error("Emitter错误，examId={}", examId, e));

        executor.execute(() -> {
            Consumer<String> tokenConsumer = token -> {
                lastActivity.set(System.currentTimeMillis());
                fullContent.append(token);
                sendEvent(emitter, EVENT_TOKEN, token);
            };

            boolean success;
            String modelType = useRag ? "RAG" : "OLLAMA";

            if (useRag) {
                success = generateWithRag(emitter, prompt, completed, lastActivity, tokenConsumer);
            } else {
                success = generateWithOllama(emitter, prompt, completed, lastActivity, tokenConsumer);
            }

            if (success && completed.compareAndSet(false, true)) {
                saveToCache(fullContent.toString(), examId, dataHash, modelType);
                sendEvent(emitter, EVENT_DONE, "");
                emitter.complete();
            }
        });

        startIdleChecker(emitter, completed, lastActivity);
        return emitter;
    }

    private SseEmitter createEmitterWithFallback(Long examId, String prompt, String dataHash) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
        StringBuilder fullContent = new StringBuilder();

        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                log.warn("RAG生成超时，准备降级到Ollama，examId={}", examId);
                sendEvent(emitter, EVENT_TIMEOUT, "RAG响应超时，切换至本地模型...");
                executor.execute(() -> {
                    try {
                        Thread.sleep(500);
                        String fallbackPrompt = aiPromptService.buildNormalPrompt(
                                reportService.getReportPageData(examId));
                        StringBuilder fallbackContent = new StringBuilder();

                        Consumer<String> tokenConsumer = token -> {
                            lastActivity.set(System.currentTimeMillis());
                            fallbackContent.append(token);
                            sendEvent(emitter, EVENT_TOKEN, token);
                        };

                        boolean success = generateWithOllama(emitter, fallbackPrompt, new AtomicBoolean(false), lastActivity, tokenConsumer);
                        if (success && completed.compareAndSet(false, true)) {
                            saveToCache(fallbackContent.toString(), examId, dataHash, "OLLAMA");
                            sendEvent(emitter, EVENT_DONE, "");
                            emitter.complete();
                        }
                    } catch (Exception e) {
                        log.error("降级生成失败", e);
                        sendError(emitter, "降级生成失败: " + e.getMessage());
                    }
                });
            }
        });

        emitter.onCompletion(() -> completed.set(true));
        emitter.onError(e -> log.error("Emitter错误，examId={}", examId, e));

        executor.execute(() -> {
            Consumer<String> tokenConsumer = token -> {
                lastActivity.set(System.currentTimeMillis());
                fullContent.append(token);
                sendEvent(emitter, EVENT_TOKEN, token);
            };

            boolean success = generateWithRag(emitter, prompt, completed, lastActivity, tokenConsumer);

            if (!success && completed.compareAndSet(false, false)) {
                log.info("RAG调用失败，执行自动降级，examId={}", examId);
                sendEvent(emitter, EVENT_FALLBACK, "知识库服务异常，切换至本地模型...");
                try {
                    Thread.sleep(500);
                    String fallbackPrompt = aiPromptService.buildNormalPrompt(
                            reportService.getReportPageData(examId));
                    StringBuilder fallbackContent = new StringBuilder();

                    Consumer<String> fallbackConsumer = token -> {
                        lastActivity.set(System.currentTimeMillis());
                        fallbackContent.append(token);
                        sendEvent(emitter, EVENT_TOKEN, token);
                    };

                    boolean ollamaSuccess = generateWithOllama(emitter, fallbackPrompt, completed, lastActivity, fallbackConsumer);
                    if (ollamaSuccess && completed.compareAndSet(false, true)) {
                        saveToCache(fallbackContent.toString(), examId, dataHash, "OLLAMA");
                        sendEvent(emitter, EVENT_DONE, "");
                        emitter.complete();
                    }
                } catch (Exception e) {
                    log.error("降级生成失败", e);
                    sendError(emitter, "降级生成失败: " + e.getMessage());
                }
            } else if (success && completed.compareAndSet(false, true)) {
                saveToCache(fullContent.toString(), examId, dataHash, "RAG");
                sendEvent(emitter, EVENT_DONE, "");
                emitter.complete();
            }
        });

        startIdleChecker(emitter, completed, lastActivity);
        return emitter;
    }

    private boolean generateWithRag(SseEmitter emitter, String prompt, AtomicBoolean completed, AtomicLong lastActivity, Consumer<String> tokenConsumer) {
        return aiGenerateService.generateWithRag(prompt, tokenConsumer, error -> log.error("RAG生成错误: {}", error.getMessage()));
    }

    private boolean generateWithOllama(SseEmitter emitter, String prompt, AtomicBoolean completed, AtomicLong lastActivity, Consumer<String> tokenConsumer) {
        return aiGenerateService.generateWithOllama(prompt, tokenConsumer, error -> {
            if (!completed.get()) {
                sendError(emitter, error.getMessage());
                completed.set(true);
            }
        });
    }

    private void saveToCache(String fullContent, Long examId, String dataHash, String modelType) {
        try {
            String cleanContent = fullContent.replaceAll("(?s)💭.*?🌸", "").trim();
            String conclusion = cleanContent;
            String advice = "";

            java.util.regex.Matcher cm = java.util.regex.Pattern
                    .compile("【体检结论】\\s*([\\s\\S]*?)(?=【健康建议】|$)")
                    .matcher(cleanContent);
            java.util.regex.Matcher am = java.util.regex.Pattern
                    .compile("【健康建议】\\s*([\\s\\S]*?)$")
                    .matcher(cleanContent);

            if (cm.find() && am.find()) {
                conclusion = cm.group(1).trim();
                advice = am.group(1).trim();
            } else {
                java.util.regex.Matcher cm2 = java.util.regex.Pattern
                        .compile("一、体检结论\\s*([\\s\\S]*?)(?=二、健康建议|$)", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(cleanContent);
                java.util.regex.Matcher am2 = java.util.regex.Pattern
                        .compile("二、健康建议\\s*([\\s\\S]*?)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(cleanContent);
                if (cm2.find() && am2.find()) {
                    conclusion = cm2.group(1).trim();
                    advice = am2.group(1).trim();
                }
            }

            aiCacheService.saveCache(examId, dataHash, conclusion, advice, modelType);
            log.info("缓存已保存，conclusion长度：{}，advice长度：{}", conclusion.length(), advice.length());
        } catch (Exception e) {
            log.error("保存缓存失败", e);
        }
    }

    private void startIdleChecker(SseEmitter emitter, AtomicBoolean completed, AtomicLong lastActivity) {
        executor.execute(() -> {
            while (!completed.get()) {
                try {
                    Thread.sleep(5000);
                    long idle = System.currentTimeMillis() - lastActivity.get();
                    if (idle > aiProperties.getStreamIdleTimeout() && !completed.get()) {
                        log.warn("流式响应空闲超过{}ms，强制关闭", aiProperties.getStreamIdleTimeout());
                        sendEvent(emitter, EVENT_ERROR, "生成过程卡住，请重试");
                        completed.set(true);
                        emitter.complete();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("空闲检测异常", e);
                }
            }
        });
    }

    private void notifyFallback(SseEmitter emitter, String message) {
        sendEvent(emitter, EVENT_FALLBACK, message);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        sendEvent(emitter, EVENT_ERROR, "AI生成失败: " + message);
        try {
            emitter.complete();
        } catch (Exception e) {
            log.error("关闭Emitter失败", e);
        }
    }
}