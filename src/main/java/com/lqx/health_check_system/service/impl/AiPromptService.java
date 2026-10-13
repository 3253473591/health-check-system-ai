package com.lqx.health_check_system.service.impl;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AiPromptService {

    @SuppressWarnings("unchecked")
    public String buildRagPrompt(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名体检报告辅助生成助手。请根据以下患者体检数据，**结合医学知识库中的诊疗指南**，撰写体检结论和健康建议。\n\n");

        appendPatientInfo(sb, data);
        appendExamData(sb, (List<Map<String, Object>>) data.get("dataTable"));
        appendRagFormatRequirements(sb);
        appendRagTemplate(sb);
        appendKnowledgeBaseRequirements(sb);

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String buildNormalPrompt(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名专业医生，请根据以下体检数据，用中文撰写「体检结论」和「健康建议」。\n\n");

        sb.append("【患者基本信息】\n");
        sb.append("姓名：").append(data.getOrDefault("realName", "")).append("\n");
        sb.append("性别：").append(data.getOrDefault("gender", "")).append("\n");
        sb.append("年龄：").append(data.getOrDefault("age", "")).append(" 岁\n");
        sb.append("体检套餐：").append(data.getOrDefault("packageName", "")).append("\n\n");

        sb.append("【体检数据】\n");
        List<Map<String, Object>> table = (List<Map<String, Object>>) data.get("dataTable");
        if (table != null && !table.isEmpty()) {
            sb.append(String.format("%-16s %-10s %-8s %-16s %-6s%n", "项目", "结果", "单位", "参考范围", "异常"));
            sb.append("----------------------------------------------------------------\n");

            String rows = table.stream()
                    .map(row -> String.format("%-16s %-10s %-8s %-16s %-6s",
                            row.getOrDefault("itemName", ""),
                            row.getOrDefault("itemValue", ""),
                            row.getOrDefault("unit", "-"),
                            row.getOrDefault("refRange", "-"),
                            row.getOrDefault("abnormal", "—")))
                    .collect(Collectors.joining("\n"));
            sb.append(rows);
        }

        sb.append("\n\n请按以下格式输出（不要输出其他内容）：\n\n");
        sb.append("【体检结论】\n（此处写结论，重点说明异常项目及整体健康状况）\n\n");
        sb.append("【健康建议】\n（此处写建议，针对异常项目或生活方式给出具体可行的建议,每条建议单独一行）\n");

        return sb.toString();
    }

    private void appendPatientInfo(StringBuilder sb, Map<String, Object> data) {
        sb.append("【患者信息】\n");
        sb.append("姓名：").append(data.getOrDefault("realName", "")).append("\n");
        sb.append("性别：").append(data.getOrDefault("gender", "")).append("\n");
        sb.append("年龄：").append(data.getOrDefault("age", "")).append(" 岁\n");
        sb.append("套餐：").append(data.getOrDefault("packageName", "")).append("\n\n");
    }

    private void appendExamData(StringBuilder sb, List<Map<String, Object>> table) {
        sb.append("【体检数据】\n");
        if (table == null || table.isEmpty()) {
            sb.append("无数据\n");
            return;
        }

        table.forEach(row -> {
            String abnormal = row.getOrDefault("abnormal", "").toString();
            String abnormalTag = "无异常".equals(abnormal) ? "" : "[" + abnormal + "]";

            sb.append(String.format("• %s: %s %s (参考: %s) %s%n",
                    row.getOrDefault("itemName", ""),
                    row.getOrDefault("itemValue", ""),
                    row.getOrDefault("unit", ""),
                    row.getOrDefault("refRange", "-"),
                    abnormalTag));
        });
    }

    private void appendRagFormatRequirements(StringBuilder sb) {
        sb.append("\n【格式要求 - 严格遵守】\n");
        sb.append("1. 使用纯文本格式，禁止使用任何Markdown标记（如 **、-、#、` 等）\n");
        sb.append("2. 不要使用加粗、斜体、列表符号、代码块等格式\n");
        sb.append("3. 段落之间用空行分隔，序号用阿拉伯数字加顿号（如 1.、2.）\n");
        sb.append("4. 专业术语正常书写，无需特殊标记\n\n");
    }

    private void appendRagTemplate(StringBuilder sb) {
        sb.append("【输出模板】\n\n");
        sb.append("一、体检结论\n");
        sb.append("1. 总体评估：患者整体健康状况一句话概括\n");
        sb.append("2. 异常指标分析：如有异常，说明指标名称、数值及临床意义；如无异常，说明各项指标均在正常范围\n");
        sb.append("3. 风险提示：结合知识库指南，指出需要注意的健康风险\n\n");
        sb.append("二、健康建议\n");
        sb.append("1. 建议一内容（具体可行，如饮食调整、运动方案等）\n");
        sb.append("2. 建议二内容\n");
        sb.append("3. 更多建议内容，可拓展至建议四乃至建议十，最后一点需提到复查周期，如建议3个月后复查）\n\n");
    }

    private void appendKnowledgeBaseRequirements(StringBuilder sb) {
        sb.append("【知识库引用要求】\n");
        sb.append("请结合上传的医学指南（如高血压防治指南等）给出专业建议，并在相关建议后注明依据（如：根据高血压指南2025版）\n");
        sb.append("禁止引用未上传的医学指南\n");
    }
}