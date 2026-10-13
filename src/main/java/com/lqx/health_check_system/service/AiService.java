package com.lqx.health_check_system.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {

    SseEmitter generateReport(Long examId);

    SseEmitter generateReportWithRag(Long examId);
}