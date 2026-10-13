package com.lqx.health_check_system.service;

import java.util.function.Consumer;

public interface AiGenerateService {

    boolean generateWithOllama(String prompt, Consumer<String> onToken, Consumer<Exception> onError);

    boolean generateWithRag(String prompt, Consumer<String> onToken, Consumer<Exception> onError);
}