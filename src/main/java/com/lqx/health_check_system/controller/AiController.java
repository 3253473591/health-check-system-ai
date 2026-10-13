package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/doctor/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/generate/{examId}")
    public SseEmitter generate(@PathVariable Long examId) {
        return aiService.generateReport(examId);
    }

    @GetMapping("/generate-rag/{examId}")
    public SseEmitter generateWithRag(@PathVariable Long examId) {
        return aiService.generateReportWithRag(examId);
    }
}